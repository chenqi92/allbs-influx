package cn.allbs.influx;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 功能: influxdb 连接配置
 *
 * @author ChenQi
 * @version 1.0
 * @since 2021/3/5 9:18
 */
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

    private boolean skipError = false;

    public InfluxDbProperties() {
    }

    public String toString() {
        return "InfluxDbProperties(openUrl=" + this.getOpenUrl() + ", username=" + this.getUsername() + ", password=[protected], database=" + this.getDatabase() + ", retentionPolicy=" + this.getRetentionPolicy() + ", retentionPolicyTime=" + this.getRetentionPolicyTime() + ", skipError=" + this.isSkipError() + ")";
    }

    public String getOpenUrl() {
        return this.openUrl;
    }

    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }

    public String getDatabase() {
        return this.database;
    }

    public String getRetentionPolicy() {
        return this.retentionPolicy;
    }

    public String getRetentionPolicyTime() {
        return this.retentionPolicyTime;
    }

    public boolean isSkipError() {
        return this.skipError;
    }

    public void setOpenUrl(String openUrl) {
        this.openUrl = openUrl;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public void setRetentionPolicy(String retentionPolicy) {
        this.retentionPolicy = retentionPolicy;
    }

    public void setRetentionPolicyTime(String retentionPolicyTime) {
        this.retentionPolicyTime = retentionPolicyTime;
    }

    public void setSkipError(boolean skipError) {
        this.skipError = skipError;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof InfluxDbProperties)) return false;
        final InfluxDbProperties other = (InfluxDbProperties) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$openUrl = this.getOpenUrl();
        final Object other$openUrl = other.getOpenUrl();
        if (!Objects.equals(this$openUrl, other$openUrl)) return false;
        final Object this$username = this.getUsername();
        final Object other$username = other.getUsername();
        if (!Objects.equals(this$username, other$username)) return false;
        final Object this$password = this.getPassword();
        final Object other$password = other.getPassword();
        if (!Objects.equals(this$password, other$password)) return false;
        final Object this$database = this.getDatabase();
        final Object other$database = other.getDatabase();
        if (!Objects.equals(this$database, other$database)) return false;
        final Object this$retentionPolicy = this.getRetentionPolicy();
        final Object other$retentionPolicy = other.getRetentionPolicy();
        if (!Objects.equals(this$retentionPolicy, other$retentionPolicy))
            return false;
        final Object this$retentionPolicyTime = this.getRetentionPolicyTime();
        final Object other$retentionPolicyTime = other.getRetentionPolicyTime();
        if (!Objects.equals(this$retentionPolicyTime, other$retentionPolicyTime))
            return false;
        if (this.isSkipError() != other.isSkipError()) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof InfluxDbProperties;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $openUrl = this.getOpenUrl();
        result = result * PRIME + ($openUrl == null ? 43 : $openUrl.hashCode());
        final Object $username = this.getUsername();
        result = result * PRIME + ($username == null ? 43 : $username.hashCode());
        final Object $password = this.getPassword();
        result = result * PRIME + ($password == null ? 43 : $password.hashCode());
        final Object $database = this.getDatabase();
        result = result * PRIME + ($database == null ? 43 : $database.hashCode());
        final Object $retentionPolicy = this.getRetentionPolicy();
        result = result * PRIME + ($retentionPolicy == null ? 43 : $retentionPolicy.hashCode());
        final Object $retentionPolicyTime = this.getRetentionPolicyTime();
        result = result * PRIME + ($retentionPolicyTime == null ? 43 : $retentionPolicyTime.hashCode());
        result = result * PRIME + (this.isSkipError() ? 79 : 97);
        return result;
    }
}
