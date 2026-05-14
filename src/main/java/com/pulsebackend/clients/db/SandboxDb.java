package com.pulsebackend.clients.db;

import com.pulsebackend.clients.api.pojo.SandboxResult;
import com.pulsebackend.controllers.DbController;
import com.pulsebackend.managers.DatabaseManager;
import org.jooq.DSLContext;

public class SandboxDb {
    public final DbController<SandboxResult> sandboxOrders;

    public SandboxDb() {
        DatabaseManager databaseManager = DatabaseManager.getInstance();
        DSLContext db = databaseManager.dsl("events");
        sandboxOrders = new DbController<>("sandbox_orders", SandboxResult.class, db);
    }
}
