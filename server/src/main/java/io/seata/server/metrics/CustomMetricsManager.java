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

import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.seata.config.ConfigurationFactory;
import io.seata.server.event.EventBusManager;
import zalopay.constants.ZalopayConfigurationKeys;
import zalopay.metrics.PrometheusRegistry;

/**
 * @author phuctt4
 */
public class CustomMetricsManager {
    private static class SingletonHolder {
        private static CustomMetricsManager INSTANCE = new CustomMetricsManager();
    }

    public static final CustomMetricsManager get() {
        return CustomMetricsManager.SingletonHolder.INSTANCE;
    }

    private PrometheusMeterRegistry registryPrometheus;

    public void init() {
        boolean enabled = ConfigurationFactory.getInstance().getBoolean(
                ZalopayConfigurationKeys.METRICS_PREFIX + ZalopayConfigurationKeys.METRICS_ENABLED, false);
        if (enabled) {
            registryPrometheus = PrometheusRegistry.getInstance();
            EventBusManager.get().register(new CustomMetricsSubscriber(registryPrometheus));
        }
    }
}
