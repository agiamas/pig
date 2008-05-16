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
package org.apache.pig.impl.physicalLayer.topLevelOperators;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pig.backend.executionengine.ExecException;
import org.apache.pig.data.BagFactory;
import org.apache.pig.data.DataBag;
import org.apache.pig.data.DataType;
import org.apache.pig.data.Tuple;
import org.apache.pig.impl.logicalLayer.OperatorKey;
import org.apache.pig.impl.physicalLayer.POStatus;
import org.apache.pig.impl.physicalLayer.Result;
import org.apache.pig.impl.physicalLayer.plans.ExprPlan;
import org.apache.pig.impl.physicalLayer.plans.PhyPlanVisitor;
import org.apache.pig.impl.physicalLayer.topLevelOperators.expressionOperators.ExpressionOperator;
import org.apache.pig.impl.physicalLayer.topLevelOperators.expressionOperators.POUserComparisonFunc;
import org.apache.pig.impl.physicalLayer.topLevelOperators.expressionOperators.POUserFunc;
import org.apache.pig.impl.plan.VisitorException;

/**
 * This implementation is applicable for both the physical plan and for the
 * local backend, as the conversion of physical to mapreduce would see the SORT
 * operator and take necessary steps to convert it to a quantile and a sort job.
 * 
 * This is a blocking operator. The sortedDataBag accumulates Tuples and sorts
 * them only when there an iterator is started. So all the tuples from the input
 * operator should be accumulated and filled into the dataBag. The attachInput
 * method is not applicable here.
 * 
 * 
 */
public class POSort extends PhysicalOperator<PhyPlanVisitor> {

	//private List<Integer> mSortCols;
	private List<ExprPlan> sortPlans;
	private List<Byte> ExprOutputTypes;
	private List<Boolean> mAscCols;
	private POUserComparisonFunc mSortFunc;
	private final Log log = LogFactory.getLog(getClass());

	private boolean inputsAccumulated = false;
	public boolean isUDFComparatorUsed = false;
	private DataBag sortedBag;
	transient Iterator<Tuple> it;

	public POSort(OperatorKey k, int rp, List inp, List<ExprPlan> sortPlans,
			List<Boolean> mAscCols, POUserFunc mSortFunc) {
		super(k, rp, inp);
		//this.mSortCols = mSortCols;
		this.sortPlans = sortPlans;
		this.mAscCols = mAscCols;
		this.mSortFunc = (POUserComparisonFunc) mSortFunc;
		if (mSortFunc == null) {
			sortedBag = BagFactory.getInstance().newSortedBag(
					new SortComparator());
			ExprOutputTypes = new ArrayList<Byte>(sortPlans.size());

			for(ExprPlan plan : sortPlans) {
				ExprOutputTypes.add(plan.getLeaves().get(0).resultType);
			}
		} else {
			sortedBag = BagFactory.getInstance().newSortedBag(
					new UDFSortComparator());
			isUDFComparatorUsed = true;
		}
	}

	public POSort(OperatorKey k, int rp, List inp) {
		super(k, rp, inp);

	}

	public POSort(OperatorKey k, int rp) {
		super(k, rp);

	}

	public POSort(OperatorKey k, List inp) {
		super(k, inp);

	}

	public POSort(OperatorKey k) {
		super(k);

	}
	
	public class SortComparator implements Comparator<Tuple> {
		public int compare(Tuple o1, Tuple o2) {
			int count = 0;
			int ret = 0;
			if(sortPlans == null || sortPlans.size() == 0) 
				return 0;
			for(ExprPlan plan : sortPlans) {
				try {
					plan.attachInput(o1);
					Result res1 = getResult(plan, ExprOutputTypes.get(count));
					plan.attachInput(o2);
					Result res2 = getResult(plan, ExprOutputTypes.get(count));
					if(res1.returnStatus != POStatus.STATUS_OK || res2.returnStatus != POStatus.STATUS_OK) {
						log.error("Error processing the input in the expression plan : " + plan.toString());
					} else {
						if(mAscCols.get(count ++))
							ret = DataType.compare(res1.result, res2.result);
						else
							ret = DataType.compare(res2.result, res1.result);
					}
						
				} catch (ExecException e) {
					log.error("Invalid result while executing the expression plan : " + plan.toString() + "\n" + e.getMessage());
				}
				
			}
			return ret;
		} 
		
		private Result getResult(ExprPlan plan, byte resultType) throws ExecException {
			ExpressionOperator Op = plan.getLeaves().get(0);
			Result res = null;
			
			switch (resultType) {
            case DataType.BYTEARRAY:
                res = Op.getNext(dummyDBA);
                break;
            case DataType.CHARARRAY:
                res = Op.getNext(dummyString);
                break;
            case DataType.DOUBLE:
                res = Op.getNext(dummyDouble);
                break;
            case DataType.FLOAT:
                res = Op.getNext(dummyFloat);
                break;
            case DataType.INTEGER:
                res = Op.getNext(dummyInt);
                break;
            case DataType.LONG:
                res = Op.getNext(dummyLong);
                break;
            }
			return res;
		}
	}

	public class UDFSortComparator implements Comparator<Tuple> {

		public int compare(Tuple t1, Tuple t2) {

			mSortFunc.attachInput(t1, t2);
			Integer i = null;
			Result res = null;
			try {
				res = mSortFunc.getNext(i);
			} catch (ExecException e) {

				log.error("Input not ready. Error on reading from input. "
						+ e.getMessage());
			}
			if (res != null)
				return (Integer) res.result;
			else
				return 0;
		}

	}

	@Override
	public String name() {

		return "POSort - " + mKey.toString();
	}

	@Override
	public boolean isBlocking() {

		return true;
	}

	@Override
	public Result getNext(Tuple t) throws ExecException {
		Result res = new Result();
		if (!inputsAccumulated) {
			res = processInput();
			while (res.returnStatus != POStatus.STATUS_EOP) {
				if (res.returnStatus == POStatus.STATUS_ERR) {
					log.error("Error in reading from the inputs");
					continue;
				} else if (res.returnStatus == POStatus.STATUS_NULL) {
					continue;
				}
				sortedBag.add((Tuple) res.result);
				res = processInput();

			}

			inputsAccumulated = true;

		}
		if (it == null) {
			it = sortedBag.iterator();
		}
		res.result = it.next();
		if (res.result == null)
			res.returnStatus = POStatus.STATUS_EOP;
		else
			res.returnStatus = POStatus.STATUS_OK;
		return res;
	}

	@Override
	public boolean supportsMultipleInputs() {

		return false;
	}

	@Override
	public boolean supportsMultipleOutputs() {

		return false;
	}

	@Override
	public void visit(PhyPlanVisitor v) throws VisitorException {

		v.visitSort(this);
	}

    public List<ExprPlan> getSortPlans() {
        return sortPlans;
    }

    public void setSortPlans(List<ExprPlan> sortPlans) {
        this.sortPlans = sortPlans;
    }

    public POUserComparisonFunc getMSortFunc() {
        return mSortFunc;
    }

    public void setMSortFunc(POUserComparisonFunc sortFunc) {
        mSortFunc = sortFunc;
    }

}