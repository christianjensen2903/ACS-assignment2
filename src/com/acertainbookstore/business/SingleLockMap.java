package com.acertainbookstore.business;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


public class SingleLockMap<K, V> implements Map<K, V> {

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    
	/** The mapping of books from ISBN to {@link BookStoreBook}. */
	private Map<K, V> inner_map = null;

    public SingleLockMap() {
        inner_map = new java.util.concurrent.ConcurrentHashMap<K, V>();
    }

    // Put function
    public V put(K key, V value) {
        lock.writeLock().lock();
        try {
            return inner_map.put(key, value);
        }
        finally {
            lock.writeLock().unlock();
        }
    }
    // Get function
    @Override
    public V get(Object key) {
        lock.readLock().lock();
        try {
            return inner_map.get(key);
        }
        finally {
            lock.readLock().unlock();
        }
    }

    public Collection<V> values() {
        lock.readLock().lock();
        try {
            return inner_map.values();
        }
        finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public V remove(Object key) {
        lock.writeLock().lock();
        try {
            return inner_map.remove(key);
        }
        finally {
            lock.writeLock().unlock();
        }
    }

    public void clear() {
        lock.writeLock().lock();
        try {
            inner_map.clear();
        }
        finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean containsKey(Object key) {
        lock.readLock().lock();
        try {
            return inner_map.containsKey(key);
        }
        finally {
            lock.readLock().unlock();
        }
    }

    public Set<Entry<K, V>> entrySet() {
        lock.readLock().lock();
        try {
            return inner_map.entrySet();
        }
        finally {
            lock.readLock().unlock();
        }
    }
    @Override
    public int size() {
        lock.readLock().lock();
        try {
            return inner_map.size();
        }
        finally {
            lock.readLock().unlock();
        }
    }
    @Override
    public boolean isEmpty() {
        lock.readLock().lock();
        try {
            return inner_map.isEmpty();
        }
        finally {
            lock.readLock().unlock();
        }
    }
    @Override
    public boolean containsValue(Object value) {
        lock.readLock().lock();
        try {
            return inner_map.containsValue(value);
        }
        finally {
            lock.readLock().unlock();
        }
    }
    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        lock.writeLock().lock();
        try {
            inner_map.putAll(m);
        }
        finally {
            lock.writeLock().unlock();
        }
    }
    @Override
    public Set<K> keySet() {
        lock.readLock().lock();
        try {
            return inner_map.keySet();
        }
        finally {
            lock.readLock().unlock();
        }
    }   
}