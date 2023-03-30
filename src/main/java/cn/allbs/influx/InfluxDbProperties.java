package cn.allbs.influx;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 功能: influxdb 连接配置
 *
 * @author ChenQi
 * @version 1.0
 * @since 2021/3/5 9:18
 */
@Data
@Component
@ConfigurationProperties(prefix = "influx")
public class InfluxDbProperties {

    /**
     * influxDb连接
     */
    private String openUrl;
    /**
     * 用户名
     */
    private String username;
    /**
     * 密码
     */
    private String password;
    /**
     * 数据库名
     */
    private String database;
    /**
     * 数据保存策略
     */
    private String retentionPolicy;
    /**
     * 留存时间 如2m、 3d
     */
    private String retentionPolicyTime;

    public String toString() {
        return "InfluxDbProperties(openUrl=" + this.getOpenUrl() + ", username=" + this.getUsername() + ", password=[protected], database=" + this.getDatabase() + ", retentionPolicy=" + this.getRetentionPolicy() + ", retentionPolicyTime=" + this.getRetentionPolicyTime() + ")";
    }
}
