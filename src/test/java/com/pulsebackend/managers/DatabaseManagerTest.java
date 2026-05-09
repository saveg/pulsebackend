package com.pulsebackend.managers;

import org.jooq.DSLContext;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertThrows;

public class DatabaseManagerTest {
    @AfterMethod
    public void closeDatabaseConnections() {
        DatabaseManager.getInstance().closeAll();
    }

    @Test
    public void configuredDatabaseKeysShouldComeFromConfig() {
        assertEquals(DatabaseManager.getInstance().configuredDatabaseKeys(), List.of("events"));
    }

    @Test
    public void dslShouldCreateConfiguredH2Context() {
        DSLContext dsl = DatabaseManager.getInstance().dsl("events");

        dsl.execute("create table if not exists db_manager_test (id int primary key, status varchar(20))");
        dsl.execute("delete from db_manager_test");
        dsl.execute("insert into db_manager_test (id, status) values (1, 'ready')");

        String status = String.valueOf(dsl.fetchValue("select status from db_manager_test where id = 1"));

        assertEquals(status, "ready");
    }

    @Test
    public void dslShouldReuseContextByKey() {
        DatabaseManager manager = DatabaseManager.getInstance();

        DSLContext firstContext = manager.dsl("events");
        DSLContext secondContext = manager.dsl("events");

        assertSame(firstContext, secondContext);
    }

    @Test
    public void dslShouldFailForMissingDatabaseConfig() {
        assertThrows(NullPointerException.class, () -> DatabaseManager.getInstance().dsl("missing"));
    }
}
