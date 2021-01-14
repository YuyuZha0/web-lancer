package com.tencent.weblancer.web.repo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.CaseFormat;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * @author fishzhao
 * @since 2021-01-11
 */
@Slf4j
public final class DataSourceRegistry implements Function<String, DataSource>, AutoCloseable {

  private static final Pattern DATA_SOURCE_ID_PATTERN = Pattern.compile("[A-Za-z0-9_\\-]+");
  private static final int DEFAULT_MAX_POOL_SIZE = 3;

  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final Map<Pair<String, Class<?>>, Method> setterMap = new HashMap<>();
  private final ConcurrentMap<String, HikariConfig> configMap = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, HikariDataSource> dataSourceMap = new ConcurrentHashMap<>();
  private final ScheduledExecutorService poolScheduler = new ScheduledThreadPoolExecutor(
          Math.min(3, Runtime.getRuntime().availableProcessors()),
          new ThreadFactoryBuilder().setDaemon(true).setNameFormat("hikari-cp-pool-scheduler-%d").build()
  );

  {
    initializeSetterMap();
  }

  @Override
  public DataSource apply(String s) {
    checkNotClosed();
    checkDataSourceId(s);
    HikariConfig config = configMap.get(s);
    Preconditions.checkArgument(
            config != null, "No dataSource with id `%s` has been registered!", s);
    return dataSourceMap.computeIfAbsent(s, key -> new HikariDataSource(config));
  }

  private void checkNotClosed() {
    Preconditions.checkArgument(!closed.get(), "DataSourceRegistry already closed!");
  }

  private void initializeSetterMap() {
    Class<?> klass = HikariConfig.class;
    List<Method> methodList = new ArrayList<>(Arrays.asList(klass.getDeclaredMethods()));
    for (Class<?> superClass : ClassUtils.getAllSuperclasses(klass)) {
      methodList.addAll(Arrays.asList(superClass.getDeclaredMethods()));
    }
    for (Method method : methodList) {
      int mod = method.getModifiers();
      if (!Modifier.isPublic(mod) || Modifier.isStatic(mod)) {
        continue;
      }
      if (!method.getName().startsWith("set")) {
        continue;
      }
      Class<?>[] parameterTypes = method.getParameterTypes();
      if (parameterTypes.length != 1) {
        continue;
      }
      if (String.class.isAssignableFrom(parameterTypes[0])) {
        setterMap.put(Pair.of(method.getName(), String.class), method);
      }
      if (int.class.isAssignableFrom(parameterTypes[0])
          || Integer.class.isAssignableFrom(parameterTypes[0])) {
        setterMap.put(Pair.of(method.getName(), int.class), method);
      }
      if (long.class.isAssignableFrom(parameterTypes[0])
          || Long.class.isAssignableFrom(parameterTypes[0])) {
        setterMap.put(Pair.of(method.getName(), long.class), method);
      }
      if (boolean.class.isAssignableFrom(parameterTypes[0])
          || Boolean.class.isAssignableFrom(parameterTypes[0])) {
        setterMap.put(Pair.of(method.getName(), boolean.class), method);
      }
    }
    log.debug("[{}] setter(s) found in {}", setterMap.size(), klass);
  }

  public void registerDataSourceConfig(String id, ObjectNode config) {
    checkNotClosed();
    checkDataSourceId(id);
    Preconditions.checkArgument(config != null && !config.isEmpty(), "illegal config");
    HikariConfig hikariConfig = new HikariConfig();
    hikariConfig.setMaximumPoolSize(DEFAULT_MAX_POOL_SIZE);
    final Iterator<Map.Entry<String, JsonNode>> fields = config.fields();
    while (fields.hasNext()) {
      Map.Entry<String, JsonNode> entry = fields.next();
      JsonNode value = entry.getValue();
      if (value.isNull() || !value.isValueNode()) {
        continue;
      }
      if (trySet(hikariConfig, entry.getKey(), value)) {
        log.debug("HikariCP config `{}` applied successfully!", entry.getKey());
        continue;
      }
      String textValue = value.asText().trim();
      if (StringUtils.isNotEmpty(textValue)) {
        hikariConfig.addDataSourceProperty(entry.getKey(), textValue);
      }
    }
    hikariConfig.setScheduledExecutor(poolScheduler);
    configMap.put(id, hikariConfig);
  }

  private boolean trySet(HikariConfig config, String fieldName, JsonNode value) {
    String setterName = "set" + CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, fieldName);
    try {
      Method method;
      if ((method = setterMap.get(Pair.of(setterName, String.class))) != null) {
        String text = value.asText().trim();
        if (!text.isEmpty()) {
          method.invoke(config, text);
        } else {
          log.warn("Empty dataSource Config `{}` will be ignored!", fieldName);
        }
        return true;
      }
      if ((method = setterMap.get(Pair.of(setterName, int.class))) != null) {
        method.invoke(config, value.asInt());
        return true;
      }
      if ((method = setterMap.get(Pair.of(setterName, long.class))) != null) {
        method.invoke(config, value.asLong());
        return true;
      }
      if ((method = setterMap.get(Pair.of(setterName, boolean.class))) != null) {
        method.invoke(config, value.asLong());
        return true;
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return false;
  }

  private void checkDataSourceId(String s) {
    Preconditions.checkArgument(
            StringUtils.isNotEmpty(s) && DATA_SOURCE_ID_PATTERN.matcher(s).matches(),
            "illegal dataSourceId: %s",
            s);
  }

  @Override
  public void close() {
    if (!closed.compareAndSet(false, true)) {
      return;
    }
    List<String> dataSourceIdList = new ArrayList<>(dataSourceMap.keySet());
    for (String dataSourceId : dataSourceIdList) {
      HikariDataSource dataSource = dataSourceMap.remove(dataSourceId);
      if (dataSource != null) {
        dataSource.close();
        log.info("DataSource[{}] closed.", dataSourceId);
      }
    }
    //noinspection UnstableApiUsage
    MoreExecutors.shutdownAndAwaitTermination(poolScheduler, Duration.ofSeconds(2));
  }
}
