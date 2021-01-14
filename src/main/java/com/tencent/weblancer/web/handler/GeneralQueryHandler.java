package com.tencent.weblancer.web.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.ValidationMessage;
import com.networknt.schema.ValidationResult;
import com.tencent.weblancer.web.conf.ParameterScope;
import io.netty.buffer.ByteBufInputStream;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.RoutingContext;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author fishzhao
 * @since 2021-01-11
 */
@Slf4j
public final class GeneralQueryHandler implements Handler<RoutingContext> {

  private final LinkedHashSet<ParameterScope> parameterScopes;
  private final JsonSchema parameterValidation;
  private final SqlSessionFactory sqlSessionFactory;
  private final String stmtId;
  private final boolean unwrapArray;
  private final ObjectMapper objectMapper;

  public GeneralQueryHandler(
      @NonNull LinkedHashSet<ParameterScope> parameterScopes,
      JsonSchema parameterValidation,
      @NonNull SqlSessionFactory sqlSessionFactory,
      @NonNull String stmtId,
      boolean unwrapArray,
      @NonNull ObjectMapper objectMapper) {
    this.parameterScopes = parameterScopes;
    this.parameterValidation = parameterValidation;
    this.sqlSessionFactory = sqlSessionFactory;
    this.stmtId = stmtId;
    this.unwrapArray = unwrapArray;
    this.objectMapper = objectMapper;
  }

  private static void putMultiMap(ObjectNode parameters, MultiMap multiMap) {
    if (multiMap == null || multiMap.isEmpty()) {
      return;
    }
    for (String name : multiMap.names()) {
      List<String> values = multiMap.getAll(name);
      if (values.isEmpty()) {
        continue;
      }
      if (values.size() == 1) {
        String value = StringUtils.trimToEmpty(values.get(0));
        if (!value.isEmpty()) {
          parameters.put(name, value);
        }
        continue;
      }
      for (String value : values) {
        value = StringUtils.trimToEmpty(values.get(0));
        if (!value.isEmpty()) {
          JsonNode arrayNode = parameters.get(name);
          if (arrayNode == null || !arrayNode.isArray()) {
            arrayNode = parameters.putArray(name);
          }
          ((ArrayNode) arrayNode).add(value);
        }
      }
    }
  }

  @Override
  public void handle(RoutingContext routingContext) {
    ObjectNode params;
    try {
      params = resolveParameters(routingContext);
    } catch (Exception e) {
      ObjectNode resultWrapper =
          QueryResult.PARAMETER_RESOLVE_FAIL
              .createResultObject(objectMapper)
              .put("cause", e.getMessage());
      endWithJson(routingContext, resultWrapper);
      return;
    }

    if (parameterValidation != null) {
      ValidationResult validationResult = parameterValidation.validateAndCollect(params);
      Set<ValidationMessage> validationMessages = validationResult.getValidationMessages();
      if (validationMessages != null && !validationMessages.isEmpty()) {
        ObjectNode resultWrapper =
            QueryResult.PARAMETER_VALIDATION_FAIL.createResultObject(objectMapper);
        for (ValidationMessage validationMessage : validationMessages) {
          resultWrapper.withArray("details").add(validationMessage.getMessage());
        }
        endWithJson(routingContext, resultWrapper);
        return;
      }
    }

    routingContext
        .vertx()
        .executeBlocking(
            promise -> {
              ObjectNode resultWrapper;
              try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
                ArrayNodeHandler handler = new ArrayNodeHandler(objectMapper.createArrayNode());
                sqlSession.select(stmtId, params, handler);
                ArrayNode arrayNode = handler.getArrayNode();
                resultWrapper = wrapDbResult(arrayNode);
              } catch (Exception e) {
                log.error("Access DB with exception: ", e);
                resultWrapper = QueryResult.UNKNOWN_EXCEPTION.createResultObject(objectMapper);
                resultWrapper.put("cause", Throwables.getRootCause(e).getMessage());
              }
              endWithJson(routingContext, resultWrapper);
              promise.complete();
            },
            false);
  }

  private ObjectNode wrapDbResult(ArrayNode arrayNode) {
    if (unwrapArray) {
      if (arrayNode.isEmpty()) {
        return QueryResult.OK.createResultObject(objectMapper).putNull("data");
      }
      if (arrayNode.size() == 1) {
        return QueryResult.OK.createResultObject(objectMapper).set("data", arrayNode.get(0));
      }
      return QueryResult.MULTIPLE_RESULT_OBJECTS.createResultObject(objectMapper);
    }
    return QueryResult.OK.createResultObject(objectMapper).set("data", arrayNode);
  }

  private void endWithJson(RoutingContext context, JsonNode jsonNode) {
    Buffer buffer;
    try {
      buffer = Buffer.buffer(objectMapper.writeValueAsBytes(jsonNode));
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
    context
        .response()
        .setStatusCode(200)
        .putHeader("content-type", "application/json;charset=utf-8")
        .end(buffer);
  }

  private ObjectNode resolveParameters(RoutingContext context) throws IOException {
    ObjectNode parameters = objectMapper.createObjectNode();
    for (ParameterScope scope : parameterScopes) {
      if (scope == ParameterScope.QUERY_STRING) {
        putMultiMap(parameters, context.queryParams(StandardCharsets.UTF_8));
      }
      if (scope == ParameterScope.BODY) {
        Buffer buffer = context.getBody();
        if (buffer == null) {
          continue;
        }
        try (InputStream in = new ByteBufInputStream(buffer.getByteBuf())) {
          JsonNode bodyNode = objectMapper.readTree(in);
          Preconditions.checkArgument(bodyNode.isObject(), "body is excepted to be a json object!");
          parameters.setAll(((ObjectNode) bodyNode));
        }
      }
    }
    return parameters;
  }

  @RequiredArgsConstructor
  private static final class ArrayNodeHandler implements ResultHandler<JsonNode> {

    @Getter private final ArrayNode arrayNode;

    @Override
    public void handleResult(ResultContext<? extends JsonNode> resultContext) {
      arrayNode.add(resultContext.getResultObject());
    }
  }
}
