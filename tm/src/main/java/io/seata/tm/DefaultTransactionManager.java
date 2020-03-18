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
package io.seata.tm;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.seata.core.exception.TmTransactionException;
import io.seata.core.exception.TransactionException;
import io.seata.core.exception.TransactionExceptionCode;
import io.seata.core.model.GlobalStatus;
import io.seata.core.model.TransactionManager;
import io.seata.core.protocol.ResultCode;
import io.seata.core.protocol.transaction.AbstractTransactionRequest;
import io.seata.core.protocol.transaction.AbstractTransactionResponse;
import io.seata.core.protocol.transaction.GlobalBeginRequest;
import io.seata.core.protocol.transaction.GlobalBeginResponse;
import io.seata.core.protocol.transaction.GlobalCommitRequest;
import io.seata.core.protocol.transaction.GlobalCommitResponse;
import io.seata.core.protocol.transaction.GlobalReportRequest;
import io.seata.core.protocol.transaction.GlobalReportResponse;
import io.seata.core.protocol.transaction.GlobalRollbackRequest;
import io.seata.core.protocol.transaction.GlobalRollbackResponse;
import io.seata.core.protocol.transaction.GlobalStatusRequest;
import io.seata.core.protocol.transaction.GlobalStatusResponse;
import io.seata.core.rpc.netty.TmRpcClient;
import zalopay.metrics.PrometheusRegistry;

/**
 * The type Default transaction manager.
 *
 * @author sharajava
 */
public class DefaultTransactionManager implements TransactionManager {

    private PrometheusMeterRegistry registry = PrometheusRegistry.getInstance();
    private static final String METRICS_NAME = "seata_tm_client";
    private static final String APPLICATION_NAME = "seata";

    @Override
    public String begin(String applicationId, String transactionServiceGroup, String name, int timeout)
        throws TransactionException {
        GlobalBeginRequest request = new GlobalBeginRequest();
        request.setTransactionName(name);
        request.setTimeout(timeout);
        GlobalBeginResponse response = (GlobalBeginResponse)syncCall(request, "begin_transaction");
        if (response.getResultCode() == ResultCode.Failed) {
            registry.counter(METRICS_NAME,
                    "application", APPLICATION_NAME,
                    "type", "counter",
                    "operation", "begin_transaction",
                    "status", "failed").increment();
            throw new TmTransactionException(TransactionExceptionCode.BeginFailed, response.getMsg());
        }
        return response.getXid();
    }

    @Override
    public GlobalStatus commit(String xid) throws TransactionException {
        GlobalCommitRequest globalCommit = new GlobalCommitRequest();
        globalCommit.setXid(xid);
        GlobalCommitResponse response = (GlobalCommitResponse)syncCall(globalCommit, "commit_transaction");
        return response.getGlobalStatus();
    }

    @Override
    public GlobalStatus rollback(String xid) throws TransactionException {
        GlobalRollbackRequest globalRollback = new GlobalRollbackRequest();
        globalRollback.setXid(xid);
        GlobalRollbackResponse response = (GlobalRollbackResponse)syncCall(globalRollback, "rollback_transaction");
        return response.getGlobalStatus();
    }

    @Override
    public GlobalStatus getStatus(String xid) throws TransactionException {
        GlobalStatusRequest queryGlobalStatus = new GlobalStatusRequest();
        queryGlobalStatus.setXid(xid);
        GlobalStatusResponse response = (GlobalStatusResponse)syncCall(queryGlobalStatus, "query_status_transaction");
        return response.getGlobalStatus();
    }

    @Override
    public GlobalStatus globalReport(String xid, GlobalStatus globalStatus) throws TransactionException {
        GlobalReportRequest globalReport = new GlobalReportRequest();
        globalReport.setXid(xid);
        globalReport.setGlobalStatus(globalStatus);
        GlobalReportResponse response = (GlobalReportResponse) syncCall(globalReport, "report_transaction");
        return response.getGlobalStatus();
    }

    private AbstractTransactionResponse syncCall(AbstractTransactionRequest request, String operation) throws TransactionException {
        try {
            long begin = System.currentTimeMillis();
            AbstractTransactionResponse response = (AbstractTransactionResponse)TmRpcClient.getInstance().sendMsgWithResponse(request);
            registry.timer(METRICS_NAME,
                    "application", APPLICATION_NAME,
                    "type", "timer",
                    "operation", operation)
                    .record(System.currentTimeMillis() - begin, TimeUnit.MILLISECONDS);
            registry.counter(METRICS_NAME,
                    "application", APPLICATION_NAME,
                    "type", "counter",
                    "operation", operation,
                    "status", "succeeded").increment();
            return response;
        } catch (TimeoutException toe) {
            registry.counter(METRICS_NAME,
                    "application", APPLICATION_NAME,
                    "type", "counter",
                    "operation", operation,
                    "status", "timeout").increment();
            throw new TmTransactionException(TransactionExceptionCode.IO, "RPC timeout", toe);
        }
    }
}
