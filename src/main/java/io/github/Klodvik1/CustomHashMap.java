package io.github.Klodvik1;

import java.util.*;

public class CustomHashMap<K, V> extends AbstractMap<K, V> {
    private static final int DEFAULT_CAPACITY = 16;
    private static final int MAX_CAPACITY = 1 << 30;
    private static final float DEFAULT_LOAD_FACTOR = 0.75f;

    private static final int TREEIFY_THRESHOLD = 8;
    private static final int UNTREEIFY_THRESHOLD = 6;
    private static final int MIN_TREEIFY_CAPACITY = 64;

    private Node<K, V>[] table;
    private int size;
    private int threshold;
    private final float loadFactor;
    private int modCount;

    public CustomHashMap() {
        this(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR);
    }

    public CustomHashMap(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    public CustomHashMap(int initialCapacity, float loadFactor) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("initialCapacity < 0");
        }
        if (loadFactor <= 0.0f || Float.isNaN(loadFactor)) {
            throw new IllegalArgumentException("Invalid loadFactor: " + loadFactor);
        }

        int capacity = normalizeCapacity(initialCapacity);
        this.loadFactor = loadFactor;
        this.table = (Node<K, V>[]) new Node[capacity];
        this.threshold = (int) (capacity * loadFactor);
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public void clear() {
        if (size == 0) {
            return;
        }
        modCount++;
        Arrays.fill(table, null);
        size = 0;
    }

    @Override
    public boolean containsKey(Object key) {
        int hash = spreadHash(key);
        int index = bucketIndex(hash, table.length);
        return findNode(table[index], hash, key) != null;
    }

    @Override
    public V get(Object key) {
        int hash = spreadHash(key);
        int index = bucketIndex(hash, table.length);
        Node<K, V> node = findNode(table[index], hash, key);
        return node == null ? null : node.value;
    }

    @Override
    public V put(K key, V value) {
        return putInternal(key, value);
    }

    @Override
    public V remove(Object key) {
        int hash = spreadHash(key);
        int index = bucketIndex(hash, table.length);

        Node<K, V> head = table[index];
        if (head == null) {
            return null;
        }

        if (head instanceof TreeNode<K, V> treeHead) {
            TreeNode<K, V> root = RedBlackTree.rootOf(treeHead);
            TreeNode<K, V> target = RedBlackTree.find(root, hash, key);
            if (target == null) {
                return null;
            }

            V oldValue = target.value;

            TreeNode<K, V> nodeToUnlink = (target.left != null && target.right != null)
                    ? RedBlackTree.successorOf(target)
                    : target;

            unlinkFromChain(index, nodeToUnlink);

            root = RedBlackTree.delete(root, target);
            if (root == null) {
                table[index] = null;
            }

            size--;
            modCount++;

            if (table[index] != null) {
                int bucketSize = countBucketSize(table[index]);
                if (bucketSize <= UNTREEIFY_THRESHOLD) {
                    table[index] = gentrify(index);
                }
            }

            return oldValue;
        }

        Node<K, V> previous = null;
        Node<K, V> current = head;
        while (current != null) {
            if (current.hash == hash && Objects.equals(current.key, key)) {
                V oldValue = current.value;

                if (previous == null) {
                    table[index] = current.next;
                } else {
                    previous.next = current.next;
                }

                current.next = null;
                size--;
                modCount++;
                return oldValue;
            }

            previous = current;
            current = current.next;
        }

        return null;
    }

    private transient Set<Entry<K, V>> entrySet;

    @Override
    public Set<Entry<K, V>> entrySet() {
        Set<Entry<K, V>> result = entrySet;
        if (result == null) {
            result = new EntrySetView();
            entrySet = result;
        }
        return result;
    }

    private final class EntrySetView extends AbstractSet<Entry<K, V>> {
        @Override
        public Iterator<Entry<K, V>> iterator() {
            return new EntryIterator();
        }

        @Override
        public int size() {
            return CustomHashMap.this.size;
        }

        @Override
        public void clear() {
            CustomHashMap.this.clear();
        }
    }

    private final class EntryIterator implements Iterator<Entry<K, V>> {
        private int expectedModCount = modCount;
        private int index;
        private Node<K, V> next;
        private Node<K, V> current;

        private EntryIterator() {
            index = 0;
            next = null;
            current = null;
            advance();
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public Entry<K, V> next() {
            checkForComodification();
            if (next == null) {
                throw new NoSuchElementException();
            }

            current = next;
            next = next.next;
            if (next == null) {
                advance();
            }
            return current;
        }

        @Override
        public void remove() {
            checkForComodification();
            if (current == null) {
                throw new IllegalStateException();
            }
            CustomHashMap.this.remove(current.key);
            current = null;
            expectedModCount = modCount;
        }

        private void advance() {
            Node<K, V>[] tab = table;
            while (index < tab.length) {
                Node<K, V> head = tab[index++];
                if (head != null) {
                    next = head;
                    return;
                }
            }
            next = null;
        }

        private void checkForComodification() {
            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
        }
    }

    private V putInternal(K key, V value) {
        int hash = spreadHash(key);
        Node<K, V>[] tab = table;
        int index = bucketIndex(hash, tab.length);

        Node<K, V> head = tab[index];

        if (head == null) {
            tab[index] = new Node<>(hash, key, value, null);
            size++;
            modCount++;
            ensureCapacityAfterInsert();
            return null;
        }

        if (head instanceof TreeNode<K, V> treeHead) {
            TreeNode<K, V> root = RedBlackTree.rootOf(treeHead);
            RedBlackTree.InsertResult<K, V> result = RedBlackTree.insertOrUpdate(root, hash, key, value);

            if (result.updatedExisting) {
                return result.oldValue;
            }

            TreeNode<K, V> newNode = result.inserted;

            newNode.next = head;
            tab[index] = newNode;

            size++;
            modCount++;

            ensureCapacityAfterInsert();

            return null;
        }

        Node<K, V> current = head;
        while (current != null) {
            if (current.hash == hash && Objects.equals(current.key, key)) {
                V oldValue = current.value;
                current.value = value;
                return oldValue;
            }
            current = current.next;
        }

        tab[index] = new Node<>(hash, key, value, head);
        size++;
        modCount++;

        int bucketSize = countBucketSize(tab[index]);
        if (bucketSize >= TREEIFY_THRESHOLD) {
            if (tab.length < MIN_TREEIFY_CAPACITY) {
                resize(tab.length << 1);
            } else {
                tab[index] = treeify(index);
            }
        } else {
            ensureCapacityAfterInsert();
        }

        return null;
    }

    private void ensureCapacityAfterInsert() {
        if (size > threshold) {
            resize(table.length << 1);
        }
    }

    private void resize(int requestedCapacity) {
        int oldCapacity = table.length;
        if (oldCapacity >= MAX_CAPACITY) {
            threshold = Integer.MAX_VALUE;
            return;
        }

        int newCapacity = normalizeCapacity(requestedCapacity);
        if (newCapacity <= oldCapacity) {
            return;
        }

        Node<K, V>[] oldTable = table;
        Node<K, V>[] newTable = (Node<K, V>[]) new Node[newCapacity];

        for (Node<K, V> kvNode : oldTable) {
            Node<K, V> head = kvNode;
            while (head != null) {
                Node<K, V> next = head.next;
                head.next = null;

                Node<K, V> entry = (head instanceof TreeNode<K, V>)
                        ? new Node<>(head.hash, head.key, head.value, null)
                        : head;

                int index = bucketIndex(entry.hash, newCapacity);
                entry.next = newTable[index];
                newTable[index] = entry;

                head = next;
            }
        }

        table = newTable;
        threshold = (int) (newCapacity * loadFactor);

        if (newCapacity >= MIN_TREEIFY_CAPACITY) {
            for (int i = 0; i < newTable.length; i++) {
                Node<K, V> head = newTable[i];
                if (head != null && !(head instanceof TreeNode)) {
                    int bucketSize = countBucketSize(head);
                    if (bucketSize >= TREEIFY_THRESHOLD) {
                        newTable[i] = treeify(i);
                    }
                }
            }
        }

        modCount++;
    }

    private Node<K, V> treeify(int index) {
        Node<K, V> head = table[index];
        if (head == null || head instanceof TreeNode) {
            return head;
        }

        TreeNode<K, V> root = null;
        TreeNode<K, V> chainHead = null;

        Node<K, V> current = head;
        while (current != null) {
            TreeNode<K, V> node = new TreeNode<>(current.hash, current.key, current.value, null);

            node.next = chainHead;
            chainHead = node;

            root = RedBlackTree.insertNewNode(root, node);

            current = current.next;
        }

        if (root != null) {
            root.red = false;
        }

        return chainHead;
    }

    private Node<K, V> gentrify(int index) {
        Node<K, V> head = table[index];
        if (!(head instanceof TreeNode)) {
            return head;
        }

        Node<K, V> newHead = null;
        Node<K, V> current = head;
        while (current != null) {
            Node<K, V> next = current.next;
            newHead = new Node<>(current.hash, current.key, current.value, newHead);
            current = next;
        }

        return newHead;
    }

    private Node<K, V> findNode(Node<K, V> head, int hash, Object key) {
        if (head == null) {
            return null;
        }

        if (head instanceof TreeNode<K, V> treeHead) {
            TreeNode<K, V> root = RedBlackTree.rootOf(treeHead);
            return RedBlackTree.find(root, hash, key);
        }

        Node<K, V> current = head;
        while (current != null) {
            if (current.hash == hash && Objects.equals(current.key, key)) {
                return current;
            }
            current = current.next;
        }
        return null;
    }

    private int countBucketSize(Node<K, V> head) {
        int count = 0;
        Node<K, V> current = head;
        while (current != null) {
            count++;
            current = current.next;
        }
        return count;
    }

    private int bucketIndex(int hash, int length) {
        return (length - 1) & hash;
    }

    private int spreadHash(Object key) {
        if (key == null) {
            return 0;
        }
        int h = key.hashCode();
        return h ^ (h >>> 16);
    }

    private static int normalizeCapacity(int requested) {
        int capacity = Math.max(DEFAULT_CAPACITY, requested);
        if (capacity >= MAX_CAPACITY) {
            return MAX_CAPACITY;
        }

        int n = capacity - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return n + 1;
    }

    private void unlinkFromChain(int index, TreeNode<K, V> target) {
        Node<K, V> previous = null;
        Node<K, V> current = table[index];

        while (current != null) {
            if (current == target) {
                if (previous == null) {
                    table[index] = current.next;
                } else {
                    previous.next = current.next;
                }
                current.next = null;
                return;
            }
            previous = current;
            current = current.next;
        }
    }
}
