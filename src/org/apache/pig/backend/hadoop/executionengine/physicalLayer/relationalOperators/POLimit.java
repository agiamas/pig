package org.apache.pig.backend.hadoop.executionengine.physicalLayer.relationalOperators;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pig.backend.executionengine.ExecException;
import org.apache.pig.data.DataType;
import org.apache.pig.data.Tuple;
import org.apache.pig.backend.hadoop.executionengine.physicalLayer.POStatus;
import org.apache.pig.backend.hadoop.executionengine.physicalLayer.PhysicalOperator;
import org.apache.pig.backend.hadoop.executionengine.physicalLayer.Result;
import org.apache.pig.backend.hadoop.executionengine.physicalLayer.expressionOperators.ComparisonOperator;
import org.apache.pig.backend.hadoop.executionengine.physicalLayer.plans.PhyPlanVisitor;
import org.apache.pig.impl.plan.OperatorKey;
import org.apache.pig.impl.plan.VisitorException;

public class POLimit extends PhysicalOperator {
	   /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private Log log = LogFactory.getLog(getClass());

    // Counts for outputs processed
    private long soFar = 0;
    
    // Number of limited outputs
    long mLimit;

    public POLimit(OperatorKey k) {
        this(k, -1, null);
    }

    public POLimit(OperatorKey k, int rp) {
        this(k, rp, null);
    }

    public POLimit(OperatorKey k, List<PhysicalOperator> inputs) {
        this(k, -1, inputs);
    }

    public POLimit(OperatorKey k, int rp, List<PhysicalOperator> inputs) {
        super(k, rp, inputs);
    }
    
    public void setLimit(long limit)
    {
    	mLimit = limit;
    }
    
    public long getLimit()
    {
    	return mLimit;
    }

    /**
     * Counts the number of tuples processed into static variable soFar, if the number of tuples processed reach the 
     * limit, return EOP; Otherwise, return the tuple 
     */
    @Override
    public Result getNext(Tuple t) throws ExecException {
        Result res = null;
        Result inp = null;
        while (true) {
            inp = processInput();
            if (inp.returnStatus == POStatus.STATUS_EOP
                    || inp.returnStatus == POStatus.STATUS_ERR)
                break;
            
            if (soFar>=mLimit)
            	inp.returnStatus = POStatus.STATUS_EOP;
            
            soFar++;
            break;
        }

        return inp;
    }

    @Override
    public String name() {
        return "Limit - " + mKey.toString();
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
        v.visitLimit(this);
    }
}