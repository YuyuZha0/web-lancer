package com.tencent.weblancer.web.conf;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import io.vertx.core.http.HttpMethod;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author fishzhao
 * @since 2021-01-12
 */
public final class JsonInterfaceDefinition implements DynamicInterfaceDefinition {

  private final String dataSourceId;
  private final String uri;
  private final Set<HttpMethod> httpMethods;
  private final List<ParameterScope> parameterScopes;
  private final List<String> sqlScriptsSegments;
  private final boolean unwrapArray;
  private final JsonNode parameterValidation;

  @JsonCreator
  public JsonInterfaceDefinition(
      @JsonProperty("dataSourceId") String dataSourceId,
      @JsonProperty("uri") String uri,
      @JsonProperty("httpMethods") @JsonAlias("httpMethod") Set<HttpMethod> httpMethods,
      @JsonProperty("parameterScopes") @JsonAlias("parameterScopes")
          List<ParameterScope> parameterScopes,
      @JsonProperty("sqlScriptsSegments") @JsonAlias({"sql", "sqlScriptsSegments"})
          List<String> sqlScriptsSegments,
      @JsonProperty("unwrapArray") boolean unwrapArray,
      @JsonProperty("parameterValidation") @JsonAlias("validation") JsonNode parameterValidation) {
    this.dataSourceId = dataSourceId;
    this.uri = uri;
    this.httpMethods = httpMethods;
    this.parameterScopes = parameterScopes;
    this.sqlScriptsSegments = sqlScriptsSegments;
    this.unwrapArray = unwrapArray;
    this.parameterValidation = parameterValidation;
  }

  @Override
  public String getDataSourceId() {
    return dataSourceId;
  }

  @Override
  public String getUri() {
    return uri;
  }

  @Override
  public Set<HttpMethod> getHttpMethods() {
    return httpMethods;
  }

  @Override
  public LinkedHashSet<ParameterScope> getParameterScopes() {
    if (parameterScopes == null || parameterScopes.isEmpty()) {
      return new LinkedHashSet<>(Arrays.asList(ParameterScope.QUERY_STRING, ParameterScope.BODY));
    }
    return new LinkedHashSet<>(parameterScopes);
  }

  @Override
  public List<String> getSqlScriptSegments() {
    return sqlScriptsSegments;
  }

  @Override
  public boolean unwrapArray() {
    return unwrapArray;
  }

  @Override
  public Optional<JsonSchema> getParameterValidation() {
    if (parameterValidation == null) {
      return Optional.empty();
    }
    JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
    return Optional.of(factory.getSchema(parameterValidation));
  }
}
