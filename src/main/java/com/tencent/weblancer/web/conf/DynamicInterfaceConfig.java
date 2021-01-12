package com.tencent.weblancer.web.conf;

import com.networknt.schema.JsonSchema;
import io.vertx.core.http.HttpMethod;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author fishzhao
 * @since 2021-01-11
 */
public interface DynamicInterfaceConfig {

  String getDataSourceId();

  String getUri();

  Set<HttpMethod> getHttpMethods();

  // 按优先级排序，同名参数后面的会覆盖前面的
  LinkedHashSet<ParameterScope> getParameterScopes();

  List<String> getSqlScriptSegments();

  boolean unwrapArray();

  Optional<JsonSchema> getParameterValidation();
}
