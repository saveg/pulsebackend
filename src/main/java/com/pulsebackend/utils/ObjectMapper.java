package com.pulsebackend.utils;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Deep subset mapper & comparator:
 * - Compares only non-null fields of the expectedSubset (subset semantics).
 * - Supports: primitives, wrappers, String, Enum, Date/Time, arrays, Collection, Map, nested POJOs.
 * - Arrays & collections are compared by index (ordered).
 * - Maps: only keys present in the expected map are validated.
 */
public class ObjectMapper {

    private static final Set<Class<?>> SIMPLE_TYPES = Set.of(
            Byte.class, Short.class, Integer.class, Long.class,
            Float.class, Double.class, Boolean.class, Character.class,
            String.class, java.math.BigDecimal.class, java.math.BigInteger.class,
            java.time.Instant.class, java.time.LocalDate.class, java.time.LocalDateTime.class,
            java.time.OffsetDateTime.class, java.time.ZonedDateTime.class, Date.class
    );

    // ------------------------- PUBLIC API -------------------------

    /**
     * Deeply copies the intersection of fields from fullPojo into a new instance
     * of the class of subsetPojo. Nested structures are copied recursively.
     */
    @SuppressWarnings("unchecked")
    public static <F, P> P trimToSubset(F fullPojo, P subsetPojo) {
        if (fullPojo == null || subsetPojo == null) return null;
        try {
            return (P) deepCopyIntersect(fullPojo, subsetPojo.getClass(), new IdentityHashMap<>());
        } catch (Exception e) {
            throw new RuntimeException("Failed to deep trim POJO to subset", e);
        }
    }

    /**
     * Deep subset comparison: if it fails on a field we catch the AssertionError and perform a fallback —
     * a full comparison of the trimmed actual with the expected (provides a complete diff).
     */
    public static <F, P> void assertSubsetEquals(F fullPojo, P expectedSubset, String message) {
        try {
            deepSubsetAssert(fullPojo, expectedSubset, message, new IdentityHashMap<>(), "");
        } catch (AssertionError fieldError) {
            // Fallback: show the full objects (in their subset projection) plus the original message
            P trimmed = trimToSubset(fullPojo, expectedSubset);
            assertThat(trimmed).as(message).isEqualTo(expectedSubset);
        } catch (Exception e) {
            throw new RuntimeException("Subset assertion failed", e);
        }
    }

    // ------------------------- INTERNAL: COPY -------------------------

    private static Object deepCopyIntersect(Object source, Class<?> targetClass, IdentityHashMap<Object, Object> visited)
            throws Exception {
        if (source == null) return null;
        if (isSimple(source.getClass())) return source;
        if (visited.containsKey(source)) return visited.get(source);

        if (source.getClass().isArray()) {
            int len = Array.getLength(source);
            Object arrCopy = Array.newInstance(source.getClass().getComponentType(), len);
            visited.put(source, arrCopy);
            for (int i = 0; i < len; i++) {
                Object el = Array.get(source, i);
                Array.set(arrCopy, i, deepCopyIntersect(el, el != null ? el.getClass() : source.getClass().getComponentType(), visited));
            }
            return arrCopy;
        }

        if (source instanceof Collection<?> col) {
            Collection<Object> copy = instantiateCollection(col);
            visited.put(source, copy);
            for (Object el : col) {
                copy.add(deepCopyIntersect(el, el != null ? el.getClass() : Object.class, visited));
            }
            return copy;
        }

        if (source instanceof Map<?, ?> map) {
            Map<Object, Object> copy = instantiateMap(map);
            visited.put(source, copy);
            for (Map.Entry<?, ?> e : map.entrySet()) {
                Object k = e.getKey();
                Object v = e.getValue();
                copy.put(k, deepCopyIntersect(v, v != null ? v.getClass() : Object.class, visited));
            }
            return copy;
        }

        // Generic POJO: copy only matching field names
        Object target = targetClass.getDeclaredConstructor().newInstance();
        visited.put(source, target);
        Map<String, Field> srcFields = declaredFieldsMap(source.getClass());
        Map<String, Field> targetFields = declaredFieldsMap(targetClass);

        for (Field tf : targetFields.values()) {
            if (Modifier.isStatic(tf.getModifiers())) continue;

            Field sf = srcFields.get(tf.getName());
            if (sf == null) continue;

            sf.setAccessible(true);
            tf.setAccessible(true);

            Object val = sf.get(source);
            if (val == null) continue;

            Object deep = deepCopyIntersect(val, val.getClass(), visited);
            tf.set(target, deep);
        }
        return target;
    }

    // ------------------------- INTERNAL: ASSERT -------------------------

    private static void deepSubsetAssert(Object actual, Object expected, String baseMsg,
                                         IdentityHashMap<SeenPair, Boolean> visited, String path) throws Exception {
        if (expected == null) return; // nothing to assert
        String msgPrefix = baseMsg + (path.isEmpty() ? "" : " @ " + path);

        // Simple types direct comparison
        if (isSimple(expected.getClass())) {
            assertThat(actual).as(msgPrefix).isEqualTo(expected);
            return;
        }

        // Cycle detection
        SeenPair pair = new SeenPair(actual, expected);
        if (visited.containsKey(pair)) return;
        visited.put(pair, Boolean.TRUE);

        // If expected not null -> actual must not be null
        assertThat(actual).as(msgPrefix + " actual is null while expected not null").isNotNull();

        if (expected.getClass().isArray()) {
            assertThat(actual.getClass().isArray()).as(msgPrefix + " actual not array").isTrue();
            int expLen = Array.getLength(expected);
            int actLen = Array.getLength(actual);
            assertThat(actLen).as(msgPrefix + " length mismatch").isEqualTo(expLen);
            for (int i = 0; i < expLen; i++) {
                Object ev = Array.get(expected, i);
                if (ev == null) continue;
                Object av = Array.get(actual, i);
                deepSubsetAssert(av, ev, baseMsg, visited, path + "[" + i + "]");
            }
            return;
        }

        if (expected instanceof Collection<?> expCol) {
            assertThat(actual instanceof Collection<?>).as(msgPrefix + " actual not collection").isTrue();
            Collection<?> actCol = (Collection<?>) actual;
            assertThat(actCol.size()).as(msgPrefix + " collection size mismatch").isEqualTo(expCol.size());
            Iterator<?> itE = expCol.iterator();
            Iterator<?> itA = actCol.iterator();
            int idx = 0;
            while (itE.hasNext()) {
                Object ev = itE.next();
                Object av = itA.next();
                if (ev != null)
                    deepSubsetAssert(av, ev, baseMsg, visited, path + "[" + idx + "]");
                idx++;
            }
            return;
        }

        if (expected instanceof Map<?, ?> expMap) {
            assertThat(actual instanceof Map<?, ?>).as(msgPrefix + " actual not map").isTrue();
            Map<?, ?> actMap = (Map<?, ?>) actual;
            for (Map.Entry<?, ?> e : expMap.entrySet()) {
                Object key = e.getKey();
                Object ev = e.getValue();
                if (ev == null) continue;
                assertThat(actMap.containsKey(key)).as(msgPrefix + " missing key: " + key).isTrue();
                deepSubsetAssert(actMap.get(key), ev, baseMsg, visited,
                        path + (path.isEmpty() ? "" : ".") + "['" + key + "']");
            }
            return;
        }

        // Nested POJO: only non-null expected fields are asserted
        Map<String, Field> expectedFields = declaredFieldsMap(expected.getClass());
        Map<String, Field> actualFields = declaredFieldsMap(actual.getClass());

        for (Field ef : expectedFields.values()) {
            if (Modifier.isStatic(ef.getModifiers())) continue;
            ef.setAccessible(true);
            Object ev = ef.get(expected);
            if (ev == null) continue; // subset semantics
            Field af = actualFields.get(ef.getName());
            assertThat(af).as(msgPrefix + " missing field: " + ef.getName()).isNotNull();
            af.setAccessible(true);
            Object av = af.get(actual);
            deepSubsetAssert(av, ev, baseMsg, visited,
                    path + (path.isEmpty() ? "" : ".") + ef.getName());
        }
    }

    // ------------------------- HELPERS -------------------------

    private static boolean isSimple(Class<?> cls) {
        return cls.isPrimitive() || SIMPLE_TYPES.contains(cls) || Enum.class.isAssignableFrom(cls);
    }

    private static Map<String, Field> declaredFieldsMap(Class<?> cls) {
        Map<String, Field> map = new HashMap<>();
        Class<?> cur = cls;
        while (cur != null && cur != Object.class) {
            for (Field f : cur.getDeclaredFields()) {
                map.putIfAbsent(f.getName(), f);
            }
            cur = cur.getSuperclass();
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    private static Collection<Object> instantiateCollection(Collection<?> src) {
        if (src instanceof List) return new ArrayList<>();
        if (src instanceof Set) return new LinkedHashSet<>();
        if (src instanceof Queue) return new ArrayDeque<>();
        try {
            return (Collection<Object>) src.getClass().getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<Object, Object> instantiateMap(Map<?, ?> src) {
        if (src instanceof LinkedHashMap) return new LinkedHashMap<>();
        if (src instanceof SortedMap) return new TreeMap<>();
        try {
            return (Map<Object, Object>) src.getClass().getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    private record SeenPair(Object a, Object b) {}
}
