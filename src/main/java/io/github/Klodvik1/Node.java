package io.github.Klodvik1;

import java.util.Map;
import java.util.Objects;

public class Node<K, V> implements Map.Entry<K, V> {
    int hash;
    K key;
    V value;
    Node<K, V> next;

    Node(int hash, K key, V value, Node<K, V> next) {
        this.hash = hash;
        this.key = key;
        this.value = value;
        this.next = next;
    }

    @Override
    public K getKey() {
        return key;
    }

    @Override
    public V getValue() {
        return value;
    }

    @Override
    public V setValue(V value) {
        V old = this.value;
        this.value = value;
        return old;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(key) ^ Objects.hashCode(value);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Map.Entry<?, ?> entry)) {
            return false;
        }
        return Objects.equals(key, entry.getKey()) && Objects.equals(value, entry.getValue());
    }
}
