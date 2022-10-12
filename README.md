### 配置
```yaml
keray:
  schedule: 
    open: true # 定时任务配置
    namespace: dev #空间名
  api:
    rate-limit: true #开启接口令牌桶限流 默认开启 需要注入redis
    json: 
      open: true # json解析
      global-switch: # 全局开启json解析
    log: 
      all: true #api日志
    time: true # 开启接口时间记录
```

## cache使用
```yaml
spring:
  cache: 
    type: redis
    ehcache:
      config: xxxx.xml
```
#### 例子：
```xml
<ehcache>

    <diskStore path="java.io.tmpdir"/>
    <defaultCache maxElementsInMemory="10000" eternal="false"
                  timeToIdleSeconds="600" timeToLiveSeconds="600" overflowToDisk="false"/>

    <!--  大对象，实时性高，更新率高，10分钟存活时间 -->
    <cache name="cache:tree_data_cache" timeToLiveSeconds="600" />

</ehcache>

```

```java
// 使用
// 在入口类加上
@ComponentScan(value = "com.keray.common")
class Main {}
```
