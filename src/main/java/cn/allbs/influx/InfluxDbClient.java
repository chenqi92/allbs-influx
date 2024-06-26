package cn.allbs.influx;

import cn.allbs.influx.exception.InfluxdbException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.influxdb.BatchOptions;
import org.influxdb.InfluxDB;
import org.influxdb.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 抽象InfluxDB客户端
 * 提供InfluxDB的基本操作
 * <p>
 *
 * @author ChenQi
 * @version 1.0
 * @since 2021/3/5
 */
public abstract class InfluxDbClient implements InfluxTemplate {

    private static final Logger log = LoggerFactory.getLogger(InfluxDbClient.class);

    protected String database;
    protected String retentionPolicy;
    protected String retentionPolicyTime;
    protected InfluxDB influxdb;
    protected BatchOptions batchOptions;

    protected InfluxDbProperties influxDbProperties;

    @Resource
    private ObjectMapper mapper;

    public InfluxDbClient(InfluxDbProperties influxDbProperties, BatchOptions batchOptions) {
        this.batchOptions = batchOptions == null ? BatchOptions.DEFAULTS : batchOptions;
        this.database = influxDbProperties.getDatabase();
        this.retentionPolicy = Optional.ofNullable(influxDbProperties.getRetentionPolicy()).orElse("autogen");
        this.retentionPolicyTime = Optional.ofNullable(influxDbProperties.getRetentionPolicyTime()).orElse("0");
        this.influxDbProperties = influxDbProperties;
    }

    @Override
    public InfluxDB buildInfluxDb() {
        return influxdb;
    }

    /**
     * 设置数据保存策略: retentionPolicy策略名 / database 数据库名 / DURATION 数据保存时限 / REPLICATION副本个数 / 结尾 DEFAULT
     * DEFAULT表示设为默认的策略
     */
    @Override
    public void createRetentionPolicy() {
        createRetentionPolicy(retentionPolicy, retentionPolicyTime, 1, true);
    }

    /**
     * 设置自定义保留策略
     *
     * @param policyName  保留策略
     * @param duration    保留时限
     * @param replication 副本个数
     * @param isDefault   是否为默认策略
     */
    @Override
    public void createRetentionPolicy(String policyName, String duration, int replication, boolean isDefault) {
        String command = String.format("CREATE RETENTION POLICY \"%s\" ON \"%s\" DURATION %s REPLICATION %d %s",
                policyName, database, duration, replication, isDefault ? "DEFAULT" : "");
        executeQuery(command);
    }

    /**
     * 创建数据库
     *
     * @param database 库名
     */
    @Override
    public void createDatabase(String database) {
        executeQuery("CREATE DATABASE " + database);
    }

    /**
     * 操作数据库
     *
     * @param command 操作
     * @return 查询结果
     */
    @Override
    public QueryResult query(String command) {
        reConnect();
        return executeQuery(command);
    }

    /**
     * 执行查询操作并返回结果
     *
     * @param command 查询命令
     * @return 查询结果
     */
    private QueryResult executeQuery(String command) {
        try {
            return influxdb.query(new Query(command, database));
        } catch (Exception e) {
            log.error("allbs-influx warning！ InfluxDB operation execution failed due to: {}", e.getLocalizedMessage());
            throw new InfluxdbException("InfluxDB operation execution failed due to: " + e.getLocalizedMessage());
        }
    }

    /**
     * 读取相关数据并转为list 默认时间格式化为yyyy-MM-dd HH:mm:ss
     *
     * @param command sql语句
     * @return Map list
     */
    @Override
    public List<Map<String, Object>> queryMapList(String command) {
        return queryMapList(command, "yyyy-MM-dd HH:mm:ss");
    }

    /**
     * 读取相关数据并转为list
     *
     * @param command           sql语句
     * @param dateTimeFormatter 时间格式化
     * @return Map list
     */
    @Override
    public List<Map<String, Object>> queryMapList(String command, String dateTimeFormatter) {
        List<Map<String, Object>> resultList = new LinkedList<>();
        QueryResult queryResult = executeQuery(command);
        QueryResult.Result result = queryResult.getResults().get(0);
        if (result != null && result.getSeries() != null) {
            QueryResult.Series series = result.getSeries().get(0);
            if (series.getColumns() != null && series.getValues() != null) {
                List<String> columns = series.getColumns();
                series.getValues().forEach(values -> {
                    Map<String, Object> dataMap = new HashMap<>();
                    for (int i = 0; i < columns.size(); i++) {
                        String key = columns.get(i);
                        Object value = values.get(i);
                        if ("time".equals(key)) {
                            value = LocalDateTime.parse(value.toString(), DateTimeFormatter.ISO_DATE_TIME)
                                    .format(DateTimeFormatter.ofPattern(dateTimeFormatter));
                        }
                        dataMap.put(key, value);
                    }
                    resultList.add(dataMap);
                });
            }
        }
        return resultList;
    }

