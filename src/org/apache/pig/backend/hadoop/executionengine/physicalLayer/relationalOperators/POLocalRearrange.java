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
package org.apache.pig.backend.hadoop.executionengine.physicalLayer.relationalOperators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pig.PigException;
import org.apache.pig.backend.executionengine.ExecException;
import org.apache.pig.data.DataType;
import org.apache.pig.data.Tuple;
import org.apache.pig.data.TupleFactory;
import org.apache.pig.backend.hadoop.executionengine.physicalLayer.POStatus;
import org.apache.pig.backend.hadoop.executionengine.physicalLayer.PhysicalOperator;
import org.apache.pig.backend.hadoop.executionengine.physicalLayer.Result;
import org.apache.pig.backend.hadoop.executionengine.physicalLayer.expressionOperators.ExpressionOperator;
import org.apache.pig.backend.hadoop.executionengine.physicalLayer.expressionOperators.POProject;
import org.apache.pig.backend.hadoop.executionengine.physicalLayer.plans.PhyPlanVisitor;
import org.apache.pig.backend.hadoop.executionengine.physicalLayer.plans.PhysicalPlan;
import org.apache.pig.impl.plan.OperatorKey;
import org.apache.pig.impl.plan.NodeIdGenerator;
import org.apache.pig.impl.plan.VisitorException;
import org.apache.pig.impl.plan.PlanException;

/**
 * The local rearrange operator is a part of the co-group
 * implementation. It has an embedded physical plan that
 * generates tuples of the form (grpKey,(indxed inp Tuple)).
 *
 */
public class POLocalRearrange extends PhysicalOperator {

    /**
     * 
     */
    protected static final long serialVersionUID = 1L;

    protected static TupleFactory mTupleFactory = TupleFactory.getInstance();

    private Log log = LogFactory.getLog(getClass());

    protected List<PhysicalPlan> plans;
    
    protected List<ExpressionOperator> leafOps;

    // The position of this LR in the package operator
    protected byte index;
    
    protected byte keyType;

    protected boolean mIsDistinct = false;
    
    protected boolean isCross = false;
    
    // map to store mapping of projected columns to
    // the position in the "Key" where these will be projected to.
    // We use this information to strip off these columns
    // from the "Value" and in POPackage stitch the right "Value"
    // tuple back by getting these columns from the "key". The goal
    // is to reduce the amount of the data sent to Hadoop in the map.
    // Example: a  = load 'bla'; b = load 'bla'; c = cogroup a by ($2, $3), b by ($, $2)
    // For the first input (a), the map would contain following key:value
    // 2:0 (2 corresponds to $2 in cogroup a by ($2, $3) and 0 corresponds to 1st index in key)
    // 3:1 (3 corresponds to $3 in cogroup a by ($2, $3) and 0 corresponds to 2nd index in key)
    private Map<Integer, Integer> mProjectedColsMap;

    // A place holder Tuple used in distinct case where we really don't
    // have any value to pass through.  But hadoop gets cranky if we pass a
    // null, so we'll just create one instance of this empty tuple and
    // pass it for every row.  We only get around to actually creating it if
    // mIsDistinct is set to true.
    protected Tuple mFakeTuple = null;

	// indicator whether the project in the inner plans
	// is a project(*) - we set this ONLY when the project(*)
	// is the ONLY thing in the cogroup by ..
	private boolean mProjectStar = false;

    // marker to note that the "key" is a tuple
    // this is required by POPackage to pick things
    // off the "key" correctly to stitch together the
    // "value"
    private boolean isKeyTuple = false;

    private int mProjectedColsMapSize = 0;

    private ArrayList<Integer> minValuePositions;
    private int minValuePositionsSize = 0;

    private Tuple lrOutput;
    
    public POLocalRearrange(OperatorKey k) {
        this(k, -1, null);
    }

    public POLocalRearrange(OperatorKey k, int rp) {
        this(k, rp, null);
    }

    public POLocalRearrange(OperatorKey k, List<PhysicalOperator> inp) {
        this(k, -1, inp);
    }

    public POLocalRearrange(OperatorKey k, int rp, List<PhysicalOperator> inp) {
        super(k, rp, inp);
        index = -1;
        leafOps = new ArrayList<ExpressionOperator>();
        mProjectedColsMap = new HashMap<Integer, Integer>();
        lrOutput = mTupleFactory.newTuple(3);
    }

    @Override
    public void visit(PhyPlanVisitor v) throws VisitorException {
        v.visitLocalRearrange(this);
    }

    @Override
    public String name() {
        return "Local Rearrange" + "[" + DataType.findTypeName(resultType) +
            "]" + "{" + DataType.findTypeName(keyType) + "}" + "(" +
            mIsDistinct + ") - " + mKey.toString();
    }

    @Override
    public boolean supportsMultipleInputs() {
        return false;
    }

    @Override
    public boolean supportsMultipleOutputs() {
        return false;
    }

    public byte getIndex() {
        return index;
    }

    /**
     * Sets the co-group index of this operator
     * 
     * @param index the position of this operator in 
     * a co-group operation 
     * @throws ExecException if the index value is bigger then 0x7F
     */
    public void setIndex(int index) throws ExecException {
        setIndex(index, false);
    }

    /**
     * Sets the multi-query index of this operator
     * 
     * @param index the position of the parent plan of this operator
     * in the enclosed split operator
     * @throws ExecException if the index value is bigger then 0x7F
     */
    public void setMultiQueryIndex(int index) throws ExecException {
        setIndex(index, true);
    }
    
    private void setIndex(int index, boolean multiQuery) throws ExecException {
        if (index > 0x7F) {
            int errCode = 1082;
            String msg = multiQuery? 
                    "Merge more than 127 map-reduce jobs not supported."
                  : "Cogroups with more than 127 inputs not supported.";
            throw new ExecException(msg, errCode, PigException.INPUT);
        } else {
            this.index = multiQuery ? (byte)(index | 0x80) : (byte)index;
        }            
        lrOutput.set(0, new Byte(this.index));
    }
    
    public boolean isDistinct() { 
        return mIsDistinct;
    }

    public void setDistinct(boolean isDistinct) {
        mIsDistinct = isDistinct;
        if (mIsDistinct) {
            mFakeTuple = mTupleFactory.newTuple();
        }
    }
    
    /**
     * Overridden since the attachment of the new input should cause the old
     * processing to end.
     */
    @Override
    public void attachInput(Tuple t) {
        super.attachInput(t);
    }
    
    /**
     * Calls getNext on the generate operator inside the nested
     * physical plan. Converts the generated tuple into the proper
     * format, i.e, (key,indexedTuple(value))
     */
    @Override
    public Result getNext(Tuple t) throws ExecException {
        
        Result inp = null;
        Result res = null;
        while (true) {
            inp = processInput();
            if (inp.returnStatus == POStatus.STATUS_EOP || inp.returnStatus == POStatus.STATUS_ERR)
                break;
            if (inp.returnStatus == POStatus.STATUS_NULL)
                continue;
            
            for (PhysicalPlan ep : plans) {
                ep.attachInput((Tuple)inp.result);
            }
            List<Result> resLst = new ArrayList<Result>();
            for (ExpressionOperator op : leafOps){
                
                switch(op.getResultType()){
                case DataType.BAG:
                    res = op.getNext(dummyBag);
                    break;
                case DataType.BOOLEAN:
                    res = op.getNext(dummyBool);
                    break;
                case DataType.BYTEARRAY:
                    res = op.getNext(dummyDBA);
                    break;
                case DataType.CHARARRAY:
                    res = op.getNext(dummyString);
                    break;
                case DataType.DOUBLE:
                    res = op.getNext(dummyDouble);
                    break;
                case DataType.FLOAT:
                    res = op.getNext(dummyFloat);
                    break;
                case DataType.INTEGER:
                    res = op.getNext(dummyInt);
                    break;
                case DataType.LONG:
                    res = op.getNext(dummyLong);
                    break;
                case DataType.MAP:
                    res = op.getNext(dummyMap);
                    break;
                case DataType.TUPLE:
                    res = op.getNext(dummyTuple);
                    break;
                }
                if(res.returnStatus!=POStatus.STATUS_OK)
                    return new Result();
                resLst.add(res);
            }
            res.result = constructLROutput(resLst,(Tuple)inp.result);
            
            return res;
        }
        return inp;
    }
    
    protected Tuple constructLROutput(List<Result> resLst, Tuple value) throws ExecException{
        //Construct key
        Object key;
        
        if(resLst.size()>1){
            Tuple t = mTupleFactory.newTuple(resLst.size());
            int i=-1;
            for(Result res : resLst)
                t.set(++i, res.result);
            key = t;           
        } else if (resLst.size() == 1 && keyType == DataType.TUPLE) {
            
            // We get here after merging multiple jobs that have different
            // map key types into a single job during multi-query optimization.
            // If the key isn't a tuple, it must be wrapped in a tuple.
            Object obj = resLst.get(0).result;
            if (obj instanceof Tuple) {
                key = (Tuple)obj;
            } else {
                Tuple t = mTupleFactory.newTuple(1);
                t.set(0, resLst.get(0).result);
                key = t;
            }        
        }
        else{
            key = resLst.get(0).result;
        }
        
        if (mIsDistinct) {

            //Put the key and the indexed tuple
            //in a tuple and return
            lrOutput.set(1, key);
            lrOutput.set(2, mFakeTuple);
            return lrOutput;
        } else if(isCross){
        
            for(int i=0;i<plans.size();i++)
                value.getAll().remove(0);
            //Put the index, key, and value
            //in a tuple and return
            lrOutput.set(1, key);
            lrOutput.set(2, value);
            return lrOutput;
        } else {

            //Put the index, key, and value
            //in a tuple and return
            lrOutput.set(1, key);
            
            // strip off the columns in the "value" which 
            // are present in the "key"
            if(mProjectedColsMapSize != 0 || mProjectStar == true) {

                Tuple minimalValue = null;
                if(!mProjectStar) {
                    if(minValuePositions == null) {
                        // the very first time, we will have to build
                        // the "value" tuple piecemeal but we can
                        // do better next time round
                        minValuePositions = new ArrayList<Integer>();
                        minimalValue = mTupleFactory.newTuple();
                        // look for individual columns that we are
                        // projecting
                        for (int i = 0; i < value.size(); i++) {
                            if(mProjectedColsMap.get(i) == null) {
                                // this column was not found in the "key"
                                // so send it in the "value"
                                minimalValue.append(value.get(i));
                                minValuePositions.add(i);
                            }
                        }
                        minValuePositionsSize = minValuePositions.size();
                    } else {
                        minimalValue = mTupleFactory.newTuple(minValuePositionsSize);
                        for(int i = 0; i < minValuePositionsSize; i++) {
                            minimalValue.set(i, value.get(minValuePositions.get(i)));
                        }
                    }
                } else {
                    // for the project star case
                    // we would send out an empty tuple as
                    // the "value" since all elements are in the
                    // "key"
                    minimalValue = mTupleFactory.newTuple();
    
                }
                lrOutput.set(2, minimalValue);
            
            } else {
            
                // there were no columns in the "key"
                // which we can strip off from the "value"
                // so just send the value we got
                lrOutput.set(2, value);
                
            }
            return lrOutput;
        }
    }

    public byte getKeyType() {
        return keyType;
    }

    public void setKeyType(byte keyType) {
        this.keyType = keyType;
    }

    public List<PhysicalPlan> getPlans() {
        return plans;
    }

    public void setPlans(List<PhysicalPlan> plans) throws PlanException {
        this.plans = plans;
        leafOps.clear();
        int keyIndex = 0; // zero based index for fields in the key
        for (PhysicalPlan plan : plans) {
            ExpressionOperator leaf = (ExpressionOperator)plan.getLeaves().get(0); 
            leafOps.add(leaf);
            
            // don't optimize CROSS
            if(!isCross) {
                // Look for the leaf Ops which are POProject operators - get the 
                // the columns that these POProject Operators are projecting.
                // They MUST be projecting either a column or '*'.
                // Keep track of the columns which are being projected and
                // the position in the "Key" where these will be projected to.
                // Then we can use this information to strip off these columns
                // from the "Value" and in POPackage stitch the right "Value"
                // tuple back by getting these columns from the "key". The goal
                // is reduce the amount of the data sent to Hadoop in the map.
                if(leaf instanceof POProject) {
                    POProject project = (POProject) leaf;
                    if(project.isStar()) {
                        if(plans.size() == 1) {
                            // note that we have a project *
                            mProjectStar  = true;
                            // key will be a tuple in this case
                            isKeyTuple = true;
                        } else {
                            // TODO: currently "group by (*, somethingelse)" is NOT
                            // allowed. So we should never get here. But once it is
                            // allowed, we will need to handle it. For now just log
                            log.debug("Project * in group by not being optimized in key-value transfer");
                        }
                    } else {
                        try {
                            mProjectedColsMap.put(project.getColumn(), keyIndex);
                        } catch (ExecException e) {
                            int errCode = 2070;
                            String msg = "Problem in accessing column from project operator.";
                            throw new PlanException(msg, errCode, PigException.BUG);
                        }
                    }
                    if(project.getResultType() == DataType.TUPLE)
                        isKeyTuple = true;
                }
                keyIndex++;
            }
        }
        if(keyIndex > 1) {
            // make a note that the "key" is a tuple
            // this is required by POPackage to pick things
            // off the "key" correctly to stitch together the
            // "value"
            isKeyTuple  = true;
        }
        mProjectedColsMapSize = mProjectedColsMap.size();
    }

    /**
     * Make a deep copy of this operator.  
     * @throws CloneNotSupportedException
     */
    @Override
    public POLocalRearrange clone() throws CloneNotSupportedException {
        List<PhysicalPlan> clonePlans = new
            ArrayList<PhysicalPlan>(plans.size());
        for (PhysicalPlan plan : plans) {
            clonePlans.add(plan.clone());
        }
        POLocalRearrange clone = new POLocalRearrange(new OperatorKey(
            mKey.scope, 
            NodeIdGenerator.getGenerator().getNextNodeId(mKey.scope)),
            requestedParallelism);
        try {
            clone.setPlans(clonePlans);
        } catch (PlanException pe) {
            CloneNotSupportedException cnse = new CloneNotSupportedException("Problem with setting plans of " + this.getClass().getSimpleName());
            cnse.initCause(pe);
            throw cnse;
        }
        clone.keyType = keyType;
        clone.index = index;
        try {
            clone.lrOutput.set(0, index);
        } catch (ExecException e) {
            CloneNotSupportedException cnse = new CloneNotSupportedException("Problem with setting index of output.");
            cnse.initCause(e);
            throw cnse;
        }
        // Needs to be called as setDistinct so that the fake index tuple gets
        // created.
        clone.setDistinct(mIsDistinct);
        return clone;
    }

    public boolean isCross() {
        return isCross;
    }

    public void setCross(boolean isCross) {
        this.isCross = isCross;
    }

    /**
     * @return the mProjectedColsMap
     */
    public Map<Integer, Integer> getProjectedColsMap() {
        return mProjectedColsMap;
    }

    /**
     * @return the mProjectStar
     */
    public boolean isProjectStar() {
        return mProjectStar;
    }

    /**
     * @return the keyTuple
     */
    public boolean isKeyTuple() {
        return isKeyTuple;
    }

    /**
     * @param plans
     * @throws ExecException 
     */
    public void setPlansFromCombiner(List<PhysicalPlan> plans) throws PlanException {
        this.plans = plans;
        leafOps.clear();
        mProjectedColsMap.clear();
        int keyIndex = 0; // zero based index for fields in the key
        for (PhysicalPlan plan : plans) {
            ExpressionOperator leaf = (ExpressionOperator)plan.getLeaves().get(0); 
            leafOps.add(leaf);
            
            // don't optimize CROSS
            if(!isCross) {
                // Look for the leaf Ops which are POProject operators - get the 
                // the columns that these POProject Operators are projecting.
                // Keep track of the columns which are being projected and
                // the position in the "Key" where these will be projected to.
                // Then we can use this information to strip off these columns
                // from the "Value" and in POPostCombinerPackage stitch the right "Value"
                // tuple back by getting these columns from the "key". The goal
                // is reduce the amount of the data sent to Hadoop in the map.
                if(leaf instanceof POProject) {
                    POProject project = (POProject) leaf;
                    if(project.isStar()) {
                        int errCode = 2021;
                        String msg = "Internal error. Unexpected operator project(*) in local rearrange inner plan.";
                        throw new PlanException(msg, errCode, PigException.BUG);
                    } else {
                        try {
                            mProjectedColsMap.put(project.getColumn(), keyIndex);
                        } catch (ExecException e) {
                            int errCode = 2070;
                            String msg = "Problem in accessing column from project operator.";
                            throw new PlanException(msg, errCode, PigException.BUG);
                        }
                    }
                    if(project.getResultType() == DataType.TUPLE)
                        isKeyTuple = true;
                }
                keyIndex++;
            }
        }
        if(keyIndex > 1) {
            // make a note that the "key" is a tuple
            // this is required by POPackage to pick things
            // off the "key" correctly to stitch together the
            // "value"
            isKeyTuple  = true;
        }
        mProjectedColsMapSize  = mProjectedColsMap.size();
        
    }

}
