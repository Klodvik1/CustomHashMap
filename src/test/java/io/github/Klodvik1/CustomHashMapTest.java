package io.github.Klodvik1;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class CustomHashMapTest {

    private static final int COLLISION_HASH_POSITIVE = 1;
    private static final int COLLISION_HASH_NEGATIVE = -1;

    @Test
    public void constructor_invalidArguments_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> new CustomHashMap<>(-1));
        assertThrows(IllegalArgumentException.class, () -> new CustomHashMap<>(16, 0.0f));
        assertThrows(IllegalArgumentException.class, () -> new CustomHashMap<>(16, -1.0f));
        assertThrows(IllegalArgumentException.class, () -> new CustomHashMap<>(16, Float.NaN));
    }

    @Test
    public void nullKey_andNullValue_contract() {
        Map<String, Integer> map = new CustomHashMap<>();

        assertNull(map.put(null, 10));
        assertEquals(Integer.valueOf(10), map.get(null));
        assertTrue(map.containsKey(null));

        assertNull(map.put("x", null));
        assertTrue(map.containsKey("x"));
        assertNull(map.get("x"));

        assertEquals(Integer.valueOf(10), map.remove(null));
        assertFalse(map.containsKey(null));

        assertNull(map.remove("missing"));
        assertNull(map.get("missing"));
    }

    @Test
    public void put_shouldReturnOldValue_andNotChangeSize_onUpdate() {
        Map<String, Integer> map = new CustomHashMap<>();

        assertNull(map.put("a", 1));
        assertEquals(1, map.size());

        assertEquals(Integer.valueOf(1), map.put("a", 2));
        assertEquals(1, map.size());
        assertEquals(Integer.valueOf(2), map.get("a"));

        assertNull(map.put("b", 3));
        assertEquals(2, map.size());
    }

    @Test
    public void clear_shouldRemoveAllEntries_andResetSize() {
        Map<Integer, Integer> map = new CustomHashMap<>();
        for (int i = 0; i < 1000; i++) {
            map.put(i, i * 10);
        }

        assertFalse(map.isEmpty());
        assertEquals(1000, map.size());

        map.clear();

        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        assertTrue(map.entrySet().isEmpty());
        assertTrue(map.keySet().isEmpty());
        assertTrue(map.values().isEmpty());
        assertNull(map.get(1));
        assertFalse(map.containsKey(1));
    }

    @Test
    public void putAll_shouldBehaveLikeHashMap() {
        Map<Integer, Integer> src = new HashMap<>();
        for (int i = 0; i < 1000; i++) {
            src.put(i, i + 1);
        }

        Map<Integer, Integer> map = new CustomHashMap<>();
        map.putAll(src);

        assertEquals(src, map);
        assertEquals(src.hashCode(), map.hashCode());
    }

    @Test
    public void entrySet_shouldSupportContainsRemove_andSetValueShouldReflectInMap() {
        CustomHashMap<String, Integer> map = new CustomHashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        Set<Map.Entry<String, Integer>> entries = map.entrySet();

        assertTrue(entries.contains(new AbstractMap.SimpleEntry<>("a", 1)));
        assertFalse(entries.contains(new AbstractMap.SimpleEntry<>("a", 999)));

        assertTrue(entries.remove(new AbstractMap.SimpleEntry<>("a", 1)));
        assertFalse(map.containsKey("a"));
        assertNull(map.get("a"));
        assertEquals(1, map.size());

        Map.Entry<String, Integer> bEntry = entries.iterator().next();
        assertEquals("b", bEntry.getKey());
        assertEquals(Integer.valueOf(2), bEntry.getValue());

        Integer old = bEntry.setValue(20);
        assertEquals(Integer.valueOf(2), old);
        assertEquals(Integer.valueOf(20), map.get("b"));
    }

    @Test
    public void keySet_and_values_shouldBeLiveViews() {
        Map<String, Integer> map = new CustomHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 2);

        Set<String> keys = map.keySet();
        Collection<Integer> values = map.values();

        assertTrue(keys.contains("a"));
        assertEquals(3, keys.size());
        assertEquals(3, values.size());

        assertTrue(keys.remove("b"));
        assertFalse(map.containsKey("b"));
        assertEquals(2, map.size());
        assertEquals(2, keys.size());
        assertEquals(2, values.size());

        assertTrue(values.remove(Integer.valueOf(2)));
        assertEquals(1, map.size());
        assertTrue(map.containsKey("a"));
        assertFalse(map.containsKey("c"));

        assertEquals(Collections.singleton("a"), map.keySet());
        assertEquals(Collections.singletonList(1), new ArrayList<>(map.values()));
    }

    @Test
    public void iterator_contract_nextOnEmpty_andRemoveRules() {
        Map<String, Integer> map = new CustomHashMap<>();
        Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();

        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::next);
        assertThrows(IllegalStateException.class, it::remove);

        map.put("x", 10);
        Iterator<Map.Entry<String, Integer>> it2 = map.entrySet().iterator();

        assertTrue(it2.hasNext());
        Map.Entry<String, Integer> e = it2.next();
        assertEquals("x", e.getKey());

        it2.remove();
        assertTrue(map.isEmpty());
        assertThrows(IllegalStateException.class, it2::remove);
    }

    @Test
    public void iterator_shouldBeFailFast_onStructuralModification() {
        Map<String, Integer> map = new CustomHashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();
        map.put("c", 3);

        assertThrows(ConcurrentModificationException.class, it::next);
    }

    @Test
    public void heavyCollisions_comparableKeys_shouldBehaveCorrectly() {
        Map<CollidingComparableKey, Integer> map = new CustomHashMap<>(64);
        Map<CollidingComparableKey, Integer> ref = new HashMap<>();

        for (int i = 0; i < 200; i++) {
            CollidingComparableKey k = new CollidingComparableKey(i, COLLISION_HASH_POSITIVE);
            assertEquals(ref.put(k, i * 10), map.put(k, i * 10));
        }
        assertEquals(ref, map);

        for (int i = 0; i < 200; i += 3) {
            CollidingComparableKey k = new CollidingComparableKey(i, COLLISION_HASH_POSITIVE);
            assertEquals(ref.put(k, i * 100), map.put(k, i * 100));
        }
        assertEquals(ref, map);

        for (int i = 0; i < 200; i += 2) {
            CollidingComparableKey k = new CollidingComparableKey(i, COLLISION_HASH_POSITIVE);
            assertEquals(ref.remove(k), map.remove(k));
        }
        assertEquals(ref, map);

        for (int i = 0; i < 200; i++) {
            CollidingComparableKey k = new CollidingComparableKey(i, COLLISION_HASH_POSITIVE);
            assertEquals(ref.get(k), map.get(k));
            assertEquals(ref.containsKey(k), map.containsKey(k));
        }
    }

    @Test
    public void heavyCollisions_nonComparableKeys_shouldBehaveCorrectly_evenWithNegativeHash() {
        Map<CollidingKey, Integer> map = new CustomHashMap<>(64);
        Map<CollidingKey, Integer> ref = new HashMap<>();

        for (int i = 0; i < 250; i++) {
            int hash = (i % 2 == 0) ? COLLISION_HASH_NEGATIVE : COLLISION_HASH_POSITIVE;
            CollidingKey k = new CollidingKey(i, hash);
            Integer v = (i % 10 == 0) ? null : (i + 7);
            assertEquals(ref.put(k, v), map.put(k, v));
        }
        assertEquals(ref, map);

        for (int i = 0; i < 250; i += 5) {
            int hash = (i % 2 == 0) ? COLLISION_HASH_NEGATIVE : COLLISION_HASH_POSITIVE;
            CollidingKey k = new CollidingKey(i, hash);
            assertEquals(ref.remove(k), map.remove(k));
        }
        assertEquals(ref, map);
    }

    @Test
    public void bulkOperations_shouldSurviveMultipleResizes_andMatchHashMap() {
        Map<Integer, Integer> map = new CustomHashMap<>();
        Map<Integer, Integer> ref = new HashMap<>();

        for (int i = 0; i < 50000; i++) {
            Integer key = i;
            Integer value = i * 2;
            assertEquals(ref.put(key, value), map.put(key, value));
        }

        for (int i = 0; i < 50000; i += 2) {
            assertEquals(ref.remove(i), map.remove(i));
        }

        for (int i = 0; i < 50000; i++) {
            assertEquals(ref.get(i), map.get(i));
        }

        assertMapViewsEqual(ref, map);
    }

    @Test
    public void randomized_compareWithHashMap_shouldMatch_forIntegers_andNulls() {
        Random rnd = new Random(7);

        Map<Integer, Integer> ref = new HashMap<>();
        Map<Integer, Integer> test = new CustomHashMap<>();

        for (int i = 0; i < 120000; i++) {
            int op = rnd.nextInt(8);

            Integer key = rnd.nextInt(2000);
            if (rnd.nextInt(250) == 0) key = null;

            Integer value = rnd.nextInt(50000);
            if (rnd.nextInt(250) == 0) value = null;

            switch (op) {
                case 0 -> assertEquals(ref.put(key, value), test.put(key, value));
                case 1 -> assertEquals(ref.get(key), test.get(key));
                case 2 -> assertEquals(ref.remove(key), test.remove(key));
                case 3 -> assertEquals(ref.containsKey(key), test.containsKey(key));
                case 4 -> assertEquals(ref.containsValue(value), test.containsValue(value));
                case 5 -> {
                    assertEquals(ref.size(), test.size());
                    assertEquals(ref.isEmpty(), test.isEmpty());
                }
                case 6 -> {
                    if (!ref.isEmpty()) {
                        Integer anyKey = ref.keySet().iterator().next();
                        assertEquals(ref.get(anyKey), test.get(anyKey));
                    }
                }
                case 7 -> {
                    if (rnd.nextInt(3000) == 0) {
                        ref.clear();
                        test.clear();
                    }
                }
            }

            if (i % 10000 == 0) {
                assertEquals(ref, test);
            }
        }

        assertMapViewsEqual(ref, test);
    }

    private record CollidingComparableKey(int id, int hash) implements Comparable<CollidingComparableKey> {

        @Override
            public int hashCode() {
                return hash;
            }

            @Override
            public boolean equals(Object obj) {
                if (this == obj) return true;
                if (!(obj instanceof CollidingComparableKey other)) return false;
                return this.id == other.id;
            }

            @Override
            public int compareTo(CollidingComparableKey other) {
                return Integer.compare(this.id, other.id);
            }

            @Override
            public String toString() {
                return "CollidingComparableKey{id=" + id + ", hash=" + hash + "}";
            }
        }

    private record CollidingKey(int id, int hash) {

        @Override
            public int hashCode() {
                return hash;
            }

            @Override
            public boolean equals(Object obj) {
                if (this == obj) return true;
                if (!(obj instanceof CollidingKey other)) return false;
                return this.id == other.id;
            }

            @Override
            public String toString() {
                return "CollidingKey{id=" + id + ", hash=" + hash + "}";
            }
        }

    private static <K, V> void assertMapViewsEqual(Map<K, V> expected, Map<K, V> actual) {
        assertEquals(expected, actual);
        assertEquals(expected.hashCode(), actual.hashCode());

        assertEquals(expected.keySet(), actual.keySet());

        assertEquals(multiset(expected.values()), multiset(actual.values()));

        assertEquals(multisetEntries(expected.entrySet()), multisetEntries(actual.entrySet()));
    }

    private static <T> Map<T, Integer> multiset(Collection<T> items) {
        Map<T, Integer> counts = new HashMap<>();
        for (T item : items) {
            counts.merge(item, 1, Integer::sum);
        }
        return counts;
    }

    private static <K, V> Map<Map.Entry<K, V>, Integer> multisetEntries(Set<Map.Entry<K, V>> items) {
        Map<Map.Entry<K, V>, Integer> counts = new HashMap<>();
        for (Map.Entry<K, V> item : items) {
            counts.merge(new AbstractMap.SimpleEntry<>(item.getKey(), item.getValue()), 1, Integer::sum);
        }
        return counts;
    }
}