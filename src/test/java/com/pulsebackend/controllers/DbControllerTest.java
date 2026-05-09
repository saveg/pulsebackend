package com.pulsebackend.controllers;

import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;

public class DbControllerTest {
    private Connection connection;
    private DSLContext dsl;
    private DbController<TestRecord> controller;

    @BeforeMethod
    public void setUpDatabase() throws Exception {
        connection = DriverManager.getConnection(
                "jdbc:h2:mem:db_controller_test;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        dsl = DSL.using(connection);
        dsl.execute("drop table if exists test_records");
        dsl.execute("create table test_records (id int primary key, status varchar(20), amount varchar(20))");
        dsl.execute("insert into test_records (id, status, amount) values (1, 'created', '10.00')");
        dsl.execute("insert into test_records (id, status, amount) values (2, 'updated', '20.50')");
        controller = new DbController<>("test_records", TestRecord.class, dsl);
    }

    @AfterMethod
    public void closeDatabase() throws Exception {
        if (connection != null) {
            connection.close();
        }
    }

    @Test
    public void getRecordsShouldReturnMatchingRows() {
        List<TestRecord> records = controller.getRecords(Map.of("status", "created"));

        assertEquals(records.size(), 1);
        assertEquals(records.get(0).id, 1);
        assertEquals(records.get(0).amount, "10.00");
    }

    @Test
    public void getRecordsShouldReturnAllRowsForEmptyCondition() {
        List<TestRecord> records = controller.getRecords(Map.of());

        assertEquals(records.size(), 2);
    }

    @Test
    public void waitForOneRecordShouldReturnMatchingRow() {
        TestRecord record = controller.waitForOneRecord(Map.of("id", 2), 500, 10);

        assertEquals(record.status, "updated");
        assertEquals(record.amount, "20.50");
    }

    public static class TestRecord {
        public Integer id;
        public String status;
        public String amount;
    }
}
