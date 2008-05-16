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
package org.apache.pig.data;

import java.lang.Class;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.apache.hadoop.io.BooleanWritable;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;
import org.apache.pig.backend.executionengine.ExecException;

/**
 * A class of static final values used to encode data type.  This could be
 * done as an enumeration, but it done as byte codes instead to save
 * creating objects.  A few utility functions are also included.
 */
public class DataType {
    // IMPORTANT! This list can be used to record values of data on disk,
    // so do not change the values.  You may strand user data.
    // IMPORTANT! Order matters here, as compare() below uses the order to
    // order unlike datatypes.  Don't change this ordering.
    // Spaced unevenly to leave room for new entries without changing
    // values or creating order issues.  
    public static final byte UNKNOWN   =   0;
    public static final byte NULL      =   1;
    public static final byte BOOLEAN   =   5;
    public static final byte INTEGER   =  10;
    public static final byte LONG      =  15;
    public static final byte FLOAT     =  20;
    public static final byte DOUBLE    =  25;
    public static final byte BYTEARRAY =  50;
    public static final byte CHARARRAY =  55;
    public static final byte MAP       = 100;
    public static final byte TUPLE     = 110;
    public static final byte BAG       = 120;
    public static final byte ERROR     =  -1;

    /**
     * Determine the datatype of an object.
     * @param o Object to test.
     * @return byte code of the type, or ERROR if we don't know.
     */
    public static byte findType(Object o) {
        if (o == null) return NULL;

        // Try to put the most common first
        if (o instanceof DataByteArray) return BYTEARRAY;
        else if (o instanceof String) return CHARARRAY;
        else if (o instanceof Tuple) return TUPLE;
        else if (o instanceof DataBag) return BAG;
        else if (o instanceof Integer) return INTEGER;
        else if (o instanceof Long) return LONG;
        else if (o instanceof Map) return MAP;
        else if (o instanceof Float) return FLOAT;
        else if (o instanceof Double) return DOUBLE;
        else if (o instanceof Boolean) return BOOLEAN;
        else return ERROR;
    }

    /**
     * Given a Type object determine the data type it represents.  This isn't
     * cheap, as it uses reflection, so use sparingly.
     * @param t Type to examine
     * @return byte code of the type, or ERROR if we don't know.
     */
    public static byte findType(Type t) {
        if (t == null) return NULL;

        // Try to put the most common first
        if (t == DataByteArray.class) return BYTEARRAY;
        else if (t == String.class) return CHARARRAY;
        else if (t == Integer.class) return INTEGER;
        else if (t == Long.class) return LONG;
        else if (t == Float.class) return FLOAT;
        else if (t == Double.class) return DOUBLE;
        else if (t == Boolean.class) return BOOLEAN;
        else {
            // Might be a tuple or a bag, need to check the interfaces it
            // implements
            if (t instanceof Class) {
                Class c = (Class)t;
                Class[] interfaces = c.getInterfaces();
                for (int i = 0; i < interfaces.length; i++) {
                    if (interfaces[i].getName().equals("org.apache.pig.data.Tuple")) {
                        return TUPLE;
                    } else if (interfaces[i].getName().equals("org.apache.pig.data.DataBag")) {
                        return BAG;
                    } else if (interfaces[i].getName().equals("java.util.Map")) {
                        return MAP;
                    }
                }
            }
            return ERROR;
        }
    }
    
    static BooleanWritable boolWrit = new BooleanWritable();
    static BytesWritable bytesWrit = new BytesWritable();
    static Text stringWrit = new Text();
    static FloatWritable floatWrit = new FloatWritable();
    static IntWritable intWrit = new IntWritable();
    static LongWritable longWrit = new LongWritable();
    static DataBag defDB = BagFactory.getInstance().newDefaultBag();
    static Tuple defTup = TupleFactory.getInstance().newTuple();
    
