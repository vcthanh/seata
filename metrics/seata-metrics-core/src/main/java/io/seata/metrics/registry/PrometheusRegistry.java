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
package io.seata.metrics.registry;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.core.lang.NonNull;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;

import java.time.Duration;

/**
 * @author phuctt4
 */
public class PrometheusRegistry {
    private static volatile PrometheusMeterRegistry INSTANCE;

    public static PrometheusMeterRegistry getInstance() {
        if(INSTANCE == null) {
            synchronized (PrometheusRegistry.class) {
                INSTANCE = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
                INSTANCE.config()
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
            }
        }
        return INSTANCE;
    }

}
