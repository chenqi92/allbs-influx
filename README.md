## 使用
### 添加依赖
{% tabs tag-hide %}
<!-- tab maven -->
```xml
<dependency>
  <groupId>cn.allbs</groupId>
  <artifactId>allbs-influx</artifactId>
  <version>2.0.1</version>
</dependency>
```

<!-- endtab -->

<!-- tab Gradle -->

```
implementation 'cn.allbs:allbs-influx:2.0.1'
```

<!-- endtab -->

<!-- tab Kotlin -->

```
implementation("cn.allbs:allbs-influx:2.0.1")
```
<!-- endtab -->
{% endtabs %}

### 添加配置

```yaml
influx:
  open_url: http://192.168.1.111:8086
  username: ${INFLUX-USER:root}
  password: ${INFLUX-PWD:123456}
  database: allbstest
  # influxdb储存策略
  retention_policy: autogen
  # 储存永久
  retention_policy_time: 0s
```

### 启用

启动类添加注解`@EnableAllbsInflux`

### 在需要使用influxdb的类中注入template

```java
private final InfluxTemplate influxTemplate;
```

### 业务使用

#### 插入数据

time时间为系统默认时间

```java
// tags
Map<String, String> tagMap = new HashMap<>(2);
tagMap.put("entNo", "q0038");
tagMap.put("outletNo", "q0038g0001");
// fields
Map<String, Object> fieldMap = new HashMap<>(2);
fieldMap.put("IPA", "1");
fieldMap.put("pushTime", "2020-03-05 15:00:00");
influxTemplate.insert("表名", tagMap, fieldMap);
```

#### 表中time设定自定义时间

```java
influxTemplate.insert("表名", tagMap, fieldMap, Instant.now().toEpochMilli(), TimeUnit.MILLISECONDS);
```

#### 时区问题

当前插入数据都为当地实际时间，考虑到部分开发使用influxdb时会插入0时区的时间，所以可以自定义时间偏移量，下方代码插入时间将会比实际时间减少8小时

```java
influxTemplate.insert(database, tags, params, ZoneOffset.of("+8"));
```

#### 批量插入

考虑到批量插入时时间戳不能一致，所以不再提供自定义时间的参数，如果实在需要可以循环单个插入

```java
String database = "cq_test";
// tags
Map<String, String> tags = new HashMap<>();
tags.put("tag1", "1111");
tags.put("tag2", "22222");
// params
List<Map<String, Object>> list = new ArrayList<>();
for (int i = 0; i < 4; i++) {
    Map<String, Object> params = new HashMap<>();
    params.put("info", "测试数据" + i);
    params.put("type", i);
    list.add(params);
}
influxTemplate.batchInsert(database, tags, list);
```

![image-20230315155153105](https://nas.allbs.cn:9006/cloudpic/2023/03/6cf8a333b36952eaef8dbd77bc70f476.png)

#### 查询数据，工具未做处理

```java
QueryResult result = influxTemplate.query("SELECT * FROM \"zt_gas_waste\" order by time desc limit 100\n");
List<QueryResult.Series> series = result.getResults().get(0).getSeries();
```

#### 查询数据并转换为List<Map<String, Object>>

```java
String sql = "SELECT * FROM cq_test order by time desc";
List<Map<String, Object>> resList = influxTemplate.queryMapList(sql);
log.info(JsonUtil.toJson(resList));
```

![image-20230315155538528](https://nas.allbs.cn:9006/cloudpic/2023/03/0fe264dfbac3e78289e1f5531e02ee5c.png)

#### 查询数据并将时间格式化为指定类型

```java
String sql = "SELECT * FROM cq_test order by time desc";
List<Map<String, Object>> resList = influxTemplate.queryMapList(sql, "yyyy年MM月dd日HH时mm分ss秒");
log.info(JsonUtil.toJson(resList));
```

![image-20230315172034935](https://nas.allbs.cn:9006/cloudpic/2023/03/f90005cd1b08553fb9c740d44d9d08ad.png)

#### 转为实体类 List

```java
String sql = "SELECT * FROM cq_test order by time desc";
List<CqTest> resList = influxTemplate.queryBeanList(sql, CqTest.class);
log.info(resList.toString());
```

![image-20230316134739656](https://nas.allbs.cn:9006/cloudpic/2023/03/a453eaa909ce74542101461f37aa8dcb.png)
