package cn.allbs.influx.client;

import cn.allbs.influx.InfluxDbClient;
import cn.allbs.influx.InfluxDbProperties;
import cn.allbs.influx.exception.InfluxdbException;
import org.influxdb.BatchOptions;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.slf4j.Logger;

/**
 * 功能:
 *
 * @author ChenQi
 * @version 1.0
 * @since 2021/3/5 9:20
 */
public class DefaultInfluxTemplate extends InfluxDbClient {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(DefaultInfluxTemplate.class);

    public DefaultInfluxTemplate(InfluxDbProperties influxDbProperties, BatchOptions batchOptions) {
        super(influxDbProperties, batchOptions);
    }

    @Override
    public InfluxDB buildInfluxDb() {
        if (influxdb == null) {
            try {
                influxdb = InfluxDBFactory.connect(influxDbProperties.getOpenUrl(), influxDbProperties.getUsername(), influxDbProperties.getPassword());
                createDatabase(this.database);
                influxdb.setDatabase(this.database);
                log.debug("init influxDb, current configuration is {}", influxDbProperties);
            } catch (Exception e) {
                log.error("create database error {}", e.getMessage());
                if (!this.influxDbProperties.isSkipError()) {
                    throw new InfluxdbException("influxdb连接失败,请检查influxdb的服务地址及账号密码!");
                }
            }
        }
        return influxdb;
    }
}
