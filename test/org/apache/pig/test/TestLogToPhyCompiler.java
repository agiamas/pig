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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pig.ExecType;
import org.apache.pig.backend.executionengine.ExecException;
import org.apache.pig.builtin.PigStorage;
import org.apache.pig.impl.PigContext;
import org.apache.pig.impl.logicalLayer.ExpressionOperator;
import org.apache.pig.impl.logicalLayer.LogToPhyTranslationVisitor;
import org.apache.pig.impl.logicalLayer.LogicalOperator;
import org.apache.pig.impl.logicalLayer.LogicalPlan;
import org.apache.pig.impl.logicalLayer.LogicalPlanBuilder;
import org.apache.pig.impl.logicalLayer.OperatorKey;
import org.apache.pig.impl.physicalLayer.plans.PhysicalPlan;
import org.apache.pig.impl.plan.VisitorException;
import org.junit.Test;

/**
 * All new tests should be included at the end of the existing test cases. This is to ensure that 
 * nodeIdGenerator produces the same sequence as in the golden files for the previously existing test cases 
 * 
 * To generate golden files, use the following code :
 * ByteArrayOutputStream baos = new ByteArrayOutputStream();
 * pp.explain(baos);
 * FileOutputStream fos = new FileOutputStream("test/org/apache/pig/test/data/GoldenFiles/Union.gld");
 * fos.write(baos.toByteArray());
 *
 */
public class TestLogToPhyCompiler extends junit.framework.TestCase {

    private final Log log = LogFactory.getLog(getClass());
    
    File A;
    final int MAX_RANGE = 10;
    
    Random r = new Random();
    PigContext pc = new PigContext(ExecType.LOCAL);
    
    
    private void writeData(File input, int noTuples, int arityOfTuples, char separator) throws IOException {
    	FileOutputStream dat = new FileOutputStream(input);
        
        for(int i = 0; i < noTuples; i++) {
            
            for(int j = 0; j < arityOfTuples; j++) {
            	int temp = r.nextInt(MAX_RANGE);
            	if(j == arityOfTuples - 1) {
            		dat.write((temp + "\n").getBytes());
            	} else {
            		dat.write((temp + "" + separator).getBytes());
            	}
            }
        }
                    
        dat.close();
    }
    
    @Test
    public void testComplexForeach() throws VisitorException, IOException {
        /*String query = "foreach (load 'a') {" +
                "B = FILTER $0 BY (($1 == $2) AND ('a' < 'b'));" +
                "generate B;" +
                "};";*/
    	String query = "foreach (load 'a') {" +
        "B = FILTER $0 BY ($1 == $2);" +
        "generate B;" +
        "};";
        LogicalPlan plan = buildPlan(query);
    	PhysicalPlan pp = buildPhysicalPlan(plan); 
    	int MAX_SIZE = 100000;
    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
        pp.explain(baos);
        String compiledPlan = baos.toString();
    	FileInputStream fis = new FileInputStream("test/org/apache/pig/test/data/GoldenFiles/ComplexForeach.gld");
        byte[] b = new byte[MAX_SIZE];
        int len = fis.read(b);
        String goldenPlan = new String(b, 0, len);

        System.out.println();
        System.out.println(compiledPlan);
        System.out.println("-------------");
        //System.out.println(compiledPlan.compareTo(goldenPlan)==0);
        assertEquals(true, compiledPlan.compareTo(goldenPlan) == 0);
    }
        
    public void testSort() throws VisitorException, IOException {
    	String query = "order (load 'a') by $0;";
    	LogicalPlan plan = buildPlan(query);
    	PhysicalPlan pp = buildPhysicalPlan(plan);

    	int MAX_SIZE = 100000;
    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
        pp.explain(baos);
        String compiledPlan = baos.toString();
    	FileInputStream fis = new FileInputStream("test/org/apache/pig/test/data/GoldenFiles/Sort.gld");
        byte[] b = new byte[MAX_SIZE];
        int len = fis.read(b);
        String goldenPlan = new String(b, 0, len);


        System.out.println();
        System.out.println(compiledPlan);
        System.out.println("-------------");
        
        //System.out.println(compiledPlan.compareTo(goldenPlan)==0);
        assertEquals(true, compiledPlan.compareTo(goldenPlan) == 0);
    }
        
    public void testDistinct() throws VisitorException, IOException {
    	String query = "distinct (load 'a');";
    	LogicalPlan plan = buildPlan(query);
    	PhysicalPlan pp = buildPhysicalPlan(plan);
    	
    	int MAX_SIZE = 100000;
    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
        pp.explain(baos);
        String compiledPlan = baos.toString();
    	FileInputStream fis = new FileInputStream("test/org/apache/pig/test/data/GoldenFiles/Distinct.gld");
        byte[] b = new byte[MAX_SIZE];
        int len = fis.read(b);
        String goldenPlan = new String(b, 0, len);


        System.out.println();
        System.out.println(compiledPlan);
        System.out.println("-------------");
        //System.out.println(compiledPlan.compareTo(goldenPlan)==0);
        assertEquals(true, compiledPlan.compareTo(goldenPlan) == 0);
    }
    
    public void testCogroup() throws VisitorException, IOException {
    	String query = "cogroup (load 'a') by ($0 + $1, $0 - $1), (load 'b') by ($0 + $1, $0 - $1);";
    	LogicalPlan plan = buildPlan(query);
    	PhysicalPlan pp = buildPhysicalPlan(plan);
    	
    	
    	int MAX_SIZE = 100000;
    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
        pp.explain(baos);
        String compiledPlan = baos.toString();
    	FileInputStream fis = new FileInputStream("test/org/apache/pig/test/data/GoldenFiles/Cogroup.gld");
        byte[] b = new byte[MAX_SIZE];
        int len = fis.read(b);
        String goldenPlan = new String(b, 0, len);


        System.out.println();
        System.out.println(compiledPlan);
        System.out.println("-------------");
        //System.out.println(compiledPlan.compareTo(goldenPlan)==0);
        assertEquals(true, compiledPlan.compareTo(goldenPlan) == 0);
    }
    
    public void testArithmetic() throws VisitorException, IOException, ExecException {
    	
    	String query = "foreach (load 'A') generate $0 + $1 + '5', $0 - '5' - $1, 'hello';";
    	LogicalPlan lp = buildPlan(query);
    	
    	PhysicalPlan pp = buildPhysicalPlan(lp);
    	
        //Ensure that there is only 1 leaf node
    	assertEquals(1, pp.getLeaves().size());
    	
    	
    	int MAX_SIZE = 100000;
    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
        pp.explain(baos);
        String compiledPlan = baos.toString();
    	FileInputStream fis = new FileInputStream("test/org/apache/pig/test/data/GoldenFiles/Arithmetic.gld");
        byte[] b = new byte[MAX_SIZE];
        int len = fis.read(b);
        String goldenPlan = new String(b, 0, len);


        System.out.println();
        System.out.println(compiledPlan);
        System.out.println("-------------");
        //System.out.println(compiledPlan.compareTo(goldenPlan)==0);
        assertEquals(true, compiledPlan.compareTo(goldenPlan) == 0);
    }
    
    public void testComparison() throws VisitorException, IOException {
    	String query = "filter (load 'a' using " + PigStorage.class.getName() + "(':')) by $0 + $1 > ($0 - $1) * ('4' / '2');";
    	LogicalPlan lp = buildPlan(query);
    	PhysicalPlan pp = buildPhysicalPlan(lp);
    	
        //Ensure that there is only 1 leaf node
    	assertEquals(1, pp.getLeaves().size());
    	
    	int MAX_SIZE = 100000;
    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
        pp.explain(baos);
        String compiledPlan = baos.toString();
    	FileInputStream fis = new FileInputStream("test/org/apache/pig/test/data/GoldenFiles/Comparison.gld");
        byte[] b = new byte[MAX_SIZE];
        int len = fis.read(b);
        String goldenPlan = new String(b, 0, len);


        System.out.println();
        System.out.println(compiledPlan);
        System.out.println("-------------");
        //System.out.println(compiledPlan.compareTo(goldenPlan)==0);
        assertEquals(true, compiledPlan.compareTo(goldenPlan) == 0);
    }

    @Test
    public void testBinCond() throws VisitorException, IOException {
        String query = "foreach (load 'a') generate ($1 == '3'? $2 + $3 : $2 - $3) ;";
        LogicalPlan lp = buildPlan(query);

        PhysicalPlan pp = buildPhysicalPlan(lp);

        
        int MAX_SIZE = 100000;
    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
        pp.explain(baos);
        String compiledPlan = baos.toString();
    	FileInputStream fis = new FileInputStream("test/org/apache/pig/test/data/GoldenFiles/BinCond.gld");
        byte[] b = new byte[MAX_SIZE];
        int len = fis.read(b);
        String goldenPlan = new String(b, 0, len);


        System.out.println();
        System.out.println(compiledPlan);
        System.out.println("-------------");
        //System.out.println(compiledPlan.compareTo(goldenPlan)==0);
        assertEquals(true, compiledPlan.compareTo(goldenPlan) == 0);
    }
    
    
    @Test
    public void testGenerate() throws VisitorException, IOException {
        String query = "foreach (load 'a') generate ($1+$2), ($1-$2), ($1*$2), ($1/$2), ($1%$2), -($1) ;";
        LogicalPlan lp = buildPlan(query);

        PhysicalPlan pp = buildPhysicalPlan(lp);

        
        int MAX_SIZE = 100000;
    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
        pp.explain(baos);
        String compiledPlan = baos.toString();
    	FileInputStream fis = new FileInputStream("test/org/apache/pig/test/data/GoldenFiles/Generate.gld");
        byte[] b = new byte[MAX_SIZE];
        int len = fis.read(b);
        String goldenPlan = new String(b, 0, len);


        System.out.println();
        System.out.println(compiledPlan);
        System.out.println("-------------");
        
        assertEquals(true, compiledPlan.compareTo(goldenPlan) == 0);
    }

    @Test
    public void testUnion() throws VisitorException, IOException {
    	String query = "union (load 'a'), (load 'b'), (load 'c');";
    	LogicalPlan lp = buildPlan(query);
    	PhysicalPlan pp = buildPhysicalPlan(lp);
    	
    	int MAX_SIZE = 100000;
    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
        pp.explain(baos);
        String compiledPlan = baos.toString();
    	FileInputStream fis = new FileInputStream("test/org/apache/pig/test/data/GoldenFiles/Union.gld");
        byte[] b = new byte[MAX_SIZE];
        int len = fis.read(b);
        String goldenPlan = new String(b, 0, len);


        System.out.println();
        System.out.println(compiledPlan);
        System.out.println("-------------");
        
        assertEquals(true, compiledPlan.compareTo(goldenPlan) == 0);
    }
    
    @Test
    public void testSplit() throws VisitorException, IOException {
    	String query = "split (load 'a') into x if $0 < '7', y if $0 > '7';";
    	LogicalPlan plan = buildPlan(query);
    	
    	PhysicalPlan pp = buildPhysicalPlan(plan);
    	
    	int MAX_SIZE = 100000;
    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
        pp.explain(baos);
        String compiledPlan = baos.toString();
    	FileInputStream fis1 = new FileInputStream("test/org/apache/pig/test/data/GoldenFiles/Split1.gld");
    	FileInputStream fis2 = new FileInputStream("test/org/apache/pig/test/data/GoldenFiles/Split2.gld");
        byte[] b1 = new byte[MAX_SIZE];
        byte[] b2 = new byte[MAX_SIZE];
        int len = fis1.read(b1);
        int test = fis2.read(b2);
        //System.out.println("Length of first plan = " + len + " of second = " + test);
        String goldenPlan1 = new String(b1, 0, len);
        String goldenPlan2 = new String(b2, 0, len);


        System.out.println();
        System.out.println(compiledPlan);
        System.out.println("-------------");
        boolean flag = false;
        //System.out.println("GoldenPlan1\n" + goldenPlan1 + "\nGoldenPlan2\n" + goldenPlan2);
        if(compiledPlan.compareTo(goldenPlan1) == 0 || compiledPlan.compareTo(goldenPlan2) == 0)
        	flag = true;
        
        //assertEquals(true, compiledPlan.compareTo(goldenPlan) == 0);
        assertEquals(true, flag);
    	
    }
    
    /*@Test
    public void testQuery4() throws VisitorException {
        String query = "foreach (load 'a') generate AVG($1, $2) ;";
        LogicalPlan lp = buildPlan(query);
        PhysicalPlan pp = buildPhysicalPlan(lp);
        
        DependencyOrderWalker<PhysicalOperator, PhysicalPlan<PhysicalOperator>> walker = new DependencyOrderWalker<PhysicalOperator, PhysicalPlan<PhysicalOperator>>(pp);
    	PhyPlanPrinterVisitor visitor = new PhyPlanPrinterVisitor(pp, walker);
    	visitor.visit();
    	System.out.println(visitor.output);
    }*/
    
    // Helper Functions
    // =================
    
    public PhysicalPlan buildPhysicalPlan(LogicalPlan lp) throws VisitorException {
    	LogToPhyTranslationVisitor visitor = new LogToPhyTranslationVisitor(lp);
    	visitor.setPigContext(pc);
    	visitor.visit();
    	return visitor.getPhysicalPlan();
    }
    
    public LogicalPlan buildPlan(String query) {
        return buildPlan(query, LogicalPlanBuilder.class.getClassLoader());
    }

    public LogicalPlan buildPlan(String query, ClassLoader cldr) {
        LogicalPlanBuilder.classloader = cldr;
        PigContext pigContext = new PigContext(ExecType.LOCAL);
        LogicalPlanBuilder builder = new LogicalPlanBuilder(pigContext); //

        try {
            LogicalPlan lp = builder.parse("Test-Plan-Builder",
                                           query,
                                           aliases,
                                           logicalOpTable,
                                           aliasOp,
                                           defineAliases);
            List<LogicalOperator> roots = lp.getRoots();
            
            if(roots.size() > 0) {
                if (logicalOpTable.get(roots.get(0)) instanceof LogicalOperator){
                    System.out.println(query);
                    System.out.println(logicalOpTable.get(roots.get(0)));
                }
                if ((roots.get(0)).getAlias()!=null){
                    aliases.put((roots.get(0)).getAlias(), lp);
                }
            }
            
            System.err.println("Query: " + query);
            
            //Just the top level roots and their children
            //Need a recursive one to travel down the tree
            
            for(LogicalOperator op: lp.getRoots()) {
                System.err.println("Logical Plan Root: " + op.getClass().getName() + " object " + op);    

                List<LogicalOperator> listOp = lp.getSuccessors(op);
                
                if(null != listOp) {
                    Iterator<LogicalOperator> iter = listOp.iterator();
                    while(iter.hasNext()) {
                        LogicalOperator lop = iter.next();
                        System.err.println("Successor: " + lop.getClass().getName() + " object " + lop);
                    }
                }
            }
            
            assertTrue(lp != null);
            return lp;
        } catch (IOException e) {
            // log.error(e);
            //System.err.println("IOException Stack trace for query: " + query);
            //e.printStackTrace();
            fail("IOException: " + e.getMessage());
        } catch (Exception e) {
            log.error(e);
            //System.err.println("Exception Stack trace for query: " + query);
            //e.printStackTrace();
            fail(e.getClass().getName() + ": " + e.getMessage() + " -- " + query);
        }
        return null;
    }
    
    Map<String, LogicalPlan> aliases = new HashMap<String, LogicalPlan>();
    Map<OperatorKey, LogicalOperator> logicalOpTable = new HashMap<OperatorKey, LogicalOperator>();
    Map<String, LogicalOperator> aliasOp = new HashMap<String, LogicalOperator>();
    Map<String, ExpressionOperator> defineAliases = new HashMap<String, ExpressionOperator>();
}