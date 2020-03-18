/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.server.metrics;

import com.google.common.eventbus.Subscribe;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.seata.core.event.Event;
import io.seata.core.event.GlobalTransactionEvent;
import io.seata.core.model.GlobalStatus;
import zalopay.event.DatabaseEvent;
import zalopay.event.OperationEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Event subscriber for metrics
 *
 * @author zhengyangyong
 */
public class CustomMetricsSubscriber {
    private final PrometheusMeterRegistry registry;

    private final Map<String, Consumer<Event>> consumers;

    private static final String APPLICATION_NAME = "seata";
    private static final String METRIC_DATABASE_NAME = "database";
    private static final String METRIC_TRANSACTION_NAME = "seata_transaction";

    public CustomMetricsSubscriber(PrometheusMeterRegistry registry) {
        this.registry = registry;
        consumers = new HashMap<>();

        consumers.put(OperationEvent.ADD_GLOBAL_SESSION.toString(), this::processAddGlobalSession);
        consumers.put(OperationEvent.UPDATE_GLOBAL_SESSION.toString(), this::processUpdateGlobalSession);
        consumers.put(OperationEvent.DELETE_GLOBAL_SESSION.toString(), this::processDeleteGlobalSession);
        consumers.put(OperationEvent.ADD_BRANCH_SESSION.toString(), this::processAddBranchSession);
        consumers.put(OperationEvent.UPDATE_BRANCH_SESSION.toString(), this::processUpdateBranchSession);
        consumers.put(OperationEvent.DELETE_BRANCH_SESSION.toString(), this::processDeleteBranchSession);

        consumers.put(GlobalStatus.Begin.toString(), this::processGlobalStatusBegin);
        consumers.put(GlobalStatus.Committed.toString(), this::processGlobalStatusCommitted);
        consumers.put(GlobalStatus.Rollbacked.toString(), this::processGlobalStatusRollbacked);

        consumers.put(GlobalStatus.CommitFailed.toString(), this::processGlobalStatusCommitFailed);
        consumers.put(GlobalStatus.RollbackFailed.toString(), this::processGlobalStatusRollbackFailed);
        consumers.put(GlobalStatus.TimeoutRollbacked.toString(), this::processGlobalStatusTimeoutRollbacked);
        consumers.put(GlobalStatus.TimeoutRollbackFailed.toString(), this::processGlobalStatusTimeoutRollbackFailed);
    }

    private void processAddGlobalSession(Event event) {
        DatabaseEvent databaseEvent = (DatabaseEvent) event;
        registry.timer(METRIC_DATABASE_NAME, "application", APPLICATION_NAME, "applicattion", APPLICATION_NAME, "type", "timer", "operation", "add_global_session")
                .record(databaseEvent.getEndTime() - databaseEvent.getBeginTime(), TimeUnit.MILLISECONDS);
    }

    private void processUpdateGlobalSession(Event event) {
        DatabaseEvent databaseEvent = (DatabaseEvent) event;
        registry.timer(METRIC_DATABASE_NAME, "application", APPLICATION_NAME, "applicattion", APPLICATION_NAME, "type", "timer", "operation", "update_global_session")
                .record(databaseEvent.getEndTime() - databaseEvent.getBeginTime(), TimeUnit.MILLISECONDS);
    }

    private void processDeleteGlobalSession(Event event) {
        DatabaseEvent databaseEvent = (DatabaseEvent) event;
        registry.timer(METRIC_DATABASE_NAME, "application", APPLICATION_NAME, "applicattion", APPLICATION_NAME,"type", "timer", "operation", "delete_global_session").record(databaseEvent.getEndTime() - databaseEvent.getBeginTime(), TimeUnit.MILLISECONDS);
    }

    private void processAddBranchSession(Event event) {
        DatabaseEvent databaseEvent = (DatabaseEvent) event;
        registry.timer(METRIC_DATABASE_NAME, "application", APPLICATION_NAME, "applicattion", APPLICATION_NAME,"type", "timer", "operation", "add_branch_session").record(databaseEvent.getEndTime() - databaseEvent.getBeginTime(), TimeUnit.MILLISECONDS);
    }

    private void processUpdateBranchSession(Event event) {
        DatabaseEvent databaseEvent = (DatabaseEvent) event;
        registry.timer(METRIC_DATABASE_NAME, "application", APPLICATION_NAME, "applicattion", APPLICATION_NAME,"type", "timer", "operation", "update_branch_session").record(databaseEvent.getEndTime() - databaseEvent.getBeginTime(), TimeUnit.MILLISECONDS);
    }

    private void processDeleteBranchSession(Event event) {
        DatabaseEvent databaseEvent = (DatabaseEvent) event;
        registry.timer(METRIC_DATABASE_NAME, "application", APPLICATION_NAME, "applicattion", APPLICATION_NAME,"type", "timer", "operation", "delete_branch_session").record(databaseEvent.getEndTime() - databaseEvent.getBeginTime(), TimeUnit.MILLISECONDS);
    }
    private void processGlobalStatusBegin(Event event) {
        registry.counter(METRIC_TRANSACTION_NAME, "application", APPLICATION_NAME, "application", APPLICATION_NAME, "application", APPLICATION_NAME, "type", "counter", "status", "active").increment();
    }

    private void processGlobalStatusCommitted(Event event) {
        registry.counter(METRIC_TRANSACTION_NAME, "application", APPLICATION_NAME, "application", APPLICATION_NAME, "application", APPLICATION_NAME, "type", "counter", "status", "active").increment(-1);
        registry.counter(METRIC_TRANSACTION_NAME, "application", APPLICATION_NAME, "application", APPLICATION_NAME, "application", APPLICATION_NAME, "type", "counter", "status", "committed").increment();
        GlobalTransactionEvent globalTransactionEvent = (GlobalTransactionEvent)event;
        registry.timer(METRIC_TRANSACTION_NAME, "application", APPLICATION_NAME, "application", APPLICATION_NAME, "application", APPLICATION_NAME, "type", "timer", "status", "committed").record(globalTransactionEvent.getEndTime() - globalTransactionEvent.getBeginTime(),
                TimeUnit.MILLISECONDS);
    }

    private void processGlobalStatusRollbacked(Event event) {
        registry.counter(METRIC_TRANSACTION_NAME, "application", APPLICATION_NAME, "application", APPLICATION_NAME, "application", APPLICATION_NAME, "type", "counter", "status", "active").increment(-1);
        registry.counter(METRIC_TRANSACTION_NAME, "application", APPLICATION_NAME, "application", APPLICATION_NAME, "application", APPLICATION_NAME, "type", "counter", "status", "rollbacked").increment(1);
        GlobalTransactionEvent globalTransactionEvent = (GlobalTransactionEvent)event;
        registry.timer(METRIC_TRANSACTION_NAME, "application", APPLICATION_NAME, "application", APPLICATION_NAME, "application", APPLICATION_NAME, "type", "timer", "status", "rollbacked").record(globalTransactionEvent.getEndTime() - globalTransactionEvent.getBeginTime(),
                TimeUnit.MILLISECONDS);
    }

    private void processGlobalStatusCommitFailed(Event event) {
        registry.counter(METRIC_TRANSACTION_NAME, "application", APPLICATION_NAME, "application", APPLICATION_NAME, "application", APPLICATION_NAME, "type", "counter", "status", "active").increment(-1);
    }

    private void processGlobalStatusRollbackFailed(Event event) {
        registry.counter(METRIC_TRANSACTION_NAME, "application", APPLICATION_NAME, "application", APPLICATION_NAME, "application", APPLICATION_NAME, "type", "counter", "status", "active").increment(-1);
    }

    private void processGlobalStatusTimeoutRollbacked(Event event) {
        registry.counter(METRIC_TRANSACTION_NAME, "application", APPLICATION_NAME, "application", APPLICATION_NAME, "application", APPLICATION_NAME, "type", "counter", "status", "active").increment(-1);
    }

    private void processGlobalStatusTimeoutRollbackFailed(Event event) {
        registry.counter(METRIC_TRANSACTION_NAME, "application", APPLICATION_NAME, "application", APPLICATION_NAME, "application", APPLICATION_NAME, "type", "counter", "status", "active").increment(-1);
    }

    @Subscribe
    public void recordGlobalTransactionEventForMetrics(Event event) {
        if (registry != null && consumers.containsKey(event.getKey())) {
            consumers.get(event.getKey()).accept(event);
        }
    }
}