    public static WritableComparable getWritableComparableTypes(Object o) throws ExecException{
        WritableComparable wcKey = null;
        Map<Byte, String> typeToName = genTypeToNameMap();
        byte type = DataType.findType(o);
        switch (type) {
        case DataType.BAG:
            wcKey = (DataBag) o;
            break;
        case DataType.BOOLEAN:
            boolWrit.set((Boolean)o);
            wcKey = boolWrit;
            break;
        case DataType.BYTEARRAY:
            byte[] dbaBytes = ((DataByteArray) o).get();
            bytesWrit.set(dbaBytes,0,dbaBytes.length);
            wcKey = bytesWrit;
            break;
        case DataType.CHARARRAY:
            stringWrit.set((String) o);
            wcKey = stringWrit;
            break;
        // Currently Hadoop does not have a DoubleWritable
        // case DataType.DOUBLE:
        case DataType.FLOAT:
            floatWrit.set((Float) o);
            wcKey = floatWrit;
            break;
        case DataType.INTEGER:
            intWrit.set((Integer) o);
            wcKey = intWrit;
            break;
        case DataType.LONG:
            longWrit.set((Long) o);
            wcKey = longWrit;
            break;
        case DataType.TUPLE:
            wcKey = (Tuple) o;
            break;
//        case DataType.MAP:
            // Hmm, This is problematic
            // Need a deep clone to convert a Map into
            // MapWritable
            // wcKey = new MapWritable();
//            break;
        default:
            throw new ExecException("The type "
                    + typeToName.get(type)
                    + " cannot be collected as a Key type");
        }
        return wcKey;
    }
    
    public static WritableComparable getWritableComparableTypes(byte type) throws ExecException{
        WritableComparable wcKey = null;
        Map<Byte, String> typeToName = genTypeToNameMap();
         switch (type) {
        case DataType.BAG:
            wcKey = defDB;
            break;
        case DataType.BOOLEAN:
            wcKey = boolWrit;
            break;
        case DataType.BYTEARRAY:
            wcKey = bytesWrit;
            break;
        case DataType.CHARARRAY:
            wcKey = stringWrit;
            break;
        // Currently Hadoop does not have a DoubleWritable
        // case DataType.DOUBLE:
        case DataType.FLOAT:
            wcKey = floatWrit;
            break;
        case DataType.INTEGER:
            wcKey = intWrit;
            break;
        case DataType.LONG:
            wcKey = longWrit;
            break;
        case DataType.TUPLE:
            wcKey = defTup;
            break;
//        case DataType.MAP:
            // Hmm, This is problematic
            // Need a deep clone to convert a Map into
            // MapWritable
            // wcKey = new MapWritable();
//            break;
        default:
            throw new ExecException("The type "
                    + typeToName.get(type)
                    + " cannot be collected as a Key type");
        }
        return wcKey;
    }
    
    /*public static WritableComparable getWritableComparableTypes(Object o) throws ExecException{
        WritableComparable wcKey = null;
        Map<Byte, String> typeToName = genTypeToNameMap();
        byte type = DataType.findType(o);
        switch (type) {
        case DataType.BAG:
            wcKey = (DataBag) o;
            break;
        case DataType.BOOLEAN:
            wcKey = new BooleanWritable((Boolean) o);
            break;
        case DataType.BYTEARRAY:
            wcKey = new BytesWritable(((DataByteArray) o)
                    .get());
            break;
        case DataType.CHARARRAY:
            wcKey = new Text((String) o);
            break;
        // Currently Hadoop does not have a DoubleWritable
        // case DataType.DOUBLE:
        case DataType.FLOAT:
            wcKey = new FloatWritable((Float) o);
            break;
        case DataType.INTEGER:
            wcKey = new IntWritable((Integer) o);
            break;
        case DataType.LONG:
            wcKey = new LongWritable((Long) o);
            break;
        case DataType.TUPLE:
            wcKey = (Tuple) o;
            break;
        case DataType.MAP:
            // Hmm, This is problematic
            // Need a deep clone to convert a Map into
            // MapWritable
            // wcKey = new MapWritable();
            break;
        default:
            throw new ExecException("The type "
                    + typeToName.get(type)
                    + " cannot be collected as a Key type");
        }
        return wcKey;
    }
    
    public static WritableComparable getWritableComparableTypes(byte type) throws ExecException{
        WritableComparable wcKey = null;
        Map<Byte, String> typeToName = genTypeToNameMap();
         switch (type) {
        case DataType.BAG:
            wcKey = DefaultBagFactory.getInstance().newDefaultBag();
            break;
        case DataType.BOOLEAN:
            wcKey = new BooleanWritable();
            break;
        case DataType.BYTEARRAY:
            wcKey = new BytesWritable();
            break;
        case DataType.CHARARRAY:
            wcKey = new Text();
            break;
        // Currently Hadoop does not have a DoubleWritable
        // case DataType.DOUBLE:
        case DataType.FLOAT:
            wcKey = new FloatWritable();
            break;
        case DataType.INTEGER:
            wcKey = new IntWritable();
            break;
        case DataType.LONG:
            wcKey = new LongWritable();
            break;
        case DataType.TUPLE:
            wcKey = TupleFactory.getInstance().newTuple();
            break;
        case DataType.MAP:
            // Hmm, This is problematic
            // Need a deep clone to convert a Map into
            // MapWritable
            // wcKey = new MapWritable();
            break;
        default:
            throw new ExecException("The type "
                    + typeToName.get(type)
                    + " cannot be collected as a Key type");
        }
        return wcKey;
    }*/
    