    /**
     * 读取相关数据并转为指定类型的list
     *
     * @param command    sql语句
     * @param targetType 目标类型
     * @param <T>        类型
     * @return 指定类型的list
     */
    @Override
    public <T> List<T> queryBeanList(String command, Class<T> targetType) {
        List<T> resultList = new LinkedList<>();
        QueryResult queryResult = executeQuery(command);
        QueryResult.Result result = queryResult.getResults().get(0);
        if (result != null && result.getSeries() != null) {
            QueryResult.Series series = result.getSeries().get(0);
            if (series.getColumns() != null && series.getValues() != null) {
                List<String> columns = series.getColumns();
                resultList = series.getValues().stream().map(values -> {
                    Map<String, Object> dataMap = new HashMap<>();
                    for (int i = 0; i < columns.size(); i++) {
                        dataMap.put(columns.get(i), values.get(i));
                    }
                    try {
                        return mapper.readValue(mapper.writeValueAsString(dataMap), targetType);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                }).collect(Collectors.toList());
            }
        }
        return resultList;
    }

    /**
     * 插入数据库 默认时区为当前系统所在的时区
     *
     * @param measurement 表名
     * @param tags        tag set
     * @param fields      field set
     */
    @Override
    public void insert(String measurement, Map<String, String> tags, Map<String, Object> fields) {
        insert(measurement, tags, fields, 0, null, ZoneOffset.UTC);
    }

    /**
     * 插入数据库
     *
     * @param measurement 表名
     * @param tags        tag set
     * @param fields      field set
     * @param zoneOffset  指定时区
     */
    @Override
    public void insert(String measurement, Map<String, String> tags, Map<String, Object> fields, ZoneOffset zoneOffset) {
        insert(measurement, tags, fields, 0, null, zoneOffset);
    }

    /**
     * 单条插入并指定时间戳
     *
     * @param measurement 表名
     * @param tags        tag set
     * @param fields      field set
     * @param time        时间戳
     * @param timeUnit    时间戳的单位
     * @param zoneOffset  时区
     */
    @Override
    public void insert(String measurement, Map<String, String> tags, Map<String, Object> fields, long time, TimeUnit timeUnit, ZoneOffset zoneOffset) {
        reConnect();
        if (time == 0) {
            time = LocalDateTime.now().toInstant(zoneOffset).toEpochMilli();
            timeUnit = TimeUnit.MILLISECONDS;
        }
        Point point = Point.measurement(measurement).time(time, timeUnit).tag(tags).fields(fields).build();
        try {
            influxdb.write(database, retentionPolicy, point);
            log.info("allbs-influx notice: InfluxDB data [{}] insertion successful.", point);
        } catch (Exception e) {
            log.error("allbs-influx warning! InfluxDB operation execution failed due to: {}", e.getLocalizedMessage());
            throw new InfluxdbException("InfluxDB operation execution failed due to: " + e.getLocalizedMessage());
        }
    }

    /**
     * 单条插入并指定时间
     *
     * @param measurement 表名
     * @param tags        tag set
     * @param fields      field set
     * @param time        指定时间
     */
    @Override
    public void insert(String measurement, Map<String, String> tags, Map<String, Object> fields, LocalDateTime time) {
        insert(measurement, tags, fields, time, ZoneOffset.UTC);
    }

    /**
     * 单条插入并指定时间
     *
     * @param measurement 表名
     * @param tags        tag set
     * @param fields      field set
     * @param time        指定时间
     * @param zoneOffset  时区
     */
    @Override
    public void insert(String measurement, Map<String, String> tags, Map<String, Object> fields, LocalDateTime time, ZoneOffset zoneOffset) {
        insert(measurement, tags, fields, time.toInstant(zoneOffset).toEpochMilli(), TimeUnit.MILLISECONDS, zoneOffset);
    }

    /**
     * 批量插入数据，默认时区为UTC
     *
     * @param measurement 表名
     * @param tags        tag set
     * @param fieldLists  field list
     */
    @Override
    public void batchInsert(String measurement, Map<String, String> tags, List<Map<String, Object>> fieldLists) {
        batchInsert(measurement, tags, fieldLists, ZoneOffset.UTC);
    }

    /**
     * 批量插入数据，指定时区
     *
     * @param measurement 表名
     * @param tags        tag set
     * @param fieldLists  field list
     * @param zoneOffset  时区
     */
    @Override
    public void batchInsert(String measurement, Map<String, String> tags, List<Map<String, Object>> fieldLists, ZoneOffset zoneOffset) {
        reConnect();
        BatchPoints batchPoints = BatchPoints.database(database)
                .retentionPolicy(retentionPolicy)
                .consistency(InfluxDB.ConsistencyLevel.ALL)
                .build();

        for (int i = 0; i < fieldLists.size(); i++) {
            Point point = Point.measurement(measurement)
                    .time(LocalDateTime.now().toInstant(zoneOffset).toEpochMilli() * 1000 + i, TimeUnit.MICROSECONDS)
                    .tag(tags)
                    .fields(fieldLists.get(i))
                    .build();
            batchPoints.point(point);
        }

        try {
            influxdb.write(batchPoints);
            log.info("allbs-influx notice: InfluxDB batch data [{}] insertion successful.", batchPoints);
        } catch (Exception e) {
            log.error("allbs-influx warning! InfluxDB operation execution failed due to: {}", e.getLocalizedMessage());
            throw new InfluxdbException("InfluxDB operation execution failed due to: " + e.getLocalizedMessage());
        }
    }

    /**
     * 批量插入数据，指定时间
     *
     * @param measurement 表名
     * @param tags        tag set
     * @param fieldLists  field list
     * @param time        指定时间
     */
    public void batchInsert(String measurement, Map<String, String> tags, List<Map<String, Object>> fieldLists, LocalDateTime time) {
        reConnect();
        BatchPoints batchPoints = BatchPoints.database(database)
                .retentionPolicy(retentionPolicy)
                .consistency(InfluxDB.ConsistencyLevel.ALL)
                .build();

        for (int i = 0; i < fieldLists.size(); i++) {
            Point point = Point.measurement(measurement)
                    .time(time.toInstant(ZoneOffset.UTC).toEpochMilli() * 1000 + i, TimeUnit.MICROSECONDS)
                    .tag(tags)
                    .fields(fieldLists.get(i))
                    .build();
            batchPoints.point(point);
        }

        try {
            influxdb.write(batchPoints);
            log.info("allbs-influx notice: InfluxDB batch data [{}] insertion successful", batchPoints);
        } catch (Exception e) {
            log.error("allbs-influx warning! InfluxDB operation execution failed due to: {}", e.getLocalizedMessage());
            throw new InfluxdbException("InfluxDB operation execution failed due to: " + e.getLocalizedMessage());
        }
    }

    /**
     * 批量插入数据，指定时间和时区
     *
     * @param measurement 表名
     * @param tags        tag set
     * @param fieldLists  field list
     * @param time        指定时间
     * @param zoneOffset  时区
     */
    public void batchInsert(String measurement, Map<String, String> tags, List<Map<String, Object>> fieldLists, LocalDateTime time, ZoneOffset zoneOffset) {
        reConnect();
        BatchPoints batchPoints = BatchPoints.database(database)
                .retentionPolicy(retentionPolicy)
                .consistency(InfluxDB.ConsistencyLevel.ALL)
                .build();

        for (int i = 0; i < fieldLists.size(); i++) {
            Point point = Point.measurement(measurement)
                    .time(time.toInstant(zoneOffset).toEpochMilli() * 1000 + i, TimeUnit.MICROSECONDS)
                    .tag(tags)
                    .fields(fieldLists.get(i))
                    .build();
            batchPoints.point(point);
        }

        try {
            influxdb.write(batchPoints);
            log.info("allbs-influx notice: InfluxDB batch data [{}] insertion successful.", batchPoints);
        } catch (Exception e) {
            log.error("allbs-influx warning! InfluxDB operation execution failed due to: {}", e.getLocalizedMessage());
            throw new InfluxdbException("InfluxDB operation execution failed due to: " + e.getLocalizedMessage());
        }
    }

    /**
     * 多库多表多条数据插入
     *
     * @param batchPoints 多条插入数据
     */
    @Override
    public void batchInsert(BatchPoints batchPoints) {
        reConnect();
        try {
            influxdb.write(batchPoints);
            log.info("allbs-influx notice: InfluxDB batch data [{}] insertion successful.", batchPoints);
        } catch (Exception e) {
            log.error("allbs-influx warning! InfluxDB operation execution failed due to: {}", e.getLocalizedMessage());
            throw new InfluxdbException("InfluxDB operation execution failed due to: " + e.getLocalizedMessage());
        }
    }

    /**
     * 批量操作结束时手动刷新数据
     */
    @Override
    public void flush() {
        if (influxdb != null) {
            influxdb.flush();
        }
    }

    /**
     * 启用批量操作，操作结束时必须调用disableBatch或者手动flush
     */
    @Override
    public void enableBatch() {
        if (influxdb != null) {
            influxdb.enableBatch(this.batchOptions);
        }
    }

    /**
     * 禁用批量操作
     */
    @Override
    public void disableBatch() {
        if (influxdb != null) {
            influxdb.disableBatch();
        }
    }

    /**
     * 测试是否已正常连接
     *
     * @return 是否已连接
     */
    @Override
    public boolean ping() {
        try {
            Pong pong = influxdb.ping();
            return pong != null;
        } catch (Exception e) {
            log.error("allbs-influx warning! InfluxDB ping failed due to: {}", e.getLocalizedMessage());
            return false;
        }
    }

    /**
     * 断线重连
     */
    @Override
    public void reConnect() {
        if (influxdb == null) {
            this.influxdb = buildInfluxDb();
        }
    }
}
