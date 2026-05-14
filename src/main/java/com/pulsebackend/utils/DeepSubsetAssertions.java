package com.pulsebackend.utils;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.ZonedDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Currency;
import java.util.Date;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Assertions for deep partial object containment.
 *
 * <p>Only non-null values from expected are validated. Extra fields, map keys, or object state
 * on actual are ignored. Arrays and collections are compared by index and must have the same size.
 */
public final class DeepSubsetAssertions {

    private static final Set<Class<?>> SIMPLE_TYPES = Set.of(
            Byte.class, Short.class, Integer.class, Long.class,
            Float.class, Double.class, Boolean.class, Character.class,
            String.class, BigDecimal.class, BigInteger.class,
            Instant.class, LocalDate.class, LocalTime.class, LocalDateTime.class,
            OffsetTime.class, OffsetDateTime.class, ZonedDateTime.class,
            Duration.class, Period.class, Date.class,
            UUID.class, URI.class, URL.class, Locale.class, Currency.class
    );

    private DeepSubsetAssertions() {
    }

    /**
     * Deeply copies from actual only the fields that are non-null in expected.
     */
    @SuppressWarnings("unchecked")
    public static <A, E> E trimToSubset(A actual, E expected) {
        if (actual == null || expected == null) return null;
        try {
            return (E) deepCopyExpectedShape(actual, expected, new IdentityHashMap<>());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to deep trim object to expected subset", e);
        }
    }

    /**
     * Asserts that actual contains every non-null field from expected and that their values are equal.
     */
    public static <A, E> void assertSubsetEquals(A actual, E expected, String message) {
        assertContainsSubset(actual, expected, message);
    }

    /**
     * Asserts that actual contains every non-null field from expected and that their values are equal.
     */
    public static <A, E> void assertContainsSubset(A actual, E expected, String message) {
        try {
            assertContainsSubset(actual, expected, message, new HashSet<>(), "");
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Subset assertion failed", e);
        }
    }

    private static Object deepCopyExpectedShape(Object actual, Object expected, IdentityHashMap<Object, Object> visited)
            throws ReflectiveOperationException {
        if (actual == null || expected == null) return null;
        if (isSimple(expected.getClass())) return actual;
        if (visited.containsKey(actual)) return visited.get(actual);

        if (expected.getClass().isArray()) {
            int expectedLength = Array.getLength(expected);
            Object arrayCopy = Array.newInstance(expected.getClass().getComponentType(), expectedLength);
            visited.put(actual, arrayCopy);

            if (!actual.getClass().isArray()) return arrayCopy;

            int actualLength = Array.getLength(actual);
            int copiedLength = Math.min(actualLength, expectedLength);
            for (int i = 0; i < copiedLength; i++) {
                Object expectedElement = Array.get(expected, i);
                if (expectedElement == null) continue;
                Array.set(arrayCopy, i, deepCopyExpectedShape(Array.get(actual, i), expectedElement, visited));
            }
            return arrayCopy;
        }

        if (expected instanceof Collection<?> expectedCollection) {
            Collection<Object> copy = instantiateCollection(expectedCollection);
            visited.put(actual, copy);

            if (!(actual instanceof Collection<?> actualCollection)) return copy;

            Iterator<?> actualIterator = actualCollection.iterator();
            Iterator<?> expectedIterator = expectedCollection.iterator();
            while (actualIterator.hasNext() && expectedIterator.hasNext()) {
                Object actualElement = actualIterator.next();
                Object expectedElement = expectedIterator.next();
                copy.add(expectedElement == null ? null : deepCopyExpectedShape(actualElement, expectedElement, visited));
            }
            return copy;
        }

        if (expected instanceof Map<?, ?> expectedMap) {
            Map<Object, Object> copy = instantiateMap(expectedMap);
            visited.put(actual, copy);

            if (!(actual instanceof Map<?, ?> actualMap)) return copy;

            for (Map.Entry<?, ?> entry : expectedMap.entrySet()) {
                Object expectedValue = entry.getValue();
                if (expectedValue == null) continue;
                Object key = entry.getKey();
                copy.put(key, deepCopyExpectedShape(actualMap.get(key), expectedValue, visited));
            }
            return copy;
        }

        Object target = expected.getClass().getDeclaredConstructor().newInstance();
        visited.put(actual, target);

        Map<String, Field> actualFields = declaredFieldsMap(actual.getClass());
        for (Field expectedField : declaredFieldsMap(expected.getClass()).values()) {
            if (shouldSkipField(expectedField)) continue;

            expectedField.setAccessible(true);
            Object expectedValue = expectedField.get(expected);
            if (expectedValue == null) continue;

            Field actualField = actualFields.get(expectedField.getName());
            if (actualField == null) continue;

            actualField.setAccessible(true);
            expectedField.setAccessible(true);
            Object actualValue = actualField.get(actual);
            Object copiedValue = deepCopyExpectedShape(actualValue, expectedValue, visited);
            expectedField.set(target, copiedValue);
        }
        return target;
    }

    private static void assertContainsSubset(Object actual, Object expected, String baseMessage,
                                             Set<SeenPair> visited, String path)
            throws ReflectiveOperationException {
        if (expected == null) return;

        String description = describe(baseMessage, path);

        if (isSimple(expected.getClass())) {
            assertThat(actual).as(description).isEqualTo(expected);
            return;
        }

        assertThat(actual).as(description + " actual is null while expected is not null").isNotNull();

        SeenPair pair = new SeenPair(actual, expected);
        if (!visited.add(pair)) return;

        if (expected.getClass().isArray()) {
            assertThat(actual.getClass().isArray()).as(description + " actual is not an array").isTrue();
            int expectedLength = Array.getLength(expected);
            int actualLength = Array.getLength(actual);
            assertThat(actualLength).as(description + " array length mismatch").isEqualTo(expectedLength);
            for (int i = 0; i < expectedLength; i++) {
                Object expectedElement = Array.get(expected, i);
                if (expectedElement == null) continue;
                assertContainsSubset(Array.get(actual, i), expectedElement, baseMessage, visited, path + "[" + i + "]");
            }
            return;
        }

        if (expected instanceof Collection<?> expectedCollection) {
            assertThat(actual instanceof Collection<?>).as(description + " actual is not a collection").isTrue();
            Collection<?> actualCollection = (Collection<?>) actual;
            assertThat(actualCollection.size()).as(description + " collection size mismatch").isEqualTo(expectedCollection.size());

            Iterator<?> actualIterator = actualCollection.iterator();
            Iterator<?> expectedIterator = expectedCollection.iterator();
            int index = 0;
            while (expectedIterator.hasNext()) {
                Object expectedElement = expectedIterator.next();
                Object actualElement = actualIterator.next();
                if (expectedElement != null) {
                    assertContainsSubset(actualElement, expectedElement, baseMessage, visited, path + "[" + index + "]");
                }
                index++;
            }
            return;
        }

        if (expected instanceof Map<?, ?> expectedMap) {
            assertThat(actual instanceof Map<?, ?>).as(description + " actual is not a map").isTrue();
            Map<?, ?> actualMap = (Map<?, ?>) actual;
            for (Map.Entry<?, ?> entry : expectedMap.entrySet()) {
                Object expectedValue = entry.getValue();
                if (expectedValue == null) continue;
                Object key = entry.getKey();
                assertThat(actualMap.containsKey(key)).as(description + " missing key: " + key).isTrue();
                assertContainsSubset(actualMap.get(key), expectedValue, baseMessage, visited, appendMapPath(path, key));
            }
            return;
        }

        Map<String, Field> actualFields = declaredFieldsMap(actual.getClass());
        for (Field expectedField : declaredFieldsMap(expected.getClass()).values()) {
            if (shouldSkipField(expectedField)) continue;

            expectedField.setAccessible(true);
            Object expectedValue = expectedField.get(expected);
            if (expectedValue == null) continue;

            Field actualField = actualFields.get(expectedField.getName());
            assertThat(actualField)
                    .as(description + " missing field: " + expectedField.getName())
                    .isNotNull();

            actualField.setAccessible(true);
            assertContainsSubset(actualField.get(actual), expectedValue, baseMessage, visited,
                    appendFieldPath(path, expectedField.getName()));
        }
    }

    private static boolean isSimple(Class<?> cls) {
        return cls.isPrimitive()
                || SIMPLE_TYPES.contains(cls)
                || Enum.class.isAssignableFrom(cls)
                || Optional.class.isAssignableFrom(cls);
    }

    private static Map<String, Field> declaredFieldsMap(Class<?> cls) {
        Map<String, Field> fields = new LinkedHashMap<>();
        Class<?> current = cls;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                fields.putIfAbsent(field.getName(), field);
            }
            current = current.getSuperclass();
        }
        return fields;
    }

    private static boolean shouldSkipField(Field field) {
        int modifiers = field.getModifiers();
        return Modifier.isStatic(modifiers) || field.isSynthetic();
    }

    @SuppressWarnings("unchecked")
    private static Collection<Object> instantiateCollection(Collection<?> source) {
        if (source instanceof List) return new ArrayList<>();
        if (source instanceof Set) return new LinkedHashSet<>();
        if (source instanceof Queue) return new ArrayDeque<>();
        try {
            return (Collection<Object>) source.getClass().getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            return new ArrayList<>();
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<Object, Object> instantiateMap(Map<?, ?> source) {
        if (source instanceof LinkedHashMap) return new LinkedHashMap<>();
        if (source instanceof SortedMap) return new TreeMap<>();
        try {
            return (Map<Object, Object>) source.getClass().getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            return new LinkedHashMap<>();
        }
    }

    private static String describe(String baseMessage, String path) {
        String message = baseMessage == null || baseMessage.isBlank() ? "Expected subset" : baseMessage;
        return path.isEmpty() ? message : message + " @ " + path;
    }

    private static String appendFieldPath(String path, String field) {
        return path.isEmpty() ? field : path + "." + field;
    }

    private static String appendMapPath(String path, Object key) {
        String keyPath = "['" + key + "']";
        return path.isEmpty() ? keyPath : path + keyPath;
    }

    private static final class SeenPair {
        private final Object actual;
        private final Object expected;

        private SeenPair(Object actual, Object expected) {
            this.actual = actual;
            this.expected = expected;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof SeenPair that)) return false;
            return actual == that.actual && expected == that.expected;
        }

        @Override
        public int hashCode() {
            return 31 * System.identityHashCode(actual) + System.identityHashCode(expected);
        }
    }
}
