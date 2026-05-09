package com.pulsebackend.clients.db;

import com.pulsebackend.clients.db.records.EventCompletedActionRecord;
import com.pulsebackend.clients.db.records.EventRecord;
import com.pulsebackend.managers.DatabaseManager;
import org.jooq.DSLContext;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;

public class DbClientsTest {
    @AfterMethod
    public void closeDatabaseConnections() {
        DatabaseManager.getInstance().closeAll();
    }

    @Test
    public void eventDbShouldExposeTwoTypedTableControllers() {
        EventDb eventDb = new EventDb();

        assertEquals(eventDb.event.getTableName(), "event");
        assertEquals(eventDb.event.getType(), EventRecord.class);
        assertEquals(eventDb.eventCompletedAction.getTableName(), "event_completed_action");
        assertEquals(eventDb.eventCompletedAction.getType(), EventCompletedActionRecord.class);
    }

    @Test
    public void eventDbShouldReturnEventRecordType() {
        DSLContext dsl = DatabaseManager.getInstance().dsl("events");
        dsl.execute("drop table if exists event");
        dsl.execute("create table event (id varchar(40) primary key, customer_id varchar(40), event_id varchar(40), event_status varchar(20), source varchar(20))");
        dsl.execute("insert into event (id, customer_id, event_id, event_status, source) values ('row-1', 'customer-1', 'event-1', 'created', 'mobile')");

        EventDb eventDb = new EventDb();
        List<EventRecord> records = eventDb.event.getRecords(Map.of("id", "row-1"));

        assertEquals(records.size(), 1);
        EventRecord record = records.get(0);
        assertEquals(record.id, "row-1");
        assertEquals(record.customer_id, "customer-1");
        assertEquals(record.event_status, "created");
    }

    @Test
    public void eventDbShouldReturnEventCompletedActionRecordType() {
        DSLContext dsl = DatabaseManager.getInstance().dsl("events");
        dsl.execute("drop table if exists event_completed_action");
        dsl.execute("create table event_completed_action (id varchar(40) primary key, customer_id varchar(40), event_id varchar(40), source varchar(20), status varchar(20), \"value\" varchar(20))");
        dsl.execute("insert into event_completed_action (id, customer_id, event_id, source, status, \"value\") values ('action-1', 'customer-1', 'event-1', 'mobile', 'done', '25.50')");

        EventDb eventDb = new EventDb();
        EventCompletedActionRecord record = eventDb.eventCompletedAction.waitForOneRecord(
                Map.of("id", "action-1"),
                500,
                10
        );

        assertEquals(record.id, "action-1");
        assertEquals(record.event_id, "event-1");
        assertEquals(record.status, "done");
        assertEquals(record.value, "25.5");
    }
}
