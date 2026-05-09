package com.pulsebackend.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pulsebackend.utils.Wait;
import io.qameta.allure.Allure;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.ResultQuery;
import org.jooq.conf.ParamType;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.jooq.impl.DSL.and;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;
import static org.jooq.impl.DSL.trueCondition;

public class DbController<T> {
    private final String tableName;
    private final Class<T> type;
    private final DSLContext dsl;
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.INDENT_OUTPUT);

    public DbController(String tableName, Class<T> type, DSLContext dsl) {
        this.tableName = tableName;
        this.type = type;
        this.dsl = dsl;
    }

    public T waitForOneRecord(Map<String, ?> where) {
        return waitForOneRecord(where, 10_000, 500);
    }

    public T waitForOneRecord(Map<String, ?> where, long timeout, long poll) {
        return Allure.step("Wait for one record in table " + tableName, () -> {
            ResultQuery<?> query = select(where);
            Allure.addAttachment("SQL", query.getSQL(ParamType.INLINED));
            T record = new Wait(timeout, poll).waitingFor(() -> query.fetchOneInto(type));
            if (record == null) {
                throw new AssertionError("No records were found in table " + tableName + " with condition " + where);
            }
            Allure.addAttachment("Record", "application/json", toJsonSafe(record), ".json");
            return record;
        });
    }

    public List<T> getRecords(Map<String, ?> where) {
        return Allure.step("Get records from table " + tableName, () -> {
            ResultQuery<?> query = select(where);
            Allure.addAttachment("SQL", query.getSQL(ParamType.INLINED));
            List<T> records = query.fetchInto(type);
            Allure.addAttachment(
                    "Records",
                    "application/json",
                    records.stream().map(this::toJsonSafe).collect(Collectors.joining("\n")),
                    ".json"
            );
            return records;
        });
    }

    private ResultQuery<?> select(Map<String, ?> where) {
        return dsl.select()
                .from(table(name(nameParts(tableName))))
                .where(toCondition(where));
    }

    public String getTableName() {
        return tableName;
    }

    public Class<T> getType() {
        return type;
    }

    private Condition toCondition(Map<String, ?> where) {
        if (where == null || where.isEmpty()) {
            return trueCondition();
        }

        return and(where.entrySet()
                .stream()
                .map(entry -> field(name(nameParts(entry.getKey()))).eq(entry.getValue()))
                .toArray(Condition[]::new));
    }

    private String[] nameParts(String value) {
        return Arrays.stream(value.split("\\."))
                .filter(part -> !part.isBlank())
                .toArray(String[]::new);
    }

    private String toJsonSafe(Object value) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (Exception exception) {
            return String.valueOf(value);
        }
    }
}
