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

import java.time.Duration;
import java.util.List;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.metrics.exporter.Exporter;
import io.seata.metrics.exporter.ExporterFactory;
import io.seata.metrics.exporter.prometheus.PrometheusExporter;
import io.seata.metrics.registry.Registry;
import io.seata.metrics.registry.RegistryFactory;
import io.seata.server.event.EventBusManager;

/**
 * Metrics manager for init
 *
 * @author zhengyangyong
 */
public class MetricsManager {
    private static class SingletonHolder {
        private static MetricsManager INSTANCE = new MetricsManager();
    }

    public static final MetricsManager get() {
        return MetricsManager.SingletonHolder.INSTANCE;
    }

    private Registry registry;
    private PrometheusMeterRegistry registryPrometheus;

    public Registry getRegistry() {
        return registry;
    }
    public PrometheusMeterRegistry getRegistryPrometheus() {
        return registryPrometheus;
    }

    public void init() {
        boolean enabled = ConfigurationFactory.getInstance().getBoolean(
            ConfigurationKeys.METRICS_PREFIX + ConfigurationKeys.METRICS_ENABLED, false);
        if (enabled) {
            registryPrometheus = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
            registryPrometheus.config()
                    .meterFilter(
                            new MeterFilter() {
                                @Override
                                public DistributionStatisticConfig configure(
                                        Meter.Id id, @NonNull DistributionStatisticConfig config) {

                                    return DistributionStatisticConfig.builder()
                                            .percentilesHistogram(false)
                                            .percentiles(0.99)
                                            .expiry(Duration.ofMinutes(1))
                                            .bufferLength(3)
                                            .sla(1, 2, 5, 10, 20, 30, 40, 50, 75, 100, 150, 200, 250, 500, 1000)
                                            .build()
                                            .merge(config);

                                }
                            });

                List<Exporter> exporters = ExporterFactory.getInstanceList();
                //only at least one metrics exporter implement had imported in pom then need register MetricsSubscriber
                if (exporters.size() != 0) {
                    exporters.forEach(exporter -> {
                        exporter.setRegistryPrometheus(registryPrometheus);
                        if(exporter instanceof PrometheusExporter) {
                            ((PrometheusExporter) exporter).init();
                        }
                    });
                    EventBusManager.get().register(new CustomMetricsSubscriber(registryPrometheus));
                }


        }
    }
}
