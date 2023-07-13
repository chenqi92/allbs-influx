package cn.allbs.influx;

import org.influxdb.InfluxDB;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.QueryResult;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 功能:
 *
 * @author ChenQi
 * @version 1.0
 * @since 2021/3/5 9:20
 */
public interface InfluxTemplate {

    InfluxDB buildInfluxDb();

    /**
     * 设置数据保存策略:retentionPolicy策略名 /database 数据库名/ DURATION 数据保存时限/REPLICATION副本个数/结尾 DEFAULT
     * DEFAULT表示设为默认的策略
     */
    void createRetentionPolicy();

    /**
     * 设置自定义保留策略
     *
     * @param policyName  保留策略
     * @param duration    保留时限
     * @param replication 副本个数
     * @param isDefault   是否为结尾
     */
    void createRetentionPolicy(String policyName, String duration, int replication, boolean isDefault);

    /**
     * 创建数据库
     *
     * @param database 库名
     */
    void createDatabase(String database);

    /**
     * 操作数据库
     *
     * @param command 操作
     * @return @{@link QueryResult}
     */
    QueryResult query(String command);

    /**
     * 读取相关数据并转为list 默认时间格式化为yyyy-MM-dd HH:mm:ss
     *
     * @param command sql语句
     * @return Map list
     */
    List<Map<String, Object>> queryMapList(String command);

    /**
     * 读取相关数据并转为list
     *
     * @param command           sql语句
     * @param dateTimeFormatter 时间格式化
     * @return Map list
     */
    List<Map<String, Object>> queryMapList(String command, String dateTimeFormatter);

    /**
     * 读取相关数据并转为list
     *
     * @param command sql语句
     * @return Map list
     */
    <T> List<T> queryBeanList(String command, Class<T> targetType);

    /**
     * 插入数据库 默认时区为当前系统所在的时区
     *
     * @param measurement 表名
     * @param tags        tag set
     * @param fields      field set
     */
    void insert(String measurement, Map<String, String> tags, Map<String, Object> fields);

    /**
     * 插入数据库
     *
     * @param measurement 表名
     * @param tags        tag set
     * @param fields      field set
     * @param zoneOffset  指定时区
     */
    void insert(String measurement, Map<String, String> tags, Map<String, Object> fields, ZoneOffset zoneOffset);

    /**
     * 方法功能: 单条插入并指定时间戳
     *
     * @param measurement 表名
     * @param tags        tag set
     * @param fields      field set
     * @param time        时间戳
     * @param timeUnit    时间戳的单位
     * @since 2021/3/5 9:32
     */
    void insert(String measurement, Map<String, String> tags, Map<String, Object> fields, long time, TimeUnit timeUnit, ZoneOffset zoneOffset);

    /**
     * tag 一定情况下的批量插入
     *
     * @param measurement 表名
     * @param tags        tag
     * @param fieldLists  field
     */
    void batchInsert(String measurement, Map<String, String> tags, List<Map<String, Object>> fieldLists, ZoneOffset zoneOffset);

    /**
     * tag 一定情况下的批量插入
     *
     * @param measurement 表名
     * @param tags        tag
     * @param fieldLists  field
     */
    void batchInsert(String measurement, Map<String, String> tags, List<Map<String, Object>> fieldLists);

    /**
     * 方法功能: 多库多表多条数据插入
     *
     * @param batchPoints 多条插入数据
     * @since 2021/3/5 9:35
     */
    void batchInsert(BatchPoints batchPoints);

    /**
     * 批量操作结束时手动刷新数据
     */
    void flush();

    /**
     * 如果调用了enableBatch,操作结束时必须调用disableBatch或者手动flush
     */
    void enableBatch();

    /**
     * 禁止批量操作
     */
    void disableBatch();

    /**
     * 测试是否已正常连接
     *
     * @return boolean
     */
    boolean ping();

    void reConnect();
}
