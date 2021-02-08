package com.tencent.weblancer.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.tencent.weblancer.external.JacksonBindingConfiguration;
import com.tencent.weblancer.web.conf.DynamicInterfaceDefinition;
import com.tencent.weblancer.web.handler.GeneralQueryHandler;
import com.tencent.weblancer.web.repo.DataSourceRegistry;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import org.apache.commons.lang3.StringUtils;
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
  private final Map<String, List<DynamicInterfaceDefinition>> dataSourceIdConfigMap;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public HttpRequestHandlerBuilder(
      @NonNull Vertx vertx,
      @NonNull DataSourceRegistry dataSourceRegistry,
      @NonNull List<DynamicInterfaceDefinition> dynamicInterfaceDefinitions) {
    checkDynamicInterfaceDefinitions(dynamicInterfaceDefinitions);
    this.vertx = vertx;
    this.dataSourceRegistry = dataSourceRegistry;
    this.dataSourceIdConfigMap =
        dynamicInterfaceDefinitions.stream()
            .map(c -> new AbstractMap.SimpleImmutableEntry<>(c.getDataSourceId(), c))
            .collect(groupingBy(Map.Entry::getKey, mapping(Map.Entry::getValue, toList())));
  }

  @Override
  public Handler<HttpServerRequest> get() {
    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create(false));
    Map<String, DynamicInterfaceDefinition> stmtIdConfigMap = new HashMap<>();
    for (Map.Entry<String, List<DynamicInterfaceDefinition>> entry :
        dataSourceIdConfigMap.entrySet()) {
      stmtIdConfigMap.clear();
      SqlSessionFactory sqlSessionFactory =
          createSqlSessionFactory(entry.getKey(), entry.getValue(), stmtIdConfigMap);
      for (Map.Entry<String, DynamicInterfaceDefinition> entry1 : stmtIdConfigMap.entrySet()) {
        DynamicInterfaceDefinition definition = entry1.getValue();
        GeneralQueryHandler queryHandler =
            new GeneralQueryHandler(
                definition.getParameterScopes(),
                definition.getParameterValidation().orElse(null),
                sqlSessionFactory,
                entry1.getKey(),
                definition.unwrapArray(),
                objectMapper);
        Set<HttpMethod> httpMethods = definition.getHttpMethods();
        if (httpMethods == null || httpMethods.isEmpty()) {
          router.route(definition.getUri()).handler(queryHandler);
        } else {
          for (HttpMethod method : httpMethods) {
            router.route(method, definition.getUri()).handler(queryHandler);
          }
        }
      }
    }
    return router;
  }

  private SqlSessionFactory createSqlSessionFactory(
      String dataSourceId,
      Iterable<DynamicInterfaceDefinition> dynamicInterfaceDefinitions,
      Map<String, DynamicInterfaceDefinition> stmtIdConfigMap) {
    DataSource dataSource = dataSourceRegistry.apply(dataSourceId);
    Environment environment =
        new Environment(dataSourceId, new JdbcTransactionFactory(), dataSource);
    JacksonBindingConfiguration configuration =
        new JacksonBindingConfiguration(environment, objectMapper);
    for (DynamicInterfaceDefinition definition : dynamicInterfaceDefinitions) {
      String stmtId = stmtId(definition.getDataSourceId(), definition.getUri());
      configuration.addMappedStatement(createMappedStatement(definition, configuration, stmtId));
      stmtIdConfigMap.put(stmtId, definition);
    }
    return new SqlSessionFactoryBuilder().build(configuration);
  }

  private MappedStatement createMappedStatement(
      DynamicInterfaceDefinition definition, Configuration configuration, String stmtId) {
    return new MappedStatement.Builder(
            configuration,
            stmtId,
            configuration
                .getLanguageRegistry()
                .getDefaultDriver()
                .createSqlSource(
                    configuration,
                    StringUtils.join(definition.getSqlScriptSegments(), ' ').trim(),
                    ObjectNode.class),
            SqlCommandType.SELECT)
        .resultMaps(
            Collections.singletonList(
                new ResultMap.Builder(
                        configuration,
                        stmtId + "-resultMap",
                        ObjectNode.class,
                        Collections.emptyList())
                    .build()))
        .build();
  }

  private String stmtId(String dataSourceId, String uri) {
    return Strings.lenientFormat("%s(%s)#%s", dataSourceId, uri, STMT_COUNTER.incrementAndGet());
  }

  private void checkDynamicInterfaceDefinitions(
      List<DynamicInterfaceDefinition> dynamicInterfaceDefinitions) {
    Preconditions.checkArgument(
        !dynamicInterfaceDefinitions.isEmpty(), "empty dynamicInterfaceDefinitions!");
    Map<String, Set<HttpMethod>> uriHttpMethodsMap = new HashMap<>();
    for (DynamicInterfaceDefinition definition : dynamicInterfaceDefinitions) {
      String uri = definition.getUri();
      Preconditions.checkArgument(
          StringUtils.isNotEmpty(uri) && URI_PATTERN.matcher(uri).matches(),
          "illegal uri pattern: `%s`, valid pattern is: `%s`",
          uri,
          URI_PATTERN.pattern());
      if (definition.getHttpMethods() == null || definition.getHttpMethods().isEmpty()) {
        Preconditions.checkArgument(
            uriHttpMethodsMap.put(uri, new HashSet<>(HttpMethod.values())) == null,
            "duplicated uri: `%s`",
            uri);
        continue;
      }
      for (HttpMethod httpMethod : definition.getHttpMethods()) {
        Preconditions.checkArgument(
            uriHttpMethodsMap
                .compute(uri, (k, v) -> v == null ? new HashSet<>() : v)
                .add(httpMethod),
            "duplicated uri: `%s(%s)`",
            uri,
            httpMethod);
      }
    }
  }
}
