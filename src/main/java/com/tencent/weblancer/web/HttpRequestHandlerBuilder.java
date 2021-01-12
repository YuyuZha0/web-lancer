package com.tencent.weblancer.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.tencent.weblancer.mybatis.jackson.JacksonBindingConfiguration;
import com.tencent.weblancer.web.conf.DynamicInterfaceConfig;
import com.tencent.weblancer.web.handler.GeneralQueryHandler;
import com.tencent.weblancer.web.repo.DataSourceRegistry;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.checkerframework.checker.nullness.qual.NonNull;

import javax.sql.DataSource;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

/**
 * @author fishzhao
 * @since 2021-01-11
 */
public final class HttpRequestHandlerBuilder implements Supplier<Handler<HttpServerRequest>> {

  private static final AtomicInteger STMT_COUNTER = new AtomicInteger(0);
  private static final Pattern URI_PATTERN = Pattern.compile("(/[a-zA-Z0-9_\\-]+)+|/");

  private final Vertx vertx;
  private final DataSourceRegistry dataSourceRegistry;
  private final Map<String, List<DynamicInterfaceConfig>> dataSourceIdConfigMap;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public HttpRequestHandlerBuilder(
      @NonNull Vertx vertx,
      @NonNull DataSourceRegistry dataSourceRegistry,
      @NonNull List<DynamicInterfaceConfig> configList) {
    checkConfigList(configList);
    this.vertx = vertx;
    this.dataSourceRegistry = dataSourceRegistry;
    this.dataSourceIdConfigMap =
        configList.stream()
            .map(c -> new AbstractMap.SimpleImmutableEntry<>(c.getDataSourceId(), c))
            .collect(groupingBy(Map.Entry::getKey, mapping(Map.Entry::getValue, toList())));
  }

  @Override
  public Handler<HttpServerRequest> get() {
    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create(false));
    Map<String, DynamicInterfaceConfig> stmtIdConfigMap = new HashMap<>();
    for (Map.Entry<String, List<DynamicInterfaceConfig>> entry : dataSourceIdConfigMap.entrySet()) {
      stmtIdConfigMap.clear();
      SqlSessionFactory sqlSessionFactory =
          createSqlSessionFactory(entry.getKey(), entry.getValue(), stmtIdConfigMap);
      for (Map.Entry<String, DynamicInterfaceConfig> entry1 : stmtIdConfigMap.entrySet()) {
        DynamicInterfaceConfig config = entry1.getValue();
        GeneralQueryHandler queryHandler =
            new GeneralQueryHandler(
                config.getParameterScopes(),
                config.getParameterValidation().orElse(null),
                sqlSessionFactory,
                entry1.getKey(),
                config.unwrapArray(),
                objectMapper);
        Set<HttpMethod> httpMethods = config.getHttpMethods();
        if (httpMethods == null || httpMethods.isEmpty()) {
          router.route(config.getUri()).handler(queryHandler);
        } else {
          for (HttpMethod method : httpMethods) {
            router.route(method, config.getUri()).handler(queryHandler);
          }
        }
      }
    }
    return router;
  }

  private SqlSessionFactory createSqlSessionFactory(
      String dataSourceId,
      Iterable<DynamicInterfaceConfig> configList,
      Map<String, DynamicInterfaceConfig> stmtIdConfigMap) {
    DataSource dataSource = dataSourceRegistry.apply(dataSourceId);
    Environment environment =
        new Environment(dataSourceId, new JdbcTransactionFactory(), dataSource);
    JacksonBindingConfiguration configuration =
        new JacksonBindingConfiguration(environment, objectMapper);
    for (DynamicInterfaceConfig config : configList) {
      String stmtId = stmtId(config.getDataSourceId(), config.getUri());
      configuration.addMappedStatement(createMappedStatement(config, configuration, stmtId));
      stmtIdConfigMap.put(stmtId, config);
    }
    return new SqlSessionFactoryBuilder().build(configuration);
  }

  private MappedStatement createMappedStatement(
      DynamicInterfaceConfig config, Configuration configuration, String stmtId) {
    return new MappedStatement.Builder(
            configuration,
            stmtId,
            configuration
                .getLanguageRegistry()
                .getDefaultDriver()
                .createSqlSource(
                    configuration,
                    Joiner.on(" ").join(config.getSqlScriptSegments()).trim(),
                    ObjectNode.class),
            SqlCommandType.SELECT)
        .resultMaps(
            Collections.singletonList(
                new ResultMap.Builder(
                        configuration, stmtId + "-resultMap", ObjectNode.class, new ArrayList<>())
                    .build()))
        .build();
  }

  private String stmtId(String dataSourceId, String uri) {
    return Strings.lenientFormat("%s(%s)#%s", dataSourceId, uri, STMT_COUNTER.incrementAndGet());
  }

  private void checkConfigList(List<DynamicInterfaceConfig> configList) {
    Preconditions.checkArgument(!configList.isEmpty(), "empty configList!");
    Set<Pair<HttpMethod, String>> uriSet = new HashSet<>();
    for (DynamicInterfaceConfig config : configList) {
      String uri = config.getUri();
      Preconditions.checkArgument(
          StringUtils.isNotEmpty(uri) && URI_PATTERN.matcher(uri).matches(),
          "illegal uri pattern: `%s`, valid pattern is: `%s`",
          uri,
          URI_PATTERN.pattern());
      for (HttpMethod httpMethod : config.getHttpMethods()) {
        Preconditions.checkArgument(
            uriSet.add(Pair.of(httpMethod, uri)), "duplicated uri: `%s(%s)`", uri, httpMethod);
      }
    }
  }
}