    public static int numTypes(){
        byte[] types = genAllTypes();
        return types.length;
    }
    public static byte[] genAllTypes(){
        byte[] types = { DataType.BAG, DataType.BOOLEAN, DataType.BYTEARRAY, DataType.CHARARRAY, 
                DataType.DOUBLE, DataType.FLOAT, DataType.INTEGER, DataType.LONG, DataType.MAP, DataType.TUPLE};
        return types;
    }
    
    private static String[] genAllTypeNames(){
        String[] names = { "BAG", "BOOLEAN", "BYTEARRAY", "CHARARRAY", "DOUBLE", "FLOAT", "INTEGER", "LONG", 
                "MAP", "TUPLE" };
        return names;
    }
    
    public static Map<Byte, String> genTypeToNameMap(){
        byte[] types = genAllTypes();
        String[] names = genAllTypeNames();
        Map<Byte,String> ret = new HashMap<Byte, String>();
        for(int i=0;i<types.length;i++){
            ret.put(types[i], names[i]);
        }
        return ret;
    }

    public static Map<String, Byte> genNameToTypeMap(){
        byte[] types = genAllTypes();
        String[] names = genAllTypeNames();
        Map<String, Byte> ret = new HashMap<String, Byte>();
        for(int i=0;i<types.length;i++){
            ret.put(names[i], types[i]);
        }
        return ret;
    }

    /**
     * Get the type name.
     * @param o Object to test.
     * @return type name, as a String.
     */
    public static String findTypeName(Object o) {
        return findTypeName(findType(o));
    }
    
    public static Object convertToPigType(WritableComparable key) {
        if ((key instanceof DataBag) || (key instanceof Tuple))
            return key;
        if (key instanceof BooleanWritable)
            return ((BooleanWritable) key).get();
        if (key instanceof BytesWritable)
            return new DataByteArray(((BytesWritable) key).get());
        if (key instanceof Text)
            return ((Text) key).toString();
        if (key instanceof FloatWritable)
            return ((FloatWritable) key).get();
        if (key instanceof IntWritable)
            return ((IntWritable) key).get();
        if (key instanceof LongWritable)
            return ((LongWritable) key).get();
        return null;
    }

    /**
     * Get the type name from the type byte code
     * @param dt Type byte code
     * @return type name, as a String.
     */
    public static String findTypeName(byte dt) {
        switch (dt) {
        case NULL:      return "NULL";
        case BOOLEAN:   return "boolean";
        case INTEGER:   return "integer";
        case LONG:      return "long";
        case FLOAT:     return "float";
        case DOUBLE:    return "double";
        case BYTEARRAY: return "bytearray";
        case CHARARRAY: return "chararray";
        case MAP:       return "map";
        case TUPLE:     return "tuple";
        case BAG:       return "bag";
        default: return "Unknown";
        }
    }

    /**
     * Determine whether the this data type is complex.
     * @param dataType Data type code to test.
     * @return true if dataType is bag, tuple, or map.
     */
    public static boolean isComplex(byte dataType) {
        return ((dataType == BAG) || (dataType == TUPLE) ||
            (dataType == MAP));
    }

    /**
     * Determine whether the object is complex or atomic.
     * @param o Object to determine type of.
     * @return true if dataType is bag, tuple, or map.
     */
    public static boolean isComplex(Object o) {
        return isComplex(findType(o));
    }

    /**
     * Determine whether the this data type is atomic.
     * @param dataType Data type code to test.
     * @return true if dataType is bytearray, chararray, integer, long,
     * float, or boolean.
     */
    public static boolean isAtomic(byte dataType) {
        return ((dataType == BYTEARRAY) || (dataType == CHARARRAY) ||
            (dataType == INTEGER) || (dataType == LONG) || 
            (dataType == FLOAT) || (dataType == BOOLEAN));
    }

    /**
     * Determine whether the this data type is atomic.
     * @param o Object to determine type of.
     * @return true if dataType is bytearray, chararray, integer, long,
     * float, or boolean.
     */
    public static boolean isAtomic(Object o) {
        return isAtomic(findType(o));
    }

    /**
     * Determine whether the this data type has a schema.
     * @param o Object to determine if it has a schema
     * @return true if the type can have a alid schema (i.e., bag or tuple)
     */
    public static boolean isSchemaType(Object o) {
        return isSchemaType(findType(o));
    }

    /**
     * Determine whether the this data type has a schema.
     * @param o Object to determine if it has a schema
     * @return true if the type can have a alid schema (i.e., bag or tuple)
     */
    public static boolean isSchemaType(byte dataType) {
        return ((dataType == BAG) || (dataType == TUPLE)); 
    }

    /**
    /**
     * Compare two objects to each other.  This function is necessary
     * because there's no super class that implements compareTo.  This
     * function provides an (arbitrary) ordering of objects of different
     * types as follows:  NULL &lt; BOOLEAN &lt; INTEGER &lt; LONG &lt;
     * FLOAT &lt; DOUBLE * &lt; BYTEARRAY &lt; STRING &lt; MAP &lt;
     * TUPLE &lt; BAG.  No other functions should implement this cross
     * object logic.  They should call this function for it instead.
     * @param o1 First object
     * @param o2 Second object
     * @return -1 if o1 is less, 0 if they are equal, 1 if o2 is less.
     */
    public static int compare(Object o1, Object o2) {
        byte dt1 = findType(o1);
        byte dt2 = findType(o2);

        if (dt1 == dt2) {
            switch (dt1) {
            case NULL:
                return 0;

            case BOOLEAN:
                return ((Boolean)o1).compareTo((Boolean)o2);

            case INTEGER:
                return ((Integer)o1).compareTo((Integer)o2);

            case LONG:
                return ((Long)o1).compareTo((Long)o2);

            case FLOAT:
                return ((Float)o1).compareTo((Float)o2);

            case DOUBLE:
                return ((Double)o1).compareTo((Double)o2);

            case BYTEARRAY:
                return ((DataByteArray)o1).compareTo((DataByteArray)o2);

            case CHARARRAY:
                return ((String)o1).compareTo((String)o2);

            case MAP: {
                Map<Object, Object> m1 = (Map<Object, Object>)o1;
                Map<Object, Object> m2 = (Map<Object, Object>)o2;
                int sz1 = m1.size();
                int sz2 = m2.size();
                if (sz1 < sz2) {
                    return -1;
                } else if (sz1 > sz2) { 
                    return 1;
                } else {
                    // This is bad, but we have to sort the keys of the maps in order
                    // to be commutative.
                    TreeMap<Object, Object> tm1 = new TreeMap<Object, Object>(m1);
                    TreeMap<Object, Object> tm2 = new TreeMap<Object, Object>(m2);
                    Iterator<Map.Entry<Object, Object> > i1 =
                        tm1.entrySet().iterator();
                    Iterator<Map.Entry<Object, Object> > i2 =
                        tm2.entrySet().iterator();
                    while (i1.hasNext()) {
                        Map.Entry<Object, Object> entry1 = i1.next();
                        Map.Entry<Object, Object> entry2 = i2.next();
                        int c = compare(entry1.getKey(), entry2.getKey());
                        if (c != 0) {
                            return c;
                        } else {
                            c = compare(entry1.getValue(), entry2.getValue());
                            if (c != 0) {
                                return c;
                            }
                        } 
                    }
                    return 0;
                }
                      }

            case TUPLE:
                return ((Tuple)o1).compareTo((Tuple)o2);

            case BAG:
                return ((DataBag)o1).compareTo((DataBag)o2);

            default:
                throw new RuntimeException("Unkown type " + dt1 +
                    " in compare");
            }
        } else if (dt1 < dt2) {
            return -1;
        } else {
            return 1;
        }
    }

