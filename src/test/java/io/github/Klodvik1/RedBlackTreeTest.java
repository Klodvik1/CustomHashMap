package io.github.Klodvik1;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class RedBlackTreeTest {

    private static final int SAME_HASH = 42;

    @Test
    public void insert_find_update_shouldWork_acrossDifferentHashes() {
        TreeNode<ComparableKey, String> root = null;

        ComparableKey k10 = new ComparableKey(10);
        ComparableKey k5 = new ComparableKey(5);
        ComparableKey k20 = new ComparableKey(20);

        int h10 = hashForId(10);
        int h5 = hashForId(5);
        int h20 = hashForId(20);

        root = RedBlackTree.insertOrUpdate(root, h10, k10, "A").root;
        root = RedBlackTree.insertOrUpdate(root, h5, k5, "B").root;
        root = RedBlackTree.insertOrUpdate(root, h20, k20, "C").root;

        TreeNode<ComparableKey, String> n10 = RedBlackTree.find(root, h10, new ComparableKey(10));
        TreeNode<ComparableKey, String> n5 = RedBlackTree.find(root, h5, new ComparableKey(5));
        TreeNode<ComparableKey, String> n20 = RedBlackTree.find(root, h20, new ComparableKey(20));

        assertNotNull(n10);
        assertNotNull(n5);
        assertNotNull(n20);
        assertEquals("A", n10.value);
        assertEquals("B", n5.value);
        assertEquals("C", n20.value);

        RedBlackTree.InsertResult<ComparableKey, String> upd =
                RedBlackTree.insertOrUpdate(root, h5, new ComparableKey(5), "BB");

        root = upd.root;
        assertTrue(upd.updatedExisting);
        assertEquals("B", upd.oldValue);

        TreeNode<ComparableKey, String> n5b = RedBlackTree.find(root, h5, new ComparableKey(5));
        assertNotNull(n5b);
        assertEquals("BB", n5b.value);

        assertValidRedBlackTree(root);
        assertInOrderSortedByTreeComparator(root);
    }

    @Test
    public void manyInsertsDifferentHashes_shouldMaintainRbInvariants_andAllKeysShouldBeFindable() {
        TreeNode<ComparableKey, Integer> root = null;

        for (int i = 1; i <= 2000; i++) {
            ComparableKey k = new ComparableKey(i);
            int h = hashForId(i);

            root = RedBlackTree.insertOrUpdate(root, h, k, i * 3).root;

            TreeNode<ComparableKey, Integer> found = RedBlackTree.find(root, h, new ComparableKey(i));
            assertNotNull(found);
            assertEquals(Integer.valueOf(i * 3), found.value);

            assertValidRedBlackTree(root);
        }

        assertInOrderSortedByTreeComparator(root);
    }

    @Test
    public void deletesAcrossDifferentHashes_shouldMaintainRbInvariants() {
        TreeNode<ComparableKey, Integer> root = null;

        for (int i = 1; i <= 800; i++) {
            ComparableKey k = new ComparableKey(i);
            int h = hashForId(i);
            root = RedBlackTree.insertOrUpdate(root, h, k, i).root;
        }

        int[] idsToDelete = { 800, 1, 400, 799, 2, 401, 250, 600, 500, 300 };
        for (int id : idsToDelete) {
            ComparableKey probe = new ComparableKey(id);
            int h = hashForId(id);

            TreeNode<ComparableKey, Integer> node = RedBlackTree.find(root, h, probe);
            assertNotNull(node);

            root = RedBlackTree.delete(root, node);

            assertNull((root == null) ? null : RedBlackTree.find(root, h, probe));
            assertValidRedBlackTree(root);
        }

        if (root != null) {
            assertInOrderSortedByTreeComparator(root);
        }
    }

    @Test
    public void nonComparableKeys_sameHash_shouldWork_andBeFindableAndDeletable() {
        TreeNode<NonComparableKey, Integer> root = null;

        for (int i = 1; i <= 1000; i++) {
            NonComparableKey k = new NonComparableKey(i);
            root = RedBlackTree.insertOrUpdate(root, SAME_HASH, k, i * 7).root;

            TreeNode<NonComparableKey, Integer> found = RedBlackTree.find(root, SAME_HASH, new NonComparableKey(i));
            assertNotNull(found);
            assertEquals(Integer.valueOf(i * 7), found.value);

            assertValidRedBlackTree(root);
        }

        for (int i = 1; i <= 500; i += 2) {
            NonComparableKey probe = new NonComparableKey(i);
            TreeNode<NonComparableKey, Integer> node = RedBlackTree.find(root, SAME_HASH, probe);
            assertNotNull(node);

            root = RedBlackTree.delete(root, node);

            assertNull((root == null) ? null : RedBlackTree.find(root, SAME_HASH, probe));
            assertValidRedBlackTree(root);
        }

        if (root != null) {
            assertInOrderSortedByTreeComparator(root);
        }
    }

    @Test
    public void nullKey_shouldBeSupported() {
        TreeNode<String, Integer> root = null;

        root = RedBlackTree.insertOrUpdate(root, 0, null, 10).root;
        TreeNode<String, Integer> n1 = RedBlackTree.find(root, 0, null);
        assertNotNull(n1);
        assertEquals(Integer.valueOf(10), n1.value);

        RedBlackTree.InsertResult<String, Integer> upd = RedBlackTree.insertOrUpdate(root, 0, null, 20);
        root = upd.root;
        assertTrue(upd.updatedExisting);
        assertEquals(Integer.valueOf(10), upd.oldValue);

        TreeNode<String, Integer> n2 = RedBlackTree.find(root, 0, null);
        assertNotNull(n2);
        assertEquals(Integer.valueOf(20), n2.value);

        root = RedBlackTree.delete(root, n2);
        assertNull(root == null ? null : RedBlackTree.find(root, 0, null));

        assertValidRedBlackTree(root);
    }

    @Test
    public void insertNewNode_shouldPlaceNodeInTree() {
        TreeNode<ComparableKey, Integer> root = null;

        ComparableKey k1 = new ComparableKey(1);
        ComparableKey k2 = new ComparableKey(2);
        int h1 = hashForId(1);
        int h2 = hashForId(2);

        root = RedBlackTree.insertOrUpdate(root, h1, k1, 10).root;

        TreeNode<ComparableKey, Integer> newNode = new TreeNode<>(h2, k2, 20, null);
        root = RedBlackTree.insertNewNode(root, newNode);

        TreeNode<ComparableKey, Integer> found = RedBlackTree.find(root, h2, new ComparableKey(2));
        assertNotNull(found);
        assertEquals(Integer.valueOf(20), found.value);

        assertValidRedBlackTree(root);
        assertInOrderSortedByTreeComparator(root);
    }

    @Test
    public void randomized_opsDifferentHashes_shouldMatchReferenceMap_andMaintainInvariants() {
        Random rnd = new Random(1);

        TreeNode<ComparableKey, Integer> root = null;
        Map<ComparableKey, Integer> ref = new HashMap<>();

        for (int i = 0; i < 40000; i++) {
            int op = rnd.nextInt(3);
            int id = rnd.nextInt(800);
            ComparableKey key = new ComparableKey(id);
            int hash = hashForId(id);

            if (op == 0) {
                int value = rnd.nextInt(100000);
                ref.put(key, value);
                root = RedBlackTree.insertOrUpdate(root, hash, key, value).root;
            } else if (op == 1) {
                ref.remove(key);
                if (root != null) {
                    TreeNode<ComparableKey, Integer> node = RedBlackTree.find(root, hash, key);
                    if (node != null) {
                        root = RedBlackTree.delete(root, node);
                    }
                }
            } else {
                Integer expected = ref.get(key);
                TreeNode<ComparableKey, Integer> node = (root == null) ? null : RedBlackTree.find(root, hash, key);
                Integer actual = (node == null) ? null : node.value;
                assertEquals("id=" + id, expected, actual);
            }

            assertValidRedBlackTree(root);

            if (i % 5_000 == 0) {
                assertTreeContentEquals(ref, root);
            }
        }

        assertTreeContentEquals(ref, root);
        if (root != null) {
            assertInOrderSortedByTreeComparator(root);
        }
    }

    @Test
    public void comparableButNotConsistentWithEquals_shouldNotLoseKeys() {
        TreeNode<WeirdComparableKey, Integer> root = null;

        int group = 0;
        int hash = SAME_HASH;

        for (int i = 1; i <= 200; i++) {
            WeirdComparableKey k = new WeirdComparableKey(i, group);
            root = RedBlackTree.insertOrUpdate(root, hash, k, i).root;
            assertValidRedBlackTree(root);
        }

        for (int i = 1; i <= 200; i++) {
            WeirdComparableKey probe = new WeirdComparableKey(i, group);
            TreeNode<WeirdComparableKey, Integer> node = RedBlackTree.find(root, hash, probe);
            assertNotNull(node);
            assertEquals(Integer.valueOf(i), node.value);
        }
    }

    private record ComparableKey(int id) implements Comparable<ComparableKey> {

        @Override
        public int compareTo(ComparableKey other) {
            return Integer.compare(this.id, other.id);
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof ComparableKey other && other.id == this.id;
        }

    }

    private record NonComparableKey(int id) {

        @Override
        public boolean equals(Object o) {
            return o instanceof NonComparableKey other && other.id == this.id;
        }

    }

    private record WeirdComparableKey(int id, int group) implements Comparable<WeirdComparableKey> {

        @Override
        public int compareTo(WeirdComparableKey other) {
            return Integer.compare(this.group, other.group);
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof WeirdComparableKey other && other.id == this.id;
        }

        @Override
        public int hashCode() {
            return id;
        }
    }

    private static int hashForId(int id) {
        int h = id * 0x9E3779B9;
        return h ^ (h >>> 16);
    }

    private static void assertTreeContentEquals(Map<ComparableKey, Integer> expected, TreeNode<ComparableKey, Integer> root) {
        if (root == null) {
            assertTrue(expected.isEmpty());
            return;
        }

        for (Map.Entry<ComparableKey, Integer> e : expected.entrySet()) {
            int h = hashForId(e.getKey().id);
            TreeNode<ComparableKey, Integer> found = RedBlackTree.find(root, h, e.getKey());
            assertNotNull(found);
            assertEquals(e.getValue(), found.value);
        }

        Map<ComparableKey, Integer> actual = new HashMap<>();
        for (TreeNode<ComparableKey, Integer> n : inOrder(root)) {
            actual.put(n.key, n.value);
        }
        assertEquals(expected, actual);
    }

    private static <K, V> void assertValidRedBlackTree(TreeNode<K, V> root) {
        if (root == null) {
            return;
        }

        assertFalse(root.red);
        int bh = blackHeightAndValidate(root);
        assertTrue(bh > 0);
    }

    private static <K, V> int blackHeightAndValidate(TreeNode<K, V> node) {
        if (node == null) {
            return 1;
        }

        if (node.left != null) {
            assertSame(node, node.left.parent);
        }
        if (node.right != null) {
            assertSame(node, node.right.parent);
        }

        if (node.red) {
            if (node.left != null) {
                assertFalse(node.left.red);
            }
            if (node.right != null) {
                assertFalse(node.right.red);
            }
        }

        int leftBh = blackHeightAndValidate(node.left);
        int rightBh = blackHeightAndValidate(node.right);

        assertEquals(leftBh, rightBh);

        return leftBh + (node.red ? 0 : 1);
    }

    private static <K, V> void assertInOrderSortedByTreeComparator(TreeNode<K, V> root) {
        List<TreeNode<K, V>> nodes = inOrder(root);
        for (int i = 1; i < nodes.size(); i++) {
            TreeNode<K, V> prev = nodes.get(i - 1);
            TreeNode<K, V> cur = nodes.get(i);

            int cmp = compareAsTree(prev.hash, prev.key, cur.hash, cur.key);
            assertTrue(cmp <= 0);
        }
    }

    private static int compareAsTree(int hashA, Object keyA, int hashB, Object keyB) {
        int cmpHash = Integer.compare(hashA, hashB);
        if (cmpHash != 0) {
            return cmpHash;
        }

        if (Objects.equals(keyA, keyB)) {
            return 0;
        }

        Integer cmpComparable = comparableCompare(keyA, keyB);
        if (cmpComparable != null) {
            return cmpComparable;
        }

        int tie = tieBreakKeys(keyA, keyB);
        if (tie != 0) {
            return tie;
        }

        return Integer.compare(System.identityHashCode(keyA), System.identityHashCode(keyB));
    }

    private static Integer comparableCompare(Object a, Object b) {
        if (a == null || b == null) return null;
        if (a.getClass() != b.getClass()) return null;
        if (!(a instanceof Comparable<?>)) return null;
        @SuppressWarnings("unchecked")
        Comparable<Object> ca = (Comparable<Object>) a;
        return ca.compareTo(b);
    }

    private static int tieBreakKeys(Object a, Object b) {
        if (a == b) {
            return 0;
        }
        if (a == null) {
            return -1;
        }
        if (b == null) {
            return 1;
        }

        int classCompare = a.getClass().getName().compareTo(b.getClass().getName());
        if (classCompare != 0) return classCompare;

        return Integer.compare(System.identityHashCode(a), System.identityHashCode(b));
    }

    private static <K, V> List<TreeNode<K, V>> inOrder(TreeNode<K, V> root) {
        List<TreeNode<K, V>> result = new ArrayList<>();
        Deque<TreeNode<K, V>> stack = new ArrayDeque<>();
        TreeNode<K, V> current = root;

        while (current != null || !stack.isEmpty()) {
            while (current != null) {
                stack.push(current);
                current = current.left;
            }

            TreeNode<K, V> node = stack.pop();
            result.add(node);
            current = node.right;
        }

        return result;
    }
}