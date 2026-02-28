package io.github.Klodvik1;

final class TreeNode<K, V> extends Node<K, V> {
    TreeNode<K, V> parent;
    TreeNode<K, V> left;
    TreeNode<K, V> right;
    boolean red;

    TreeNode(int hash, K key, V value, Node<K, V> next) {
        super(hash, key, value, next);
        this.red = true;
    }
}
