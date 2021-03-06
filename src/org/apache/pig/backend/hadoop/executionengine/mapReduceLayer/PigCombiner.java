/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.pig.backend.hadoop.executionengine.mapReduceLayer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;

import org.apache.pig.PigException;
import org.apache.pig.backend.executionengine.ExecException;
import org.apache.pig.backend.hadoop.HDataType;
import org.apache.pig.backend.hadoop.executionengine.physicalLayer.PhysicalOperator;
import org.apache.pig.backend.hadoop.executionengine.physicalLayer.POStatus;
import org.apache.pig.backend.hadoop.executionengine.physicalLayer.PigLogger;
import org.apache.pig.backend.hadoop.executionengine.physicalLayer.Result;
import org.apache.pig.backend.hadoop.executionengine.physicalLayer.plans.PhysicalPlan;
import org.apache.pig.backend.hadoop.executionengine.physicalLayer.relationalOperators.POJoinPackage;
import org.apache.pig.backend.hadoop.executionengine.physicalLayer.relationalOperators.POPackage;
import org.apache.pig.data.DataType;
import org.apache.pig.data.TargetedTuple;
import org.apache.pig.data.Tuple;
import org.apache.pig.impl.PigContext;
import org.apache.pig.impl.io.PigNullableWritable;
import org.apache.pig.impl.io.NullableTuple;
import org.apache.pig.impl.plan.DependencyOrderWalker;
import org.apache.pig.impl.plan.VisitorException;
import org.apache.pig.impl.util.ObjectSerializer;
import org.apache.pig.impl.util.WrappedIOException;

public class PigCombiner {

    public static JobConf sJobConf = null;
    
    public static class Combine extends MapReduceBase
            implements
            Reducer<PigNullableWritable, NullableTuple, PigNullableWritable, Writable> {
        private final Log log = LogFactory.getLog(getClass());

        private final static Tuple DUMMYTUPLE = null;
        
        private byte keyType;
        
        //The reduce plan
        private PhysicalPlan cp;
        
        //The POPackage operator which is the
        //root of every Map Reduce plan is
        //obtained through the job conf. The portion
        //remaining after its removal is the reduce
        //plan
        private POPackage pack;
        
        ProgressableReporter pigReporter;
        
        PhysicalOperator[] roots;
        PhysicalOperator leaf;
        
        PigContext pigContext = null;
        private volatile boolean initialized = false;
        
        /**
         * Configures the Reduce plan, the POPackage operator
         * and the reporter thread
         */
        @Override
        public void configure(JobConf jConf) {
            super.configure(jConf);
            sJobConf = jConf;
            try {
                cp = (PhysicalPlan) ObjectSerializer.deserialize(jConf
                        .get("pig.combinePlan"));
                pack = (POPackage)ObjectSerializer.deserialize(jConf.get("pig.combine.package"));
                // To be removed
                if(cp.isEmpty())
                    log.debug("Combine Plan empty!");
                else{
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    cp.explain(baos);
                    log.debug(baos.toString());
                }
                
                keyType = ((byte[])ObjectSerializer.deserialize(jConf.get("pig.map.keytype")))[0];
                // till here
                
                long sleepTime = jConf.getLong("pig.reporter.sleep.time", 10000);

                pigReporter = new ProgressableReporter();
                if(!(cp.isEmpty())) {
                    roots = cp.getRoots().toArray(new PhysicalOperator[1]);
                    leaf = cp.getLeaves().get(0);
                }
                
                pigContext = (PigContext)ObjectSerializer.deserialize(jConf.get("pig.pigContext"));
                
            } catch (IOException ioe) {
                String msg = "Problem while configuring combiner's reduce plan.";
                throw new RuntimeException(msg, ioe);
            }
        }
        
        /**
         * The reduce function which packages the key and List &lt;Tuple&gt;
         * into key, Bag&lt;Tuple&gt; after converting Hadoop type key into Pig type.
         * The package result is either collected as is, if the reduce plan is
         * empty or after passing through the reduce plan.
         */
        public void reduce(PigNullableWritable key,
                Iterator<NullableTuple> tupIter,
                OutputCollector<PigNullableWritable, Writable> oc,
                Reporter reporter) throws IOException {
            
        	if(!initialized) {
        		initialized = true;
	            pigReporter.setRep(reporter);	            
	            PhysicalOperator.setReporter(pigReporter);

	            boolean aggregateWarning = "true".equalsIgnoreCase(pigContext.getProperties().getProperty("aggregate.warning"));

	            PigHadoopLogger pigHadoopLogger = PigHadoopLogger.getInstance();
	            pigHadoopLogger.setAggregate(aggregateWarning);
	            pigHadoopLogger.setReporter(reporter);
	            PhysicalOperator.setPigLogger(pigHadoopLogger);
        	}
            
            // In the case we optimize, we combine
            // POPackage and POForeach - so we could get many
            // tuples out of the getnext() call of POJoinPackage
            // In this case, we process till we see EOP from 
            // POJoinPacakage.getNext()
            if (pack instanceof POJoinPackage)
            {
                pack.attachInput(key, tupIter);
                while (true)
                {
                    if (processOnePackageOutput(oc))
                        break;
                }
            }
            else {
                // not optimized, so package will
                // give only one tuple out for the key
                pack.attachInput(key, tupIter);
                processOnePackageOutput(oc);
            }
            
        }
        
        // return: false-more output
        //         true- end of processing
        public boolean processOnePackageOutput(OutputCollector<PigNullableWritable, Writable> oc) throws IOException {
            try {
                Result res = pack.getNext(DUMMYTUPLE);
                if(res.returnStatus==POStatus.STATUS_OK){
                    Tuple packRes = (Tuple)res.result;
                    
                    if(cp.isEmpty()){
                        oc.collect(null, packRes);
                        return false;
                    }
                    
                    for (int i = 0; i < roots.length; i++) {
                        roots[i].attachInput(packRes);
                    }
                    while(true){
                        Result redRes = leaf.getNext(DUMMYTUPLE);
                        
                        if(redRes.returnStatus==POStatus.STATUS_OK){
                            Tuple tuple = (Tuple)redRes.result;
                            Byte index = (Byte)tuple.get(0);
                            PigNullableWritable outKey =
                                HDataType.getWritableComparableTypes(tuple.get(1), this.keyType);
                            NullableTuple val =
                                new NullableTuple((Tuple)tuple.get(2));
                            // Both the key and the value need the index.  The key needs it so
                            // that it can be sorted on the index in addition to the key
                            // value.  The value needs it so that POPackage can properly
                            // assign the tuple to its slot in the projection.
                            outKey.setIndex(index);
                            val.setIndex(index);
                            oc.collect(outKey, val);
                            continue;
                        }
                        
                        if(redRes.returnStatus==POStatus.STATUS_EOP)
                            break;
                        
                        if(redRes.returnStatus==POStatus.STATUS_NULL)
                            continue;
                        
                        if(redRes.returnStatus==POStatus.STATUS_ERR){
                            int errCode = 2090;
                            String msg = "Received Error while " +
                            "processing the combine plan.";
                            if(redRes.result != null) {
                                msg += redRes.result;
                            }
                            throw new ExecException(msg, errCode, PigException.BUG);
                        }
                    }
                }
                
                if(res.returnStatus==POStatus.STATUS_NULL)
                    return false;
                
                if(res.returnStatus==POStatus.STATUS_ERR){
                    int errCode = 2091;
                    String msg = "Packaging error while processing group.";
                    throw new ExecException(msg, errCode, PigException.BUG);
                }
                
                if(res.returnStatus==POStatus.STATUS_EOP) {
                    return true;
                }
                    
                return false;    
                
            } catch (ExecException e) {
                throw e;
            }

        }
        
        /**
         * Will be called once all the intermediate keys and values are
         * processed. So right place to stop the reporter thread.
         */
        @Override
        public void close() throws IOException {
            super.close();
        }

        /**
         * @return the keyType
         */
        public byte getKeyType() {
            return keyType;
        }

        /**
         * @param keyType the keyType to set
         */
        public void setKeyType(byte keyType) {
            this.keyType = keyType;
        }
    }
    
}
