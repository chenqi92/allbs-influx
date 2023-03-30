package cn.allbs.influx.annotation;

import cn.allbs.influx.InfluxDbConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 自定义注解功能: 开启influxdb 功能
 *
 * @author ChenQi
 * @version 1.0
 * @since 2021/3/18 13:32
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import({InfluxDbConfiguration.class})
public @interface EnableAllbsInflux {
}
