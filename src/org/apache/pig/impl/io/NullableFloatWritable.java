/**
 * 
 */
package org.apache.pig.impl.io;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.FloatWritable;

/**
 *
 */
public class NullableFloatWritable extends FloatWritable {

    private boolean isNull = false;
    
    public static byte NULL = 0x00;
    public static byte NOTNULL = 0x01;
        
    /**
     * 
     */
    public NullableFloatWritable() {
        super();
    }

    /**
     * @param value
     */
    public NullableFloatWritable(float value) {
        super(value);
    }

    /* (non-Javadoc)
     * @see org.apache.hadoop.io.IntWritable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(Object o) {
        // if both are null they are equal only here!
        if(isNull == true && ((NullableFloatWritable)o).isNull())
            return 0;
        else if(isNull == true)
            return -1; 
        else if (((NullableFloatWritable)o).isNull())
            return 1;
        else            
            return super.compareTo(o);
    }

    /* (non-Javadoc)
     * @see org.apache.hadoop.io.IntWritable#readFields(java.io.DataInput)
     */
    @Override
    public void readFields(DataInput in) throws IOException {
        byte nullMarker = in.readByte();
        if(nullMarker == NULL) {
            isNull = true;
        }
        else {
            isNull = false;
            super.readFields(in);
        }
         
    }

    /* (non-Javadoc)
     * @see org.apache.hadoop.io.IntWritable#write(java.io.DataOutput)
     */
    @Override
    public void write(DataOutput out) throws IOException {
        if(isNull()) {
            out.writeByte(NULL);
        } else {
            out.writeByte(NOTNULL);
            super.write(out);
        }
    }

    /**
     * @return the isNull
     */
    public boolean isNull() {
        return isNull;
    }

    /**
     * @param isNull the isNull to set
     */
    public void setNull(boolean isNull) {
        this.isNull = isNull;
    }
    
    

}