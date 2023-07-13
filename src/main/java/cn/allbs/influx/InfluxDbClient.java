package cn.allbs.influx;

import cn.allbs.influx.exception.InfluxdbException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.influxdb.BatchOptions;
import org.influxdb.InfluxDB;
import org.influxdb.dto.*;
import org.slf4j.Logger;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 功能:
 *
 * @author ChenQi
 * @version 1.0
 * @since 2021/3/5 9:20
 */
public abstract class InfluxDbClient implements InfluxTemplate {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(InfluxDbClient.class);
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
        retentionPolicy = influxDbProperties.getRetentionPolicy();
        retentionPolicyTime = influxDbProperties.getRetentionPolicyTime();
        influxDbProperties.setRetentionPolicy(Optional.of(influxDbProperties).map(InfluxDbProperties::getRetentionPolicy).orElse("autogen"));
        influxDbProperties.setRetentionPolicyTime(Optional.of(influxDbProperties).map(InfluxDbProperties::getRetentionPolicyTime).orElse("0"));
        this.influxDbProperties = influxDbProperties;
    }

    @Override
    public InfluxDB buildInfluxDb() {
        return influxdb;
    }

    /**
     * 设置数据保存策略:retentionPolicy策略名 /database 数据库名/ DURATION 数据保存时限/REPLICATION副本个数/结尾 DEFAULT
     * DEFAULT表示设为默认的策略
     */
    @Override
    public void createRetentionPolicy() {
        String command = String.format("CREATE RETENTION POLICY \"%s\" ON \"%s\" DURATION %s REPLICATION %s DEFAULT", retentionPolicy, database, retentionPolicyTime, 1);
        this.query(command);
    }

    /**
     * 设置自定义保留策略
     *
     * @param policyName  保留策略
     * @param duration    保留时限
     * @param replication 副本个数
     * @param isDefault   是否为结尾
     */
    @Override
    public void createRetentionPolicy(String policyName, String duration, int replication, boolean isDefault) {
        String command = String.format("CREATE RETENTION POLICY \"%s\" ON \"%s\" DURATION %s REPLICATION %s ", policyName, database, duration, replication);
        if (isDefault) {
            command = command + " DEFAULT";
        }
        this.query(command);
    }

    /**
     * 创建数据库
     *
     * @param database 库名
     */
    @Override
    public void createDatabase(String database) {
        influxdb.query(new Query("CREATE DATABASE " + database));
    }

    /**
     * 操作数据库
     *
     * @param command 操作
     * @return @{@link QueryResult}
     */
    @Override
    public QueryResult query(String command) {
        reConnect();
        QueryResult queryResult;
        try {
            queryResult = influxdb.query(new Query(command, database));
        } catch (Exception e) {
            log.error("influxdb操作失败:{}", e.getLocalizedMessage());
            throw new InfluxdbException("influxdb操作失败:" + e.getLocalizedMessage());
        }
        return queryResult;
    }

    /**
     * 读取相关数据并转为list 默认时间格式化为yyyy-MM-dd HH:mm:ss
     *
     * @param command sql语句
     * @return Map list
     */
    @Override
    public List<Map<String, Object>> queryMapList(String command) {
        return this.queryMapList(command, "yyyy-MM-dd HH:mm:ss");
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
        List<Map<String, Object>> resList = new LinkedList<>();
        QueryResult queryResult = this.query(command);
        QueryResult.Result result = queryResult.getResults().get(0);
        if (Optional.of(result).map(QueryResult.Result::getSeries).isPresent()) {
            QueryResult.Series series = result.getSeries().get(0);
            if (Optional.of(series).map(QueryResult.Series::getColumns).isPresent() && Optional.of(series).map(QueryResult.Series::getValues).isPresent()) {
                List<String> columns = series.getColumns();
                series.getValues().forEach(v -> {
                    Map<String, Object> dataMap = new HashMap<>();
                    for (int i = 0; i < columns.size(); i++) {
                        String key = columns.get(i);
                        Object value = v.get(i);
                        if ("time".equals(key)) {
                            value = LocalDateTime.parse(value.toString(), DateTimeFormatter.ISO_DATE_TIME).format(DateTimeFormatter.ofPattern(dateTimeFormatter));
                        }
                        dataMap.put(key, value);
                    }
                    resList.add(dataMap);
                });
            }
        }
        return resList;
    }

    /**
     * 读取相关数据并转为list
     *
     * @param command sql语句
     * @return Map list
     */
    @Override
    public <T> List<T> queryBeanList(String command, Class<T> targetType) {
        List<T> resList = new LinkedList<>();
        QueryResult queryResult = this.query(command);
        QueryResult.Result result = queryResult.getResults().get(0);
        if (Optional.of(result).map(QueryResult.Result::getSeries).isPresent()) {
            QueryResult.Series series = result.getSeries().get(0);
            if (Optional.of(series).map(QueryResult.Series::getColumns).isPresent() && Optional.of(series).map(QueryResult.Series::getValues).isPresent()) {
                List<String> columns = series.getColumns();
                resList = series.getValues().stream().map((v) -> {
                    Map<String, Object> dataMap = new HashMap<>();
                    for (int i = 0; i < columns.size(); i++) {
                        String key = columns.get(i);
                        Object value = v.get(i);
                        dataMap.put(key, value);
                    }
                    try {

                        return mapper.readValue(mapper.writeValueAsString(dataMap), targetType);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                }).collect(Collectors.toList());
            }
        }
        return resList;
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
     * 方法功能: 单条插入并指定时间戳
     *
     * @param measurement 表名
     * @param tags        tag set
     * @param fields      field set
     * @param time        时间戳
     * @param timeUnit    时间戳的单位
     * @since 2021/3/5 9:32
     */
    @Override
    public void insert(String measurement, Map<String, String> tags, Map<String, Object> fields, long time, TimeUnit timeUnit, ZoneOffset zoneOffset) {
        reConnect();
        if (time == 0) {
            time = LocalDateTime.now().toInstant(zoneOffset).toEpochMilli();
            timeUnit = TimeUnit.MILLISECONDS;
        }
        Point point = Point.measurement(measurement).time(time, timeUnit).tag(tags).fields(fields).build();
        log.info(("influxDB insert data:" + point));
        try {
            influxdb.write(database, retentionPolicy, point);
        } catch (Exception e) {
            log.error("数据插入失败:{}", e.getLocalizedMessage());
            throw new InfluxdbException("数据插入失败:" + e.getLocalizedMessage());
        }
    }

    /**
     * tag 一定情况下的批量插入
     *
     * @param measurement 表名
     * @param tags        tag
     * @param fieldLists  field
     */
    @Override
    public void batchInsert(String measurement, Map<String, String> tags, List<Map<String, Object>> fieldLists, ZoneOffset zoneOffset) {
        reConnect();
        BatchPoints batchPoints = BatchPoints.database(database).retentionPolicy(retentionPolicy).consistency(InfluxDB.ConsistencyLevel.ALL).build();
        for (int i = 0; i < fieldLists.size(); i++) {
            Point point = Point.measurement(measurement).time(LocalDateTime.now().toInstant(zoneOffset).toEpochMilli() * 1000 + i, TimeUnit.MICROSECONDS).tag(tags).fields(fieldLists.get(i)).build();
            batchPoints.point(point);
        }
        log.info(("influxDB insert batch data:" + batchPoints));
        try {
            influxdb.write(batchPoints);
        } catch (Exception e) {
            log.error("数据批量插入失败:{}", e.getLocalizedMessage());
            throw new InfluxdbException("数据批量插入失败:" + e.getLocalizedMessage());
        }
    }

    /**
     * tag 一定情况下的批量插入
     *
     * @param measurement 表名
     * @param tags        tag
     * @param fieldLists  field
     */
    @Override
    public void batchInsert(String measurement, Map<String, String> tags, List<Map<String, Object>> fieldLists) {
        this.batchInsert(measurement, tags, fieldLists, ZoneOffset.UTC);
    }

    /**
     * 方法功能: 多库多表多条数据插入
     *
     * @param batchPoints 多条插入数据
     * @since 2021/3/5 9:35
     */
    @Override
    public void batchInsert(BatchPoints batchPoints) {
        reConnect();
        try {
            influxdb.write(batchPoints);
        } catch (Exception e) {
            log.error("数据批量插入失败:{}", e.getLocalizedMessage());
            throw new InfluxdbException("数据批量插入失败:" + e.getLocalizedMessage());
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
     * 如果调用了enableBatch,操作结束时必须调用disableBatch或者手动flush
     */
    @Override
    public void enableBatch() {
        if (influxdb != null) {
            influxdb.enableBatch(this.batchOptions);
        }
    }

    @Override
    public void disableBatch() {
        if (influxdb != null) {
            influxdb.disableBatch();
        }
    }

    /**
     * 测试是否已正常连接
     *
     * @return boolean
     */
    @Override
    public boolean ping() {
        boolean isConnected = false;
        Pong pong;
        try {
            pong = influxdb.ping();
            if (pong != null) {
                isConnected = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isConnected;
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
