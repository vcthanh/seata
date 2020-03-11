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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.google.common.eventbus.Subscribe;
import io.seata.core.event.DatabaseEvent;
import io.seata.core.event.Event;
import io.seata.core.event.GlobalTransactionEvent;
import io.seata.core.event.OperationEvent;
import io.seata.core.model.GlobalStatus;
import io.seata.metrics.registry.Registry;
import io.seata.server.store.TransactionStoreManager;

/**
 * Event subscriber for metrics
 *
 * @author zhengyangyong
 */
public class MetricsSubscriber {
    private final Registry registry;

    private final Map<String, Consumer<Event>> consumers;

    public MetricsSubscriber(Registry registry) {
        this.registry = registry;
        consumers = new HashMap<>();
        consumers.put(GlobalStatus.Begin.toString(), this::processGlobalStatusBegin);
        consumers.put(GlobalStatus.Committed.toString(), this::processGlobalStatusCommitted);
        consumers.put(GlobalStatus.Rollbacked.toString(), this::processGlobalStatusRollbacked);

        consumers.put(GlobalStatus.CommitFailed.toString(), this::processGlobalStatusCommitFailed);
        consumers.put(GlobalStatus.RollbackFailed.toString(), this::processGlobalStatusRollbackFailed);
        consumers.put(GlobalStatus.TimeoutRollbacked.toString(), this::processGlobalStatusTimeoutRollbacked);
        consumers.put(GlobalStatus.TimeoutRollbackFailed.toString(), this::processGlobalStatusTimeoutRollbackFailed);
    }

    private void processGlobalStatusBegin(Event event) {
        registry.getCounter(MeterIdConstants.COUNTER_ACTIVE).increase(1);
    }

    private void processGlobalStatusCommitted(Event event) {
        registry.getCounter(MeterIdConstants.COUNTER_ACTIVE).decrease(1);
        registry.getCounter(MeterIdConstants.COUNTER_COMMITTED).increase(1);
        registry.getSummary(MeterIdConstants.SUMMARY_COMMITTED).increase(1);
        GlobalTransactionEvent globalTransactionEvent = (GlobalTransactionEvent)event;
        registry.getTimer(MeterIdConstants.TIMER_COMMITTED).record(globalTransactionEvent.getEndTime() - globalTransactionEvent.getBeginTime(),
            TimeUnit.MILLISECONDS);
    }

    private void processGlobalStatusRollbacked(Event event) {
        registry.getCounter(MeterIdConstants.COUNTER_ACTIVE).decrease(1);
        registry.getCounter(MeterIdConstants.COUNTER_ROLLBACKED).increase(1);
        registry.getSummary(MeterIdConstants.SUMMARY_ROLLBACKED).increase(1);
        GlobalTransactionEvent globalTransactionEvent = (GlobalTransactionEvent)event;
        registry.getTimer(MeterIdConstants.TIMER_ROLLBACK).record(globalTransactionEvent.getEndTime() - globalTransactionEvent.getBeginTime(),
            TimeUnit.MILLISECONDS);
    }

    private void processGlobalStatusCommitFailed(Event event) {
        registry.getCounter(MeterIdConstants.COUNTER_ACTIVE).decrease(1);
    }

    private void processGlobalStatusRollbackFailed(Event event) {
        registry.getCounter(MeterIdConstants.COUNTER_ACTIVE).decrease(1);
    }

    private void processGlobalStatusTimeoutRollbacked(Event event) {
        registry.getCounter(MeterIdConstants.COUNTER_ACTIVE).decrease(1);
    }

    private void processGlobalStatusTimeoutRollbackFailed(Event event) {
        registry.getCounter(MeterIdConstants.COUNTER_ACTIVE).decrease(1);
    }

    @Subscribe
    public void recordGlobalTransactionEventForMetrics(Event event) {
        if (registry != null && consumers.containsKey(event.getKey())) {
            consumers.get(event.getKey()).accept(event);
        }
    }
}
