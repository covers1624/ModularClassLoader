package net.covers1624.classloader;

public class K2BPair<K> {

    public final K k;
    public final boolean v;

    public K2BPair(K k, boolean v) {
        this.k = k;
        this.v = v;
    }

    public static <K> K2BPair<K> of(K k, boolean v) {
        return new K2BPair<>(k, v);
    }

}
