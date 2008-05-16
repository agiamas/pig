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
package org.apache.pig.impl.plan;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * An implementation of multi-map.  We can't use Apache commons
 * MultiValueMap because it isn't serializable.  And we don't want to use
 * MultiHashMap, as it is marked deprecated.
 * 
 * This class can't extend Map, because it needs to change the semantics of
 * put, so that you give it one key and one value, and it either creates a
 * new entry with the key and a new collection of value (if the is not yet
 * in the map) or adds the values to the existing collection for the key
 * (if the key is already in the map).
 */
public class MultiMap<K, V> implements Serializable {

    private HashMap<K, ArrayList<V>> mMap = new HashMap<K, ArrayList<V>>();

    /**
     * Add an element to the map.
     * @param key The key to store the value under.  If the key already
     * exists the value will be added to the collection for that key, it
     * will not replace the existing value (as in a standard map).
     * @param value value to store.
     */
    public void put(K key, V value) {
        ArrayList<V> list = mMap.get(key);
        if (list == null) {
            list = new ArrayList<V>();
            list.add(value);
            mMap.put(key, list);
        } else {
            list.add(value);
        }
    }

    /**
     * Get the collection of values associated with a given key.
     * @param key Key to fetch values for.
     * @return collection of values, or null if the key is not in the map.
     */
    public Collection<V> get(K key) {
        return mMap.get(key);
    }

    /**
     * Remove one value from an existing key.  If that is the last value
     * for the key, then remove the key too.
     * @param key Key to remove the value from.
     * @param value Value to remove.
     * @return The value being removed, or null if the key or value does
     * not exist.
     */
    public V remove(K key, V value) {
        ArrayList<V> list = mMap.get(key);
        if (list == null) return null;

        Iterator<V> i = list.iterator();
        V keeper = null;
        while (i.hasNext()) {
            keeper = i.next();
            if (keeper.equals(value)) {
                i.remove();
                break;
            }
        }

        if (list.size() == 0) {
            mMap.remove(key);
        }

        return keeper;
    }

    /**
     * Get a set of all the keys in this map.
     * @return Set of keys.
     */
    public Set<K> keySet() {
        return mMap.keySet();
    }



}