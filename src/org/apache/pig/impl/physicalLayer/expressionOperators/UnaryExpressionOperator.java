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
package org.apache.pig.impl.physicalLayer.expressionOperators;

import org.apache.pig.impl.plan.OperatorKey;
import org.apache.pig.impl.physicalLayer.plans.ExprPlanVisitor;
import org.apache.pig.impl.physicalLayer.expressionOperators.ExpressionOperator;
import org.apache.pig.impl.plan.VisitorException;

public abstract class UnaryExpressionOperator extends ExpressionOperator {

    ExpressionOperator input;
    
    public UnaryExpressionOperator(OperatorKey k, int rp) {
        super(k, rp);
        
    }

    public UnaryExpressionOperator(OperatorKey k) {
        super(k);
        
    }

    @Override
    public boolean supportsMultipleInputs() {
        // TODO Auto-generated method stub
        return false;
    }
    
    

}