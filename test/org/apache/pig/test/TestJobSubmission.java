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
package org.apache.pig.test;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.jobcontrol.Job;
import org.apache.hadoop.mapred.jobcontrol.JobControl;
import org.apache.pig.backend.executionengine.ExecException;
import org.apache.pig.backend.executionengine.ExecutionEngine;
import org.apache.pig.backend.hadoop.datastorage.HConfiguration;
import org.apache.pig.backend.hadoop.executionengine.HExecutionEngine;
import org.apache.pig.builtin.BinStorage;
import org.apache.pig.builtin.PigStorage;
import org.apache.pig.data.DataBag;
import org.apache.pig.data.DataType;
import org.apache.pig.data.DefaultBagFactory;
import org.apache.pig.data.DefaultTuple;
import org.apache.pig.data.Tuple;
import org.apache.pig.impl.PigContext;
import org.apache.pig.impl.io.FileLocalizer;
import org.apache.pig.impl.io.FileSpec;
import org.apache.pig.impl.logicalLayer.OperatorKey;
import org.apache.pig.impl.mapReduceLayer.JobControlCompiler;
import org.apache.pig.impl.mapReduceLayer.MRCompiler;
import org.apache.pig.impl.mapReduceLayer.MapReduceLauncher;
import org.apache.pig.impl.mapReduceLayer.plans.MROperPlan;
import org.apache.pig.impl.physicalLayer.POStatus;
import org.apache.pig.impl.physicalLayer.Result;
import org.apache.pig.impl.physicalLayer.plans.ExprPlan;
import org.apache.pig.impl.physicalLayer.plans.PhysicalPlan;
import org.apache.pig.impl.physicalLayer.topLevelOperators.POFilter;
import org.apache.pig.impl.physicalLayer.topLevelOperators.POForEach;
import org.apache.pig.impl.physicalLayer.topLevelOperators.POGenerate;
import org.apache.pig.impl.physicalLayer.topLevelOperators.POGlobalRearrange;
import org.apache.pig.impl.physicalLayer.topLevelOperators.POLoad;
import org.apache.pig.impl.physicalLayer.topLevelOperators.POLocalRearrange;
import org.apache.pig.impl.physicalLayer.topLevelOperators.POPackage;
import org.apache.pig.impl.physicalLayer.topLevelOperators.POSplit;
import org.apache.pig.impl.physicalLayer.topLevelOperators.POStore;
import org.apache.pig.impl.physicalLayer.topLevelOperators.POUnion;
import org.apache.pig.impl.physicalLayer.topLevelOperators.PhysicalOperator;
import org.apache.pig.impl.physicalLayer.topLevelOperators.expressionOperators.ConstantExpression;
import org.apache.pig.impl.physicalLayer.topLevelOperators.expressionOperators.POCast;
import org.apache.pig.impl.physicalLayer.topLevelOperators.expressionOperators.POProject;
import org.apache.pig.impl.physicalLayer.topLevelOperators.expressionOperators.binaryExprOps.comparators.ComparisonOperator;
import org.apache.pig.impl.plan.VisitorException;
import org.apache.pig.impl.plan.PlanException;
import org.apache.pig.impl.util.ObjectSerializer;
import org.apache.pig.test.utils.GenPhyOp;
import org.apache.pig.test.utils.GenRandomData;
import org.apache.pig.test.utils.TestHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestJobSubmission extends junit.framework.TestCase{
    
    
    static PigContext pc;
    String ldFile;
    String expFile;
    PhysicalPlan<PhysicalOperator> php = new PhysicalPlan<PhysicalOperator>();
    String stFile;
    String hadoopLdFile;
    String grpName;
    Random r = new Random();
    String curDir;
    String inpDir;
    String golDir;
    
    static {
        MiniCluster cluster = MiniCluster.buildCluster();
        pc = new PigContext();
        try {
            pc.connect();
        } catch (ExecException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        GenPhyOp.setPc(pc);
        
    }
    @Before
    public void setUp() throws Exception{
        curDir = System.getProperty("user.dir");
        inpDir = curDir + File.separatorChar + "test/org/apache/pig/test/data/InputFiles/";
        golDir = curDir + File.separatorChar + "test/org/apache/pig/test/data/GoldenFiles/";
    }
    
    @After
    public void tearDown() throws Exception {
    }
    
    private void generateInput(int numTuples) throws ExecException{
        
        DataBag inpDb = GenRandomData.genRandSmallTupDataBag(r, numTuples, 1000);
        
        POProject proj = new POProject(new OperatorKey("", r.nextLong()));
        Tuple t = new DefaultTuple();
        t.append(inpDb);
        proj.attachInput(t);
        proj.setColumn(0);
        proj.setOverloaded(true);
        proj.setResultType(DataType.TUPLE);
        
        List<PhysicalOperator> inps = new ArrayList<PhysicalOperator>();
        inps.add(proj);
        
        POStore str = new POStore(new OperatorKey("", r.nextLong()));
        str.setInputs(inps);
        
        FileSpec fSpec = new FileSpec(ldFile,PigStorage.class.getName());
        
        str.setSFile(fSpec);
        str.setPc(pc);
        str.store();
    }
    
    private void setUp1(boolean gen) throws Exception {
        ldFile = "file:" + inpDir + "jsTst1.txt";
        expFile = ldFile;
        stFile = "jsTst1";
        grpName = "jobSubTst1";
        
        if(gen){
            generateInput(100);
            return;
        }
        
        hadoopLdFile = FileLocalizer.hadoopify(ldFile, pc);

        FileSpec LFSpec = new FileSpec(hadoopLdFile,PigStorage.class.getName());
        FileSpec SFSpec = new FileSpec(stFile,PigStorage.class.getName());

        POLoad ld = new POLoad(new OperatorKey("", r.nextLong()));
        POStore st = new POStore(new OperatorKey("", r.nextLong()));
        ld.setPc(pc);
        ld.setLFile(LFSpec);
        st.setPc(pc);
        st.setSFile(SFSpec);
        
        php.add(ld);
        php.add(st);
        php.connect(ld, st);
     }

//    @Test
    public void testCompile1() throws Exception {
        boolean gen = false;

        setUp1(gen);
        
        if(gen)
            return;

        submit();
        
        assertEquals(true, FileLocalizer.fileExists(stFile, pc));
        
        FileSpec fSpecExp = new FileSpec(expFile,PigStorage.class.getName());
        FileSpec fSpecAct = new FileSpec(stFile,PigStorage.class.getName());
        
        assertEquals(true, TestHelper.areFilesSame(fSpecExp, fSpecAct, pc));
    }
    
    private void setUp2(boolean gen) throws Exception {
        ldFile = "file:" + inpDir + "jsTst2.txt";
        expFile = ldFile;
        stFile = "jsTst2";
        grpName = "jobSubTst2";
        
        if(gen){
            generateInput(1000);
            return;
        }
        
        hadoopLdFile = FileLocalizer.hadoopify(ldFile, pc);

        FileSpec LFSpec = new FileSpec(hadoopLdFile,PigStorage.class.getName());
        FileSpec SFSpec = new FileSpec(stFile,PigStorage.class.getName());

        POLoad ld = new POLoad(new OperatorKey("", r.nextLong()));
        POStore st = new POStore(new OperatorKey("", r.nextLong()));
        ld.setPc(pc);
        ld.setLFile(LFSpec);
        st.setPc(pc);
        st.setSFile(SFSpec);
        
        php.add(ld);
        php.add(st);
        php.connect(ld, st);
     }

//    @Test
    public void testCompile2() throws Exception {
        boolean gen = false;

        setUp2(gen);
        
        if(gen)
            return;

        submit();
        
        assertEquals(true, FileLocalizer.fileExists(stFile, pc));
        
        FileSpec fSpecExp = new FileSpec(expFile,PigStorage.class.getName());
        FileSpec fSpecAct = new FileSpec(stFile,PigStorage.class.getName());
        
        assertEquals(true, TestHelper.areFilesSame(fSpecExp, fSpecAct, pc));
    }
    
    private void setUp3(boolean gen) throws Exception {
        ldFile = "file:" + inpDir + "jsTst1.txt";
        expFile = "file:" + golDir + "jsTst3";
        stFile = "jsTst3";
        grpName = "jobSubTst3";
        
        if(gen){
            generateInput(1000);
            return;
        }
        
        hadoopLdFile = FileLocalizer.hadoopify(ldFile, pc);

        FileSpec LFSpec = new FileSpec(hadoopLdFile,PigStorage.class.getName());
        FileSpec SFSpec = new FileSpec(stFile,PigStorage.class.getName());

        POLoad ld = new POLoad(new OperatorKey("", r.nextLong()));
        POStore st = new POStore(new OperatorKey("", r.nextLong()));
        ld.setPc(pc);
        ld.setLFile(LFSpec);
        st.setPc(pc);
        st.setSFile(SFSpec);
        
        int[] flds = {0,1};
        Tuple sample = new DefaultTuple();
        sample.append(new String("S"));
        sample.append(new Integer("10"));
        
        POForEach fe = GenPhyOp.topForEachOPWithPlan(flds , sample);
        
        POFilter fl = GenPhyOp.topFilterOpWithProj(1, 500, GenPhyOp.LT);
        
        php.add(ld);
        php.add(fe);
        php.connect(ld, fe);
        
        php.add(fl);
        php.connect(fe, fl);
        
        php.add(st);
        php.connect(fl, st);
     }

//    @Test
    public void testCompile3() throws Exception {
        boolean gen = false;

        setUp3(gen);
        
        if(gen)
            return;

        submit();
        
        assertEquals(true, FileLocalizer.fileExists(stFile, pc));
        
        FileSpec fSpecExp = new FileSpec(expFile,PigStorage.class.getName()+"(',')");
        FileSpec fSpecAct = new FileSpec(stFile,PigStorage.class.getName());
        
        assertEquals(true, TestHelper.areFilesSame(fSpecExp, fSpecAct, pc));
    }
    
    private void setUp4(boolean gen) throws Exception {
        ldFile = "file:" + inpDir + "jsTst1.txt";
        expFile = "file:" + golDir + "jsTst4";
        stFile = "jsTst4";
        grpName = "jobSubTst4";
        
        if(gen){
            generateInput(1000);
            return;
        }
        
        hadoopLdFile = FileLocalizer.hadoopify(ldFile, pc);

        FileSpec LFSpec = new FileSpec(hadoopLdFile,PigStorage.class.getName());
        FileSpec SFSpec = new FileSpec(stFile,PigStorage.class.getName());

        POLoad ld = new POLoad(new OperatorKey("", r.nextLong()));
        POStore st = new POStore(new OperatorKey("", r.nextLong()));
        ld.setPc(pc);
        ld.setLFile(LFSpec);
        st.setPc(pc);
        st.setSFile(SFSpec);
        
        POSplit spl = GenPhyOp.topSplitOp();
        POFilter fl1 = GenPhyOp.topFilterOpWithProjWithCast(1, 200, GenPhyOp.LT);
        POFilter fl2 = GenPhyOp.topFilterOpWithProjWithCast(1, 800, GenPhyOp.GT);
        
        POUnion un = GenPhyOp.topUnionOp();
        
        php.add(ld);
        php.add(spl);
        php.connect(ld, spl);
        
        php.add(fl1);
        php.connect(spl, fl1);
        
        php.add(fl2);
        php.connect(spl, fl2);
        
        php.add(un);
        php.connect(fl1, un);
        php.connect(fl2, un);
        
        php.add(st);
        php.connect(un, st);
     }

//    @Test
    public void testCompile4() throws Exception {
        boolean gen = false;

        setUp4(gen);
        
        if(gen)
            return;
        
        submit();
        
        assertEquals(true, FileLocalizer.fileExists(stFile, pc));
        
        FileSpec fSpecExp = new FileSpec(expFile,PigStorage.class.getName()+"(',')");
        FileSpec fSpecAct = new FileSpec(stFile,PigStorage.class.getName());
        
        assertEquals(true, TestHelper.areFilesSame(fSpecExp, fSpecAct, pc));
        
    }
    
    private void setUp5(boolean gen) throws Exception {
        ldFile = "file:" + inpDir + "jsTst5.txt";
        expFile = ldFile;
        stFile = "jsTst5";
        grpName = "jobSubTst5";
        
        if(gen){
            generateInput(1000);
            return;
        }
        
        hadoopLdFile = FileLocalizer.hadoopify(ldFile, pc);

        FileSpec LFSpec = new FileSpec(hadoopLdFile,PigStorage.class.getName()+"(',')");
        FileSpec SFSpec = new FileSpec(stFile,PigStorage.class.getName());

        POLoad ld = new POLoad(new OperatorKey("", r.nextLong()));
        POStore st = new POStore(new OperatorKey("", r.nextLong()));
        ld.setPc(pc);
        ld.setLFile(LFSpec);
        st.setPc(pc);
        st.setSFile(SFSpec);
        
        Tuple sample = new DefaultTuple();
        sample.append("S");
        sample.append(1);
        POLocalRearrange lr = GenPhyOp.topLocalRearrangeOPWithPlan(0, 1, sample);
        
        POGlobalRearrange gr = GenPhyOp.topGlobalRearrangeOp();
        
        POPackage pk = GenPhyOp.topPackageOp();
        pk.setKeyType(DataType.INTEGER);
        pk.setNumInps(1);
        boolean[] inner = {false}; 
        pk.setInner(inner);
        
        POForEach fe = GenPhyOp.topForEachOPWithPlan(1);
        
        php.add(ld);
        php.add(lr);
        php.connect(ld, lr);
        
        php.add(gr);
        php.connect(lr, gr);
        
        php.add(pk);
        php.connect(gr, pk);
        
        php.add(fe);
        php.connect(pk, fe);
        
        php.add(st);
        php.connect(fe, st);
     }

    @Test
    public void testCompile5() throws Exception {
        boolean gen = false;

        setUp5(gen);
        
        if(gen)
            return;
        
        submit();
        
        assertEquals(true, FileLocalizer.fileExists(stFile, pc));
        
        FileSpec fSpecExp = new FileSpec(expFile,PigStorage.class.getName()+"(',')");
        FileSpec fSpecAct = new FileSpec(stFile,PigStorage.class.getName());
        
        assertEquals(true, TestHelper.areFilesSame(fSpecExp, fSpecAct, pc));
        
    }
    
    private void submit() throws Exception{
        assertEquals(true, FileLocalizer.fileExists(hadoopLdFile, pc));
        MapReduceLauncher mrl = new MapReduceLauncher();
        mrl.launchPig(php, grpName, pc);  
    }
}