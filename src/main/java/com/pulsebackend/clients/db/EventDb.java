package com.pulsebackend.clients.db;

import com.pulsebackend.clients.db.records.EventCompletedActionRecord;
import com.pulsebackend.clients.db.records.EventRecord;
import com.pulsebackend.controllers.DbController;
import com.pulsebackend.managers.DatabaseManager;
import org.jooq.DSLContext;

public class EventDb {
    public final DbController<EventRecord> event;
    public final DbController<EventCompletedActionRecord> eventCompletedAction;

    public EventDb() {
        DatabaseManager databaseManager = DatabaseManager.getInstance();
        DSLContext db = databaseManager.dsl("events");
        event = new DbController<>("event", EventRecord.class, db);
        eventCompletedAction = new DbController<>(
                "event_completed_action",
                EventCompletedActionRecord.class,
                db
        );
    }
}
