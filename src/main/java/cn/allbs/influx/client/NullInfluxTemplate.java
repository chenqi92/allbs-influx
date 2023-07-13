package cn.allbs.influx.client;

import cn.allbs.influx.InfluxDbClient;
import cn.allbs.influx.InfluxDbProperties;
import org.influxdb.BatchOptions;
import org.influxdb.InfluxDB;

/**
 * 类 NullInfluxTemplate
 *
 * @author ChenQi
 */
public class NullInfluxTemplate extends InfluxDbClient {

    public NullInfluxTemplate(InfluxDbProperties influxDbProperties, BatchOptions batchOptions) {
        super(influxDbProperties, batchOptions);
    }

    @Override
    public InfluxDB buildInfluxDb() {
        try {
            InfluxDbClient influxTemplate = new DefaultInfluxTemplate(influxDbProperties, batchOptions);
            influxTemplate.createRetentionPolicy();
            return influxTemplate.buildInfluxDb();
        } catch (Exception e) {
            return null;
        }
    }
}
