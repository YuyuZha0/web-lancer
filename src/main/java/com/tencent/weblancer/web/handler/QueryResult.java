package com.tencent.weblancer.web.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public enum QueryResult {
  OK(0, "ok"),
  PARAMETER_RESOLVE_FAIL(1001, "parameter resolve failed!"),
  PARAMETER_VALIDATION_FAIL(1002, "parameter validation failed!"),
  MULTIPLE_RESULT_OBJECTS(1003, "multiply results found for single value query!"),
  UNKNOWN_EXCEPTION(9999, "unknown exception!");
  public final int code;
  public final String msg;

  QueryResult(int code, String msg) {
    this.code = code;
    this.msg = msg;
  }

  ObjectNode createResultObject(ObjectMapper objectMapper) {
    ObjectNode objectNode = objectMapper.createObjectNode();
    objectNode.put("code", code);
    objectNode.put("msg", msg);
    return objectNode;
  }
}
