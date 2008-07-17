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

package org.apache.pig.impl.logicalLayer;


import org.apache.pig.impl.logicalLayer.FrontendException;
import org.apache.pig.impl.logicalLayer.schema.Schema;
import org.apache.pig.impl.plan.OperatorKey;
import org.apache.pig.impl.plan.PlanVisitor;
import org.apache.pig.impl.plan.VisitorException;

public class LOBinCond extends ExpressionOperator {

    // BinCond has a conditional expression and two nested queries.
    // If the conditional expression evaluates to true the first nested query
    // is executed else the second nested query is executed

    private static final long serialVersionUID = 2L;
    private ExpressionOperator mCond;
    private ExpressionOperator mLhsOp;
    private ExpressionOperator mRhsOp;

    /**
     * 
     * @param plan
     *            Logical plan this operator is a part of.
     * @param k
     *            Operator key to assign to this node.
     * @param cond
     *            ExpressionOperator the expression specifying condition
     * @param lhsOp
     *            ExpressionOperator query to be executed when condition is true
     * @param rhsOp
     *            ExpressionOperator query to be executed when condition is
     *            false
     */
    public LOBinCond(LogicalPlan plan, OperatorKey k,
            ExpressionOperator cond, ExpressionOperator lhsOp,
            ExpressionOperator rhsOp) {
        super(plan, k);
        mCond = cond;
        mLhsOp = lhsOp;
        mRhsOp = rhsOp;

    }// End Constructor LOBinCond

    public ExpressionOperator getCond() {
        return mCond;
    }

    public ExpressionOperator getLhsOp() {
        return mLhsOp;
    }

    public ExpressionOperator getRhsOp() {
        return mRhsOp;
    }
    
    public void setLhsOp(ExpressionOperator op) {
        mLhsOp = op ;
    }

    public void setRhsOp(ExpressionOperator op) {
        mRhsOp = op;
    }

    @Override
    public void visit(LOVisitor v) throws VisitorException {
        v.visit(this);
    }

	@Override
	public Schema getSchema() throws FrontendException {
		return mSchema;
	}

    @Override
    public Schema.FieldSchema getFieldSchema() throws FrontendException {
		//We need a check of LHS and RHS schemas
        //The type checker perform this task
        if (!mIsFieldSchemaComputed) {
            try {
                mFieldSchema = mLhsOp.getFieldSchema();
                mIsFieldSchemaComputed = true;
            } catch (FrontendException fee) {
                mFieldSchema = null;
                mIsFieldSchemaComputed = false;
                throw fee;
            }
        }
        return mFieldSchema;
    }

    @Override
    public String name() {
        return "BinCond " + mKey.scope + "-" + mKey.id;
    }

    @Override
    public boolean supportsMultipleInputs() {
        return true;
    }

}