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

import java.util.Map;
import java.util.Random;

import org.apache.pig.backend.executionengine.ExecException;
import org.apache.pig.data.DataBag;
import org.apache.pig.data.DataByteArray;
import org.apache.pig.data.DataType;
import org.apache.pig.data.Tuple;
import org.apache.pig.impl.physicalLayer.POStatus;
import org.apache.pig.impl.physicalLayer.Result;
import org.apache.pig.impl.physicalLayer.expressionOperators.ConstantExpression;
import org.apache.pig.impl.physicalLayer.expressionOperators.LessThanExpr;
import org.apache.pig.test.utils.GenPhyOp;
import org.apache.pig.test.utils.GenRandomData;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class TestLessThan extends junit.framework.TestCase {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testIntegerGt() throws Exception {
        ConstantExpression lt = GenPhyOp.exprConst();
        lt.setValue(new Integer(1));
        ConstantExpression rt = GenPhyOp.exprConst();
        rt.setValue(new Integer(0));
        LessThanExpr g = GenPhyOp.compLessThanExpr();
        g.setLhs(lt);
        g.setRhs(rt);
        g.setOperandType(DataType.INTEGER);
        Result r = g.getNext(new Boolean(true));
        assertEquals(POStatus.STATUS_OK, r.returnStatus);
        assertFalse((Boolean)r.result);
    }

    @Test
    public void testIntegerLt() throws Exception {
        ConstantExpression lt = GenPhyOp.exprConst();
        lt.setValue(new Integer(0));
        ConstantExpression rt = GenPhyOp.exprConst();
        rt.setValue(new Integer(1));
        LessThanExpr g = GenPhyOp.compLessThanExpr();
        g.setLhs(lt);
        g.setRhs(rt);
        g.setOperandType(DataType.INTEGER);
        Result r = g.getNext(new Boolean(true));
        assertEquals(POStatus.STATUS_OK, r.returnStatus);
        assertTrue((Boolean)r.result);
    }

    @Test
    public void testIntegerEq() throws Exception {
        ConstantExpression lt = GenPhyOp.exprConst();
        lt.setValue(new Integer(1));
        ConstantExpression rt = GenPhyOp.exprConst();
        rt.setValue(new Integer(1));
        LessThanExpr g = GenPhyOp.compLessThanExpr();
        g.setLhs(lt);
        g.setRhs(rt);
        g.setOperandType(DataType.INTEGER);
        Result r = g.getNext(new Boolean(true));
        assertEquals(POStatus.STATUS_OK, r.returnStatus);
        assertFalse((Boolean)r.result);
    }

    @Test
    public void testLongGt() throws Exception {
        ConstantExpression lt = GenPhyOp.exprConst();
        lt.setValue(new Long(1L));
        ConstantExpression rt = GenPhyOp.exprConst();
        rt.setValue(new Long(0L));
        LessThanExpr g = GenPhyOp.compLessThanExpr();
        g.setLhs(lt);
        g.setRhs(rt);
        g.setOperandType(DataType.LONG);
        Result r = g.getNext(new Boolean(true));
        assertEquals(POStatus.STATUS_OK, r.returnStatus);
        assertFalse((Boolean)r.result);
    }

    @Test
    public void testLongLt() throws Exception {
        ConstantExpression lt = GenPhyOp.exprConst();
        lt.setValue(new Long(0L));
        ConstantExpression rt = GenPhyOp.exprConst();
        rt.setValue(new Long(1L));
        LessThanExpr g = GenPhyOp.compLessThanExpr();
        g.setLhs(lt);
        g.setRhs(rt);
        g.setOperandType(DataType.LONG);
        Result r = g.getNext(new Boolean(true));
        assertEquals(POStatus.STATUS_OK, r.returnStatus);
        assertTrue((Boolean)r.result);
    }

    @Test
    public void testLongEq() throws Exception {
        ConstantExpression lt = GenPhyOp.exprConst();
        lt.setValue(new Long(1L));
        ConstantExpression rt = GenPhyOp.exprConst();
        rt.setValue(new Long(1L));
        LessThanExpr g = GenPhyOp.compLessThanExpr();
        g.setLhs(lt);
        g.setRhs(rt);
        g.setOperandType(DataType.LONG);
        Result r = g.getNext(new Boolean(true));
        assertEquals(POStatus.STATUS_OK, r.returnStatus);
        assertFalse((Boolean)r.result);
    }

    @Test
    public void testFloatGt() throws Exception {
        ConstantExpression lt = GenPhyOp.exprConst();
        lt.setValue(new Float(1.0f));
        ConstantExpression rt = GenPhyOp.exprConst();
        rt.setValue(new Float(0.0f));
        LessThanExpr g = GenPhyOp.compLessThanExpr();
        g.setLhs(lt);
        g.setRhs(rt);
        g.setOperandType(DataType.FLOAT);
        Result r = g.getNext(new Boolean(true));
        assertEquals(POStatus.STATUS_OK, r.returnStatus);
        assertFalse((Boolean)r.result);
    }

    @Test
    public void testFloatLt() throws Exception {
        ConstantExpression lt = GenPhyOp.exprConst();
        lt.setValue(new Float(0.0f));
        ConstantExpression rt = GenPhyOp.exprConst();
        rt.setValue(new Float(1.0f));
        LessThanExpr g = GenPhyOp.compLessThanExpr();
        g.setLhs(lt);
        g.setRhs(rt);
        g.setOperandType(DataType.FLOAT);
        Result r = g.getNext(new Boolean(true));
        assertEquals(POStatus.STATUS_OK, r.returnStatus);
        assertTrue((Boolean)r.result);
    }

    @Test
    public void testFloatEq() throws Exception {
        ConstantExpression lt = GenPhyOp.exprConst();
        lt.setValue(new Float(1.0f));
        ConstantExpression rt = GenPhyOp.exprConst();
        rt.setValue(new Float(1.0f));
        LessThanExpr g = GenPhyOp.compLessThanExpr();
        g.setLhs(lt);
        g.setRhs(rt);
        g.setOperandType(DataType.FLOAT);
        Result r = g.getNext(new Boolean(true));
        assertEquals(POStatus.STATUS_OK, r.returnStatus);
        assertFalse((Boolean)r.result);
    }

    @Test
    public void testDoubleGt() throws Exception {
        ConstantExpression lt = GenPhyOp.exprConst();
        lt.setValue(new Double(1.0));
        ConstantExpression rt = GenPhyOp.exprConst();
        rt.setValue(new Double(0.0));
        LessThanExpr g = GenPhyOp.compLessThanExpr();
        g.setLhs(lt);
        g.setRhs(rt);
        g.setOperandType(DataType.DOUBLE);
        Result r = g.getNext(new Boolean(true));
        assertEquals(POStatus.STATUS_OK, r.returnStatus);
        assertFalse((Boolean)r.result);
    }

    @Test
    public void testDoubleLt() throws Exception {
        ConstantExpression lt = GenPhyOp.exprConst();
        lt.setValue(new Double(0.0));
        ConstantExpression rt = GenPhyOp.exprConst();
        rt.setValue(new Double(1.0));
        LessThanExpr g = GenPhyOp.compLessThanExpr();
        g.setLhs(lt);
        g.setRhs(rt);
        g.setOperandType(DataType.DOUBLE);
        Result r = g.getNext(new Boolean(true));
        assertEquals(POStatus.STATUS_OK, r.returnStatus);
        assertTrue((Boolean)r.result);
    }

    @Test
    public void testDoubleEq() throws Exception {
        ConstantExpression lt = GenPhyOp.exprConst();
        lt.setValue(new Double(1.0));
        ConstantExpression rt = GenPhyOp.exprConst();
        rt.setValue(new Double(1.0));
        LessThanExpr g = GenPhyOp.compLessThanExpr();
        g.setLhs(lt);
        g.setRhs(rt);
        g.setOperandType(DataType.DOUBLE);
        Result r = g.getNext(new Boolean(true));
        assertEquals(POStatus.STATUS_OK, r.returnStatus);
        assertFalse((Boolean)r.result);
    }

    @Test
    public void testStringGt() throws Exception {
        ConstantExpression lt = GenPhyOp.exprConst();
        lt.setValue(new String("b"));
        ConstantExpression rt = GenPhyOp.exprConst();
        rt.setValue(new String("a"));
        LessThanExpr g = GenPhyOp.compLessThanExpr();
        g.setLhs(lt);
        g.setRhs(rt);
        g.setOperandType(DataType.CHARARRAY);
        Result r = g.getNext(new Boolean(true));
        assertEquals(POStatus.STATUS_OK, r.returnStatus);
        assertFalse((Boolean)r.result);
    }

    @Test
    public void testStringLt() throws Exception {
        ConstantExpression lt = GenPhyOp.exprConst();
        lt.setValue(new String("a"));
        ConstantExpression rt = GenPhyOp.exprConst();
        rt.setValue(new String("b"));
        LessThanExpr g = GenPhyOp.compLessThanExpr();
        g.setLhs(lt);
        g.setRhs(rt);
        g.setOperandType(DataType.CHARARRAY);
        Result r = g.getNext(new Boolean(true));
        assertEquals(POStatus.STATUS_OK, r.returnStatus);
        assertTrue((Boolean)r.result);
    }

    @Test
    public void testStringEq() throws Exception {
        ConstantExpression lt = GenPhyOp.exprConst();
        lt.setValue(new String("b"));
        ConstantExpression rt = GenPhyOp.exprConst();
        rt.setValue(new String("b"));
        LessThanExpr g = GenPhyOp.compLessThanExpr();
        g.setLhs(lt);
        g.setRhs(rt);
        g.setOperandType(DataType.CHARARRAY);
        Result r = g.getNext(new Boolean(true));
        assertEquals(POStatus.STATUS_OK, r.returnStatus);
        assertFalse((Boolean)r.result);
    }

    @Test
    public void testDataByteArrayGt() throws Exception {
        ConstantExpression lt = GenPhyOp.exprConst();
        lt.setValue(new DataByteArray("b"));
        ConstantExpression rt = GenPhyOp.exprConst();
        rt.setValue(new DataByteArray("a"));
        LessThanExpr g = GenPhyOp.compLessThanExpr();
        g.setLhs(lt);
        g.setRhs(rt);
        g.setOperandType(DataType.BYTEARRAY);
        Result r = g.getNext(new Boolean(true));
        assertEquals(POStatus.STATUS_OK, r.returnStatus);
        assertFalse((Boolean)r.result);
    }

    @Test
    public void testDataByteArrayLt() throws Exception {
        ConstantExpression lt = GenPhyOp.exprConst();
        lt.setValue(new DataByteArray("a"));
        ConstantExpression rt = GenPhyOp.exprConst();
        rt.setValue(new DataByteArray("b"));
        LessThanExpr g = GenPhyOp.compLessThanExpr();
        g.setLhs(lt);
        g.setRhs(rt);
        g.setOperandType(DataType.BYTEARRAY);
        Result r = g.getNext(new Boolean(true));
        assertEquals(POStatus.STATUS_OK, r.returnStatus);
        assertTrue((Boolean)r.result);
    }

    @Test
    public void testDataByteArrayEq() throws Exception {
        ConstantExpression lt = GenPhyOp.exprConst();
        lt.setValue(new DataByteArray("b"));
        ConstantExpression rt = GenPhyOp.exprConst();
        rt.setValue(new DataByteArray("b"));
        LessThanExpr g = GenPhyOp.compLessThanExpr();
        g.setLhs(lt);
        g.setRhs(rt);
        g.setOperandType(DataType.BYTEARRAY);
        Result r = g.getNext(new Boolean(true));
        assertEquals(POStatus.STATUS_OK, r.returnStatus);
        assertFalse((Boolean)r.result);
    }

}