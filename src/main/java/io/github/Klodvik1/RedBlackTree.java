package io.github.Klodvik1;

import java.util.Objects;

final class RedBlackTree {
    private RedBlackTree() { }

    static final class InsertResult<K, V> {
        final boolean updatedExisting;
        final V oldValue;
        final TreeNode<K, V> inserted;
        final TreeNode<K, V> root;

        InsertResult(boolean updatedExisting, V oldValue, TreeNode<K, V> inserted, TreeNode<K, V> root) {
            this.updatedExisting = updatedExisting;
            this.oldValue = oldValue;
            this.inserted = inserted;
            this.root = root;
        }
    }

    public static <K, V> TreeNode<K, V> rootOf(TreeNode<K, V> node) {
        TreeNode<K, V> current = node;
        while (current != null && current.parent != null) {
            current = current.parent;
        }
        return current;
    }

    public static <K, V> TreeNode<K, V> successorOf(TreeNode<K, V> node) {
        if (node == null) {
            return null;
        }
        if (node.right != null) {
            TreeNode<K, V> p = node.right;
            while (p.left != null) {
                p = p.left;
            }
            return p;
        }
        TreeNode<K, V> p = node.parent;
        TreeNode<K, V> ch = node;
        while (p != null && ch == p.right) {
            ch = p;
            p = p.parent;
        }
        return p;
    }

    public static <K, V> TreeNode<K, V> find(TreeNode<K, V> root, int hash, Object key) {
        TreeNode<K, V> current = root;
        while (current != null) {
            if (hash < current.hash) {
                current = current.left;
                continue;
            }
            if (hash > current.hash) {
                current = current.right;
                continue;
            }

            if (Objects.equals(key, current.key)) {
                return current;
            }

            Integer cmp = comparableCompare(key, current.key);
            if (cmp != null) {
                current = (cmp < 0) ? current.left : current.right;
                continue;
            }

            TreeNode<K, V> foundLeft = find(current.left, hash, key);
            if (foundLeft != null) {
                return foundLeft;
            }
            current = current.right;
        }
        return null;
    }

    public static <K, V> InsertResult<K, V> insertOrUpdate(TreeNode<K, V> root, int hash, K key, V value) {
        TreeNode<K, V> newNode = new TreeNode<>(hash, key, value, null);

        TreeNode<K, V> current = root;
        TreeNode<K, V> parent = null;
        int lastCompare = 0;

        while (current != null) {
            parent = current;

            if (hash < current.hash) {
                lastCompare = -1;
                current = current.left;
                continue;
            }
            if (hash > current.hash) {
                lastCompare = 1;
                current = current.right;
                continue;
            }

            if (Objects.equals(key, current.key)) {
                V oldValue = current.value;
                current.value = value;
                return new InsertResult<>(true, oldValue, null, root);
            }

            Integer cmp = comparableCompare(key, current.key);
            if (cmp != null) {
                lastCompare = cmp;
            } else {
                int tie = tieBreakKeys(key, current.key);
                if (tie == 0) {
                    tie = Integer.compare(System.identityHashCode(newNode), System.identityHashCode(current));
                }
                lastCompare = tie;
            }

            current = (lastCompare < 0) ? current.left : current.right;
        }

        newNode.parent = parent;
        if (parent == null) {
            root = newNode;
        } else if (lastCompare < 0) {
            parent.left = newNode;
        } else {
            parent.right = newNode;
        }

        root = fixAfterInsertion(root, newNode);
        return new InsertResult<>(false, null, newNode, root);
    }

