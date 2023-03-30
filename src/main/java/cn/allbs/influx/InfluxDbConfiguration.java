package cn.allbs.influx;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
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
@AllArgsConstructor
public class InfluxDbConfiguration {

    private final InfluxDbProperties influxDbProperties;

    @Bean
    @ConditionalOnClass(ObjectMapper.class)
    @ConditionalOnMissingBean(InfluxTemplate.class)
    public InfluxTemplate influxTemplate() {
        BatchOptions batchOptions = BatchOptions.DEFAULTS;
        InfluxTemplate influxTemplate = new InfluxTemplate(influxDbProperties, batchOptions);
        influxTemplate.createRetentionPolicy();
        return influxTemplate;
    }
}
