package cn.allbs.influx;

import cn.allbs.influx.client.DefaultInfluxTemplate;
import cn.allbs.influx.client.NullInfluxTemplate;
import cn.allbs.influx.exception.InfluxdbException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.influxdb.BatchOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 功能:
 *
 * @author ChenQi
 * @version 1.0
 * @since 2021/3/5 9:19
 */
@Configuration
@EnableConfigurationProperties({InfluxDbProperties.class})
public class InfluxDbConfiguration {

    private final InfluxDbProperties influxDbProperties;

    public InfluxDbConfiguration(InfluxDbProperties influxDbProperties) {
        this.influxDbProperties = influxDbProperties;
    }

    @Bean
    @ConditionalOnClass({ObjectMapper.class})
    @ConditionalOnMissingBean(InfluxTemplate.class)
    public InfluxTemplate influxTemplate() {
        BatchOptions batchOptions = BatchOptions.DEFAULTS;
        try {
            InfluxTemplate influxDbClient = new DefaultInfluxTemplate(influxDbProperties, batchOptions);
            influxDbClient.createRetentionPolicy();
            return influxDbClient;
        } catch (Exception e) {
            if (this.influxDbProperties.isSkipError()) {
                return new NullInfluxTemplate(influxDbProperties, batchOptions);
            } else {
                throw new InfluxdbException("Failed to create InfluxDbClient bean", e);
            }
        }
    }
}