    /**
     * Force a data object to an Integer, if possible.  Any numeric type
     * can be forced to an Integer (though precision may be lost), as well
     * as CharArray, ByteArray, or Boolean.  Complex types cannot be
     * forced to an Integer.  This isn't particularly efficient, so if you
     * already <b>know</b> that the object you have is an Integer you
     * should just cast it.
     * @return The object as a Integer.
     * @throws ExecException if the type can't be forced to an Integer.
     */
    public static Integer toInteger(Object o) throws ExecException {
        switch (findType(o)) {
        case BOOLEAN:
            if (((Boolean)o) == true) return new Integer(1);
            else return new Integer(0);

        case INTEGER:
            return (Integer)o;

        case LONG:
            return new Integer(((Long)o).intValue());

        case FLOAT:
            return new Integer(((Float)o).intValue());

        case DOUBLE:
            return new Integer(((Double)o).intValue());

        case BYTEARRAY:
            return new Integer(Integer.valueOf(((DataByteArray)o).toString()));

        case CHARARRAY:
            return new Integer(Integer.valueOf((String)o));

        case NULL:
            return null;

        case MAP:
        case TUPLE:
        case BAG:
        case UNKNOWN:
        default:
            throw new ExecException("Cannot convert a " + findTypeName(o) +
                " to an Integer");
        }
    }

    /**
     * Force a data object to a Long, if possible.  Any numeric type
     * can be forced to a Long (though precision may be lost), as well
     * as CharArray, ByteArray, or Boolean.  Complex types cannot be
     * forced to a Long.  This isn't particularly efficient, so if you
     * already <b>know</b> that the object you have is a Long you
     * should just cast it.
     * @return The object as a Long.
     * @throws ExecException if the type can't be forced to a Long.
     */
    public static Long toLong(Object o) throws ExecException {
        switch (findType(o)) {
        case BOOLEAN:
            if (((Boolean)o) == true) return new Long(1);
            else return new Long(0);

        case INTEGER:
            return new Long(((Integer)o).longValue());

        case LONG:
            return (Long)o;

        case FLOAT:
            return new Long(((Float)o).longValue());

        case DOUBLE:
            return new Long(((Double)o).longValue());

        case BYTEARRAY:
            return new Long(Long.valueOf(((DataByteArray)o).toString()));

        case CHARARRAY:
            return new Long(Long.valueOf((String)o));

        case NULL:
            return null;

        case MAP:
        case TUPLE:
        case BAG:
        case UNKNOWN:
        default:
            throw new ExecException("Cannot convert a " + findTypeName(o) +
                " to a Long");
        }
    }

    /**
     * Force a data object to a Float, if possible.  Any numeric type
     * can be forced to a Float (though precision may be lost), as well
     * as CharArray, ByteArray.  Complex types cannot be
     * forced to a Float.  This isn't particularly efficient, so if you
     * already <b>know</b> that the object you have is a Float you
     * should just cast it.
     * @return The object as a Float.
     * @throws ExecException if the type can't be forced to a Float.
     */
    public static Float toFloat(Object o) throws ExecException {
        switch (findType(o)) {
        case INTEGER:
            return new Float(((Integer)o).floatValue());

        case LONG:
            return new Float(((Long)o).floatValue());

        case FLOAT:
            return (Float)o;

        case DOUBLE:
            return new Float(((Double)o).floatValue());

        case BYTEARRAY:
            return new Float(Float.valueOf(((DataByteArray)o).toString()));

        case CHARARRAY:
            return new Float(Float.valueOf((String)o));

        case NULL:
            return null;

        case BOOLEAN:
        case MAP:
        case TUPLE:
        case BAG:
        case UNKNOWN:
        default:
            throw new ExecException("Cannot convert a " + findTypeName(o) +
                " to a Float");
        }
    }

    /**
     * Force a data object to a Double, if possible.  Any numeric type
     * can be forced to a Double, as well
     * as CharArray, ByteArray.  Complex types cannot be
     * forced to a Double.  This isn't particularly efficient, so if you
     * already <b>know</b> that the object you have is a Double you
     * should just cast it.
     * @return The object as a Double.
     * @throws ExecException if the type can't be forced to a Double.
     */
    public static Double toDouble(Object o) throws ExecException {
        switch (findType(o)) {
        case INTEGER:
            return new Double(((Integer)o).doubleValue());

        case LONG:
            return new Double(((Long)o).doubleValue());

        case FLOAT:
            return new Double(((Float)o).doubleValue());

        case DOUBLE:
            return (Double)o;

        case BYTEARRAY:
            return new Double(Double.valueOf(((DataByteArray)o).toString()));

        case CHARARRAY:
            return new Double(Double.valueOf((String)o));

        case NULL:
            return null;

        case BOOLEAN:
        case MAP:
        case TUPLE:
        case BAG:
        case UNKNOWN:
        default:
            throw new ExecException("Cannot convert a " + findTypeName(o) +
                " to a Double");
        }
    }

    /**
     * If this object is a map, return it as a map.
     * This isn't particularly efficient, so if you
     * already <b>know</b> that the object you have is a Map you
     * should just cast it.
     * @return The object as a Double.
     * @throws ExecException if the type can't be forced to a Double.
     */
    public static Map<Object, Object> toMap(Object o) throws ExecException {
        if (o == null) return null;

        if (o instanceof Map) {
            return (Map<Object, Object>)o;
        } else {
            throw new ExecException("Cannot convert a " + findTypeName(o) +
                " to a Map");
        }
    }

    /**
     * If this object is a tuple, return it as a tuple.
     * This isn't particularly efficient, so if you
     * already <b>know</b> that the object you have is a Tuple you
     * should just cast it.
     * @return The object as a Double.
     * @throws ExecException if the type can't be forced to a Double.
     */
    public static Tuple toTuple(Object o) throws ExecException {
        if (o == null) return null;

        if (o instanceof Tuple) {
            return (Tuple)o;
        } else {
            throw new ExecException("Cannot convert a " + findTypeName(o) +
                " to a Tuple");
        }
    }

    /**
     * If this object is a bag, return it as a bag.
     * This isn't particularly efficient, so if you
     * already <b>know</b> that the object you have is a bag you
     * should just cast it.
     * @return The object as a Double.
     * @throws ExecException if the type can't be forced to a Double.
     */
    public static DataBag toBag(Object o) throws ExecException {
        if (o == null) return null;

        if (o instanceof DataBag) {
            return (DataBag)o;
        } else {
            throw new ExecException("Cannot convert a " + findTypeName(o) +
                " to a DataBag");
        }
    }

    /**
     * Purely for debugging
     */
    public static void spillTupleContents(Tuple t, String label) {
        System.out.print("Tuple " + label + " ");
        Iterator<Object> i = t.getAll().iterator();
        for (int j = 0; i.hasNext(); j++) {
            System.out.print(j + ":" + i.next().getClass().getName() + " ");
        }
        System.out.println(t.toString());
    }
    
    public static boolean isNumberType(byte t) {
        switch (t) {
            case INTEGER:   return true ;
            case LONG:      return true ;
            case FLOAT:     return true ;
            case DOUBLE:    return true ;
            default: return false ;
        }        
    }
    
    public static boolean isUsableType(byte t) {
        switch (t) {
            case UNKNOWN:    return false ;
            case NULL:       return false ;
            case ERROR:      return false ;
            default :return true ;
        }
    }
}