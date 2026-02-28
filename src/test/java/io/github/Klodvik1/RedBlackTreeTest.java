package io.github.Klodvik1;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class RedBlackTreeTest {

    private static final int SAME_HASH = 42;

    private static final class Key implements Comparable<Key> {
        private final int id;

        private Key(int id) {
            this.id = id;
        }

        @Override
        public int compareTo(Key other) {
            return Integer.compare(id, other.id);
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Key other && other.id == id;
        }

        @Override
        public int hashCode() {
            return SAME_HASH;
        }

        @Override
        public String toString() {
            return "Key(" + id + ")";
        }
    }

    @Test
    public void insert_find_update_shouldWork() {
        TreeNode<Key, String> root = null;

        RedBlackTree.InsertResult<Key, String> r1 = RedBlackTree.insertOrUpdate(root, SAME_HASH, new Key(10), "A");
        root = r1.root;
        assertFalse("Ожидалась вставка нового элемента", r1.updatedExisting);
        assertNull("Старое значение должно быть null при первой вставке", r1.oldValue);

        TreeNode<Key, String> n10 = RedBlackTree.find(root, SAME_HASH, new Key(10));
        assertNotNull("Элемент должен находиться после вставки", n10);
        assertEquals("A", n10.value);

        RedBlackTree.InsertResult<Key, String> r2 = RedBlackTree.insertOrUpdate(root, SAME_HASH, new Key(10), "B");
        root = r2.root;
        assertTrue("Ожидалось обновление существующего элемента", r2.updatedExisting);
        assertEquals("A", r2.oldValue);

        TreeNode<Key, String> n10b = RedBlackTree.find(root, SAME_HASH, new Key(10));
        assertNotNull("Элемент должен находиться после обновления", n10b);
        assertEquals("B", n10b.value);

        assertValidRedBlackTree(root);
        assertInOrderSorted(root);
    }

    @Test
    public void manyInserts_shouldMaintainRbInvariants() {
        TreeNode<Key, Integer> root = null;

        for (int i = 1; i <= 200; i++) {
            RedBlackTree.InsertResult<Key, Integer> r = RedBlackTree.insertOrUpdate(root, SAME_HASH, new Key(i), i);
            root = r.root;

            assertNotNull("Элемент должен находиться после вставки id=" + i, RedBlackTree.find(root, SAME_HASH, new Key(i)));
            assertValidRedBlackTree(root);
        }

        assertInOrderSorted(root);
    }

    @Test
    public void deletes_shouldMaintainRbInvariants() {
        TreeNode<Key, Integer> root = null;

        for (int i = 1; i <= 100; i++) {
            root = RedBlackTree.insertOrUpdate(root, SAME_HASH, new Key(i), i).root;
        }
        assertValidRedBlackTree(root);

        root = deleteByKey(root, 100);
        assertNull("Элемент должен быть удалён (id=100)", RedBlackTree.find(root, SAME_HASH, new Key(100)));
        assertValidRedBlackTree(root);

        root = deleteByKey(root, 50);
        assertNull("Элемент должен быть удалён (id=50)", RedBlackTree.find(root, SAME_HASH, new Key(50)));
        assertValidRedBlackTree(root);

        root = deleteByKey(root, 1);
        assertNull("Элемент должен быть удалён (id=1)", RedBlackTree.find(root, SAME_HASH, new Key(1)));
        assertValidRedBlackTree(root);

        assertInOrderSorted(root);
    }

    @Test
    public void randomized_ops_shouldMatchReferenceMapByContent() {
        Random rnd = new Random(1);

        TreeNode<Key, Integer> root = null;
        Map<Key, Integer> ref = new TreeMap<>();

        for (int i = 0; i < 20_000; i++) {
            int op = rnd.nextInt(3);
            int id = rnd.nextInt(400);
            Key key = new Key(id);

            if (op == 0) {
                Integer value = rnd.nextInt(10_000);
                ref.put(key, value);
                root = RedBlackTree.insertOrUpdate(root, SAME_HASH, key, value).root;
            } else if (op == 1) {
                ref.remove(key);
                TreeNode<Key, Integer> node = root == null ? null : RedBlackTree.find(root, SAME_HASH, key);
                if (node != null) {
                    root = RedBlackTree.delete(root, node);
                }
            } else {
                Integer expected = ref.get(key);
                TreeNode<Key, Integer> node = root == null ? null : RedBlackTree.find(root, SAME_HASH, key);
                Integer actual = node == null ? null : node.value;
                assertEquals("Значение get() не совпало для id=" + id, expected, actual);
            }

            assertValidRedBlackTree(root);
        }

        Map<Integer, Integer> fromTree = new TreeMap<>();
        for (TreeNode<Key, Integer> n : inOrder(root)) {
            fromTree.put(n.key.id, n.value);
        }

        Map<Integer, Integer> fromRef = new TreeMap<>();
        for (Map.Entry<Key, Integer> e : ref.entrySet()) {
            fromRef.put(e.getKey().id, e.getValue());
        }

        assertEquals("Итоговое содержимое дерева не совпало с эталоном", fromRef, fromTree);
    }

    private static TreeNode<Key, Integer> deleteByKey(TreeNode<Key, Integer> root, int id) {
        TreeNode<Key, Integer> node = RedBlackTree.find(root, SAME_HASH, new Key(id));
        assertNotNull("Ожидался узел для удаления id=" + id, node);
        return RedBlackTree.delete(root, node);
    }

    private static <K, V> void assertValidRedBlackTree(TreeNode<K, V> root) {
        if (root == null) {
            return;
        }

        assertFalse("Корень дерева должен быть чёрным", root.red);

        int bh = blackHeightAndValidate(root);
        assertTrue("Чёрная высота должна быть больше 0", bh > 0);
    }

    private static <K, V> int blackHeightAndValidate(TreeNode<K, V> node) {
        if (node == null) {
            return 1;
        }

        if (node.left != null) {
            assertSame("Некорректная ссылка parent у левого ребёнка", node, node.left.parent);
        }
        if (node.right != null) {
            assertSame("Некорректная ссылка parent у правого ребёнка", node, node.right.parent);
        }

        if (node.red) {
            if (node.left != null) {
                assertFalse("У красного узла не может быть красного левого ребёнка", node.left.red);
            }
            if (node.right != null) {
                assertFalse("У красного узла не может быть красного правого ребёнка", node.right.red);
            }
        }

        int leftBh = blackHeightAndValidate(node.left);
        int rightBh = blackHeightAndValidate(node.right);

        assertEquals("Чёрная высота должна быть одинаковой на всех путях", leftBh, rightBh);

        return leftBh + (node.red ? 0 : 1);
    }

    private static <K extends Comparable<K>, V> void assertInOrderSorted(TreeNode<K, V> root) {
        List<TreeNode<K, V>> nodes = inOrder(root);
        for (int i = 1; i < nodes.size(); i++) {
            TreeNode<K, V> prev = nodes.get(i - 1);
            TreeNode<K, V> cur = nodes.get(i);

            int cmpHash = Integer.compare(prev.hash, cur.hash);
            if (cmpHash != 0) {
                assertTrue("Обход in-order должен быть отсортирован по hash", cmpHash <= 0);
            } else {
                assertTrue("Обход in-order должен быть отсортирован по Comparable-ключу", prev.key.compareTo(cur.key) <= 0);
            }
        }
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