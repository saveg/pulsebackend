package com.sandboxservice;

import com.pulsebackend.utils.DeepSubsetAssertions;
import org.testng.annotations.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class DeepSubsetAssertionsTest {

    @Test
    public void assertContainsSubsetAllowsActualToHaveExtraFields() {
        ActualOrder actual = new ActualOrder();
        actual.id = "order-1";
        actual.status = "DONE";
        actual.quantity = 10;
        actual.details = new ActualDetails();
        actual.details.symbol = "AAPL";
        actual.details.extra = "ignored";

        ExpectedOrder expected = new ExpectedOrder();
        expected.id = "order-1";
        expected.details = new ExpectedDetails();
        expected.details.symbol = "AAPL";

        DeepSubsetAssertions.assertContainsSubset(actual, expected, "order should contain expected subset");
    }

    @Test
    public void assertContainsSubsetIgnoresNullExpectedFields() {
        ActualOrder actual = new ActualOrder();
        actual.id = "order-1";
        actual.status = "DONE";

        ExpectedOrder expected = new ExpectedOrder();
        expected.id = "order-1";
        expected.status = null;

        DeepSubsetAssertions.assertContainsSubset(actual, expected, "null expected status is not asserted");
    }

    @Test
    public void assertContainsSubsetChecksOnlyExpectedMapKeys() {
        Map<String, Object> actual = new LinkedHashMap<>();
        actual.put("id", "order-1");
        actual.put("status", "DONE");

        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("id", "order-1");

        DeepSubsetAssertions.assertContainsSubset(actual, expected, "map should contain expected entries");
    }

    @Test
    public void assertContainsSubsetReportsPathOnValueMismatch() {
        ActualOrder actual = new ActualOrder();
        actual.id = "order-1";

        ExpectedOrder expected = new ExpectedOrder();
        expected.id = "order-2";

        assertThatThrownBy(() -> DeepSubsetAssertions.assertContainsSubset(actual, expected, "order subset"))
                .hasMessageContaining("order subset @ id")
                .hasMessageContaining("order-2");
    }

    @Test
    public void assertContainsSubsetHandlesCycles() {
        Node actual = new Node();
        actual.name = "root";
        actual.next = actual;

        Node expected = new Node();
        expected.name = "root";
        expected.next = expected;

        DeepSubsetAssertions.assertContainsSubset(actual, expected, "cyclic node should match");
    }

    private static class ActualOrder {
        private String id;
        private String status;
        private Integer quantity;
        private ActualDetails details;
    }

    private static class ExpectedOrder {
        private String id;
        private String status;
        private ExpectedDetails details;
    }

    private static class ActualDetails {
        private String symbol;
        private String extra;
    }

    private static class ExpectedDetails {
        private String symbol;
    }

    private static class Node {
        private String name;
        private Node next;
    }
}
