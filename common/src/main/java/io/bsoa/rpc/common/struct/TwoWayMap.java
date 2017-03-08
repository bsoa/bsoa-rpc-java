/*
 * Copyright © 2016-2017 The BSOA Project
 *
 * The BSOA Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.bsoa.rpc.common.struct;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <p></p>
 * <p>
 * Created by zhangg on 2017/03/2017/3/5 11:37. <br/>
 *
 * @author <a href=mailto:zhanggeng@howtimeflies.org>GengZhang</a>
 */
@ThreadSafe
public class TwoWayMap<K, V> implements Map<K, V> {

    private ConcurrentHashMap<K, V> kv_map = new ConcurrentHashMap<K, V>();

    private ConcurrentHashMap<V, K> vk_map = new ConcurrentHashMap<V, K>();

    private ReentrantLock lock = new ReentrantLock();

    @Override
    public int size() {
        return kv_map.size();
    }

    @Override
    public boolean isEmpty() {
        return kv_map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return kv_map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return vk_map.containsKey(value);
    }

    @Override
    public V get(Object key) {
        return kv_map.get(key);
    }

    public K getKey(Object value) {
        return vk_map.get(value);
    }

    @Override
    public V put(K key, V value) {
        lock.tryLock();
        try {
            kv_map.put(key, value);
            vk_map.put(value, key);
            return value;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public V remove(Object key) {
        lock.tryLock();
        try {
            V value = kv_map.remove(key);
            vk_map.remove(value);
            return value;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        for (Entry<? extends K, ? extends V> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void clear() {
        lock.tryLock();
        try {
            kv_map.clear();
            vk_map.clear();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Set<K> keySet() {
        return kv_map.keySet();
    }

    @Override
    public Collection<V> values() {
        return kv_map.values();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return kv_map.entrySet();
    }
}
