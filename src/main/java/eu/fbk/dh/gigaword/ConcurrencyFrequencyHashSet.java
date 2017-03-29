package eu.fbk.dh.gigaword;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created with IntelliJ IDEA.
 * User: aprosio
 * Date: 1/15/13
 * Time: 5:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class ConcurrencyFrequencyHashSet<K> implements Serializable {

    private ConcurrentHashMap<K, AtomicInteger> support = new ConcurrentHashMap<>();

    public void add(K o, int quantity) {
        support.putIfAbsent(o, new AtomicInteger(0));
        support.get(o).addAndGet(quantity);
    }

    public ConcurrencyFrequencyHashSet<K> clone() {
        ConcurrencyFrequencyHashSet<K> ret = new ConcurrencyFrequencyHashSet<>();
        for (K k : support.keySet()) {
            ret.add(k, support.get(k).intValue());
        }
        return ret;
    }

    public void addAll(Collection<K> collection) {
        for (K el : collection) {
            add(el);
        }
    }

    public void addAll(ConcurrencyFrequencyHashSet<K> frequencyHashSet) {
        for (K el : frequencyHashSet.keySet()) {
            add(el, frequencyHashSet.get(el));
        }
    }

    public void remove(K o) {
        support.remove(o);
    }

    public void add(K o) {
        add(o, 1);
    }

    public int size() {
        return support.size();
    }

    public K mostFrequent() {
        Iterator it = support.keySet().iterator();
        Integer max = null;
        K o = null;
        while (it.hasNext()) {
            K index = (K) it.next();
            if (max == null || support.get(index).intValue() > max) {
                o = index;
                max = support.get(index).intValue();
            }
        }
        return o;
    }

    public Set<K> keySet() {
        return support.keySet();
    }

    public Integer get(K o) {
        return support.get(o).intValue();
    }

    public Integer getZero(K o) {
        return support.get(o) != null ? support.get(o).intValue() : 0;
    }

    public Integer sum() {
        int total = 0;
        for (K key : support.keySet()) {
            total += support.get(key).intValue();
        }
        return total;
    }

    public Set<K> keySetWithLimit(int limit) {
        HashSet<K> ret = new HashSet<K>();

        Iterator it = support.keySet().iterator();
        while (it.hasNext()) {
            K key = (K) it.next();
            // int value = ((Integer) support.get(key)).intValue();
            int value = support.get(key).intValue();
            if (value >= limit) {
                ret.add(key);
                // ret += key.toString() + "=" + value + "\n";
            }
        }

        // return ret.trim();
        return ret;
    }

    public String toString() {
        return support.toString();
    }
}