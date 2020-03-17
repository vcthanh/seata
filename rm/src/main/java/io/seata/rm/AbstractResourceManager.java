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
package io.seata.rm;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.seata.common.exception.NotSupportYetException;
import io.seata.core.exception.RmTransactionException;
import io.seata.core.exception.TransactionException;
import io.seata.core.exception.TransactionExceptionCode;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.core.model.Resource;
import io.seata.core.model.ResourceManager;
import io.seata.core.protocol.ResultCode;
import io.seata.core.protocol.transaction.BranchRegisterRequest;
import io.seata.core.protocol.transaction.BranchRegisterResponse;
import io.seata.core.protocol.transaction.BranchReportRequest;
import io.seata.core.protocol.transaction.BranchReportResponse;
import io.seata.core.rpc.netty.RmRpcClient;

import io.seata.metrics.registry.PrometheusRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * abstract ResourceManager
 *
 * @author zhangsen
 */
public abstract class AbstractResourceManager implements ResourceManager {

    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractResourceManager.class);
    private PrometheusMeterRegistry registry = PrometheusRegistry.getInstance();

    private static final String METRICS_NAME = "seata_rm_client";
    private static final String APPLICATION_NAME = "seata";

    /**
     * registry branch record
     * @param branchType the branch type
     * @param resourceId the resource id
     * @param clientId   the client id
     * @param xid        the xid
     * @param lockKeys   the lock keys
     * @return
     * @throws TransactionException
     */
    @Override
    public Long branchRegister(BranchType branchType, String resourceId, String clientId, String xid, String applicationData, String lockKeys) throws TransactionException {
        try {
            BranchRegisterRequest request = new BranchRegisterRequest();
            request.setXid(xid);
            request.setLockKey(lockKeys);
            request.setResourceId(resourceId);
            request.setBranchType(branchType);
            request.setApplicationData(applicationData);

            long begin = System.currentTimeMillis();
            BranchRegisterResponse response = (BranchRegisterResponse) RmRpcClient.getInstance().sendMsgWithResponse(request);
            registry.timer(METRICS_NAME,
                    "application", APPLICATION_NAME,
                    "type", "timer",
                    "operation", "register_branch")
                    .record(System.currentTimeMillis() - begin, TimeUnit.MILLISECONDS);
            registry.counter(METRICS_NAME,
                    "application", APPLICATION_NAME,
                    "type", "counter",
                    "operation", "register_branch",
                    "status", "succeeded").increment();
            if (response.getResultCode() == ResultCode.Failed) {
                throw new RmTransactionException(response.getTransactionExceptionCode(), String.format("Response[ %s ]", response.getMsg()));
            }
            return response.getBranchId();
        } catch (TimeoutException toe) {
            registry.counter(METRICS_NAME,
                    "application", APPLICATION_NAME,
                    "type", "counter",
                    "operation", "register_branch",
                    "status", "timeout").increment();
            throw new RmTransactionException(TransactionExceptionCode.IO, "RPC Timeout", toe);
        } catch (RuntimeException rex) {
            registry.counter(METRICS_NAME,
                    "application", APPLICATION_NAME,
                    "type", "counter",
                    "operation", "register_branch",
                    "status", "failed").increment();
            throw new RmTransactionException(TransactionExceptionCode.BranchRegisterFailed, "Runtime", rex);
        }
    }

    /**
     * report branch status
     * @param branchType      the branch type
     * @param xid             the xid
     * @param branchId        the branch id
     * @param status          the status
     * @param applicationData the application data
     * @throws TransactionException
     */
    @Override
    public void branchReport(BranchType branchType, String xid, long branchId, BranchStatus status, String applicationData) throws TransactionException {
        try {
            BranchReportRequest request = new BranchReportRequest();
            request.setXid(xid);
            request.setBranchId(branchId);
            request.setStatus(status);
            request.setApplicationData(applicationData);

            long begin = System.currentTimeMillis();
            BranchReportResponse response = (BranchReportResponse) RmRpcClient.getInstance().sendMsgWithResponse(request);
            registry.timer(METRICS_NAME,
                    "application", APPLICATION_NAME,
                    "type", "timer",
                    "operation", "report_branch")
                    .record(System.currentTimeMillis() - begin, TimeUnit.MILLISECONDS);
            registry.counter(METRICS_NAME,
                    "application", APPLICATION_NAME,
                    "type", "counter",
                    "operation", "report_branch",
                    "status", "succeeded").increment();
            if (response.getResultCode() == ResultCode.Failed) {
                throw new RmTransactionException(response.getTransactionExceptionCode(), String.format("Response[ %s ]", response.getMsg()));
            }
        } catch (TimeoutException toe) {
            registry.counter(METRICS_NAME,
                    "application", APPLICATION_NAME,
                    "type", "counter",
                    "operation", "report_branch",
                    "status", "timeout").increment();
            throw new RmTransactionException(TransactionExceptionCode.IO, "RPC Timeout", toe);
        } catch (RuntimeException rex) {
            registry.counter(METRICS_NAME,
                    "application", APPLICATION_NAME,
                    "type", "counter",
                    "operation", "report_branch",
                    "status", "failed").increment();
            throw new RmTransactionException(TransactionExceptionCode.BranchReportFailed, "Runtime", rex);
        }
    }

    @Override
    public boolean lockQuery(BranchType branchType, String resourceId, String xid, String lockKeys) throws TransactionException {
        return false;
    }

    @Override
    public void unregisterResource(Resource resource) {
        throw new NotSupportYetException("unregister a resource");
    }

    @Override
    public void registerResource(Resource resource) {
        RmRpcClient.getInstance().registerResource(resource.getResourceGroupId(), resource.getResourceId());
    }
}