    public static <K, V> TreeNode<K, V> insertNewNode(TreeNode<K, V> root, TreeNode<K, V> newNode) {
        TreeNode<K, V> current = root;
        TreeNode<K, V> parent = null;
        int lastCompare = 0;

        while (current != null) {
            parent = current;

            if (newNode.hash < current.hash) {
                lastCompare = -1;
                current = current.left;
                continue;
            }
            if (newNode.hash > current.hash) {
                lastCompare = 1;
                current = current.right;
                continue;
            }

            Integer cmp = comparableCompare(newNode.key, current.key);
            if (cmp != null) {
                lastCompare = cmp;
            } else {
                int tie = tieBreakKeys(newNode.key, current.key);
                if (tie == 0) {
                    tie = Integer.compare(System.identityHashCode(newNode), System.identityHashCode(current));
                }
                lastCompare = tie;
            }

            current = (lastCompare < 0) ? current.left : current.right;
        }

        newNode.parent = parent;
        if (parent == null) {
            root = newNode;
        } else if (lastCompare < 0) {
            parent.left = newNode;
        } else {
            parent.right = newNode;
        }

        return fixAfterInsertion(root, newNode);
    }

    public static <K, V> TreeNode<K, V> delete(TreeNode<K, V> root, TreeNode<K, V> target) {
        TreeNode<K, V> node = target;

        if (node.left != null && node.right != null) {
            TreeNode<K, V> successor = successorOf(node);

            node.hash = successor.hash;
            node.key = successor.key;
            node.value = successor.value;

            node = successor;
        }

        TreeNode<K, V> replacement = (node.left != null) ? node.left : node.right;

        if (replacement != null) {
            replacement.parent = node.parent;

            if (node.parent == null) {
                root = replacement;
            } else if (node == node.parent.left) {
                node.parent.left = replacement;
            } else {
                node.parent.right = replacement;
            }

            node.left = null;
            node.right = null;
            node.parent = null;

            if (!node.red) {
                root = fixAfterDeletion(root, replacement);
            }

            if (root != null) {
                root.red = false;
            }
            return root;
        }

        if (node.parent == null) {
            return null;
        }

        if (!node.red) {
            root = fixAfterDeletion(root, node);
        }

        if (node.parent != null) {
            if (node == node.parent.left) {
                node.parent.left = null;
            } else if (node == node.parent.right) {
                node.parent.right = null;
            }
            node.parent = null;
        }

        if (root != null) {
            root.red = false;
        }
        return root;
    }

    private static Integer comparableCompare(Object a, Object b) {
        if (a == null || b == null) {
            return null;
        }
        if (a.getClass() != b.getClass()) {
            return null;
        }
        if (!(a instanceof Comparable<?>)) {
            return null;
        }
        return ((Comparable<Object>) a).compareTo(b);
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
        if (classCompare != 0) {
            return classCompare;
        }

        return Integer.compare(System.identityHashCode(a), System.identityHashCode(b));
    }

    private static <K, V> TreeNode<K, V> rotateLeft(TreeNode<K, V> root, TreeNode<K, V> pivot) {
        TreeNode<K, V> right = pivot.right;
        if (right == null) {
            return root;
        }

        pivot.right = right.left;
        if (right.left != null) {
            right.left.parent = pivot;
        }

        right.parent = pivot.parent;
        if (pivot.parent == null) {
            root = right;
        } else if (pivot == pivot.parent.left) {
            pivot.parent.left = right;
        } else {
            pivot.parent.right = right;
        }

        right.left = pivot;
        pivot.parent = right;
        return root;
    }

    private static <K, V> TreeNode<K, V> rotateRight(TreeNode<K, V> root, TreeNode<K, V> pivot) {
        TreeNode<K, V> left = pivot.left;
        if (left == null) {
            return root;
        }

        pivot.left = left.right;
        if (left.right != null) {
            left.right.parent = pivot;
        }

        left.parent = pivot.parent;
        if (pivot.parent == null) {
            root = left;
        } else if (pivot == pivot.parent.right) {
            pivot.parent.right = left;
        } else {
            pivot.parent.left = left;
        }

        left.right = pivot;
        pivot.parent = left;
        return root;
    }

    private static <K, V> TreeNode<K, V> fixAfterInsertion(TreeNode<K, V> root, TreeNode<K, V> node) {
        node.red = true;

        while (node != null && node != root && node.parent != null && node.parent.red) {
            TreeNode<K, V> parent = node.parent;
            TreeNode<K, V> grandparent = parent.parent;
            if (grandparent == null) {
                break;
            }

            if (parent == grandparent.left) {
                TreeNode<K, V> uncle = grandparent.right;

                if (uncle != null && uncle.red) {
                    parent.red = false;
                    uncle.red = false;
                    grandparent.red = true;
                    node = grandparent;
                } else {
                    if (node == parent.right) {
                        node = parent;
                        root = rotateLeft(root, node);
                        parent = node.parent;
                        grandparent = parent == null ? null : parent.parent;
                    }

                    if (parent != null) {
                        parent.red = false;
                    }
                    if (grandparent != null) {
                        grandparent.red = true;
                        root = rotateRight(root, grandparent);
                    }
                }
            } else {
                TreeNode<K, V> uncle = grandparent.left;

                if (uncle != null && uncle.red) {
                    parent.red = false;
                    uncle.red = false;
                    grandparent.red = true;
                    node = grandparent;
                } else {
                    if (node == parent.left) {
                        node = parent;
                        root = rotateRight(root, node);
                        parent = node.parent;
                        grandparent = parent == null ? null : parent.parent;
                    }

                    if (parent != null) {
                        parent.red = false;
                    }
                    if (grandparent != null) {
                        grandparent.red = true;
                        root = rotateLeft(root, grandparent);
                    }
                }
            }
        }

        root.red = false;
        return root;
    }

    private static <K, V> TreeNode<K, V> fixAfterDeletion(TreeNode<K, V> root, TreeNode<K, V> node) {
        TreeNode<K, V> current = node;

        while (current != root && (current == null || !current.red)) {
            TreeNode<K, V> parent = (current == null) ? null : current.parent;
            if (parent == null) {
                break;
            }

            if (current == parent.left) {
                TreeNode<K, V> sibling = parent.right;

                if (sibling != null && sibling.red) {
                    sibling.red = false;
                    parent.red = true;
                    root = rotateLeft(root, parent);
                    sibling = parent.right;
                }

                if (sibling == null) {
                    current = parent;
                    continue;
                }

                boolean leftBlack = sibling.left == null || !sibling.left.red;
                boolean rightBlack = sibling.right == null || !sibling.right.red;

                if (leftBlack && rightBlack) {
                    sibling.red = true;
                    current = parent;
                } else {
                    if (sibling.right == null || !sibling.right.red) {
                        if (sibling.left != null) {
                            sibling.left.red = false;
                        }
                        sibling.red = true;
                        root = rotateRight(root, sibling);
                        sibling = parent.right;
                    }

                    sibling.red = parent.red;
                    parent.red = false;
                    if (sibling.right != null) {
                        sibling.right.red = false;
                    }
                    root = rotateLeft(root, parent);
                    current = root;
                }
            } else {
                TreeNode<K, V> sibling = parent.left;

                if (sibling != null && sibling.red) {
                    sibling.red = false;
                    parent.red = true;
                    root = rotateRight(root, parent);
                    sibling = parent.left;
                }

                if (sibling == null) {
                    current = parent;
                    continue;
                }

                boolean leftBlack = sibling.left == null || !sibling.left.red;
                boolean rightBlack = sibling.right == null || !sibling.right.red;

                if (leftBlack && rightBlack) {
                    sibling.red = true;
                    current = parent;
                } else {
                    if (sibling.left == null || !sibling.left.red) {
                        if (sibling.right != null) {
                            sibling.right.red = false;
                        }
                        sibling.red = true;
                        root = rotateLeft(root, sibling);
                        sibling = parent.left;
                    }

                    sibling.red = parent.red;
                    parent.red = false;
                    if (sibling.left != null) {
                        sibling.left.red = false;
                    }
                    root = rotateRight(root, parent);
                    current = root;
                }
            }
        }

        if (current != null) {
            current.red = false;
        }
        return root;
    }
}
