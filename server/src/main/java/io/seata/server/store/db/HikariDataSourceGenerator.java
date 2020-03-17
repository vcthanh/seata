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
package io.seata.server.store.db;

import com.zaxxer.hikari.HikariDataSource;
import io.seata.common.loader.LoadLevel;
import io.seata.core.store.db.AbstractDataSourceGenerator;

import javax.sql.DataSource;

/**
 * @author phuctt4
 */

@LoadLevel(name = "hikari")
public class HikariDataSourceGenerator extends AbstractDataSourceGenerator {
    @Override
    public DataSource generateDataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setDriverClassName(getDriverClassName());
        ds.setJdbcUrl(getUrl());
        ds.setUsername(getUser());
        ds.setPassword(getPassword());
//        ds.set(getMinConn());
        ds.setMaximumPoolSize(getMaxConn());
        ds.setMinimumIdle(getMinConn());
        ds.setMaxLifetime(5000);
//        ds.setTimeBetweenEvictionRunsMillis(120000);
//        ds.setMinEvictableIdleTimeMillis(300000);
//        ds.setTestWhileIdle(true);
//        ds.setTestOnBorrow(true);
//        ds.setPoolPreparedStatements(true);
//        ds.setMaxPoolPreparedStatementPerConnectionSize(20);
//        ds.set(getValidationQuery(getDBType()));
        ds.setAutoCommit(true);
        return ds;
    }
}
