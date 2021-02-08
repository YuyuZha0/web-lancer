package com.tencent.weblancer.external.meta;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.ibatis.reflection.Reflector;

/**
 * @author fishzhao
 * @since 2020-12-29
 */
public final class JsonNodeReflector extends Reflector {

  public JsonNodeReflector(Class<?> clazz) {
    super(clazz);
  }

  @Override
  public boolean hasDefaultConstructor() {
    return super.hasDefaultConstructor() || JsonNode.class.isAssignableFrom(super.getType());
  }
}
