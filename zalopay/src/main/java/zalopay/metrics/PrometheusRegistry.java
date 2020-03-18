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
package zalopay.metrics;

import com.sun.net.httpserver.HttpServer;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.core.lang.NonNull;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import zalopay.constants.ZalopayConfigurationKeys;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static io.seata.core.constants.ConfigurationKeys.METRICS_EXPORTER_PROMETHEUS_PORT;

/**
 * @author phuctt4
 */
public class PrometheusRegistry {
    private static volatile PrometheusMeterRegistry INSTANCE;

    private PrometheusRegistry() {}

    private static void initServer() {
        int port = ConfigurationFactory.getInstance().getInt(
                ZalopayConfigurationKeys.METRICS_PREFIX + ZalopayConfigurationKeys.METRICS_PORT, 9898);
        List<Tag> tags = Arrays.asList(Tag.of("application", "seata"));
        new ClassLoaderMetrics(tags).bindTo(INSTANCE);
        new JvmMemoryMetrics(tags).bindTo(INSTANCE);
        new JvmGcMetrics(tags).bindTo(INSTANCE);
        new ProcessorMetrics(tags).bindTo(INSTANCE);
        new JvmThreadMetrics(tags).bindTo(INSTANCE);
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/metrics", httpExchange -> {
                String response = INSTANCE.scrape();
                httpExchange.sendResponseHeaders(200, response.getBytes().length);
                try (OutputStream os = httpExchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            });

            new Thread(server::start).start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

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

                initServer();
            }
        }
        return INSTANCE;
    }

}
