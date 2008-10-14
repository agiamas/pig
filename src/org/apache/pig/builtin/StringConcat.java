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
package org.apache.pig.builtin;

import java.io.IOException;
import org.apache.pig.EvalFunc;
import org.apache.pig.backend.executionengine.ExecException;
import org.apache.pig.data.DataType;
import org.apache.pig.data.Tuple;
import org.apache.pig.impl.logicalLayer.schema.Schema;


/**
 * Generates the concatenation of the first two fields of a tuple.
 */
public class StringConcat extends EvalFunc<String> {

    @Override
    public String exec(Tuple input) throws IOException {
        try {
            String s1 = (String)(input.get(0));
            String s2 = (String)(input.get(1));
            if(s1 != null && s2 != null)
            {
                StringBuilder sb = new StringBuilder();
                sb.append(s1);
                sb.append(s2);
                return sb.toString();
            } else {
                return null;
            } 
        } catch (ExecException exp) {
            IOException oughtToBeEE = new IOException("Error processing: " +
                input.toString() + exp.getMessage());
            oughtToBeEE.initCause(exp);
            throw oughtToBeEE;
        }
    }
    
    @Override
    public Schema outputSchema(Schema input) {
        return new Schema(new Schema.FieldSchema(null, DataType.CHARARRAY)); 
    }
   
}