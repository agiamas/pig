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

import java.util.List;
import java.util.ArrayList;
import org.apache.pig.impl.logicalLayer.schema.Schema;
import org.apache.pig.impl.logicalLayer.FrontendException;
import org.apache.pig.impl.logicalLayer.parser.ParseException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class ExpressionOperator extends LogicalOperator {

    private static final long serialVersionUID = 2L;
    private static Log log = LogFactory.getLog(ExpressionOperator.class);
    protected boolean mIsFieldSchemaComputed = false;
    protected Schema.FieldSchema mFieldSchema = null;

    /**
     * @param plan
     *            Logical plan this operator is a part of.
     * @param k
     *            Operator key to assign to this node.
     * @param rp
     *            degree of requested parallelism with which to execute this
     *            node.
     */
    public ExpressionOperator(LogicalPlan plan, OperatorKey k, int rp) {
        super(plan, k, rp);
    }

    /**
     * @param plan
     *            Logical plan this operator is a part of.
     * @param k
     *            Operator key to assign to this node.
     */
    public ExpressionOperator(LogicalPlan plan, OperatorKey k) {
        super(plan, k);
    }

    
    @Override
    public boolean supportsMultipleOutputs() {
        return false;
    }

    @Override
    public Schema getSchema() throws FrontendException{
        return mSchema;
    }

    // Default implementation just get type info from mType
    public Schema.FieldSchema getFieldSchema() throws FrontendException {
        Schema.FieldSchema fs = new Schema.FieldSchema(null, mType) ;
        return fs ;
    }

    /**
     * Set the output schema for this operator. If a schema already exists, an
     * attempt will be made to reconcile it with this new schema.
     * 
     * @param schema
     *            Schema to set.
     * @throws ParseException
     *             if there is already a schema and the existing schema cannot
     *             be reconciled with this new schema.
     */
    public final void setFieldSchema(Schema.FieldSchema fs) throws FrontendException {
        // In general, operators don't generate their schema until they're
        // asked, so ask them to do it.
        log.debug("Inside setFieldSchema");
        try {
            getFieldSchema();
        } catch (FrontendException fee) {
            // It's fine, it just means we don't have a schema yet.
        }
        log.debug("After getFieldSchema()");
        if (null == mFieldSchema) {
            log.debug("Operator schema is null; Setting it to new schema");
            mFieldSchema = fs;
        } else {
            log.debug("Reconciling schema");
            log.debug("mFieldSchema: " + mFieldSchema + " fs: " + fs);
            //log.debug("mSchema: " + mSchema + " schema: " + schema);
            try {
                if(null != mFieldSchema.schema) {
                    mFieldSchema.schema.reconcile(fs.schema);
                } else {
                    mFieldSchema.schema = fs.schema;
                }
                mFieldSchema.type = fs.type;
                mFieldSchema.alias = fs.alias;
            } catch (ParseException pe) {
                throw new FrontendException(pe.getMessage());
            }
        }
    }

}
