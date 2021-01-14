package com.tencent.weblancer.mybatis.jackson.meta;

import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.ibatis.reflection.factory.DefaultObjectFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * @author fishzhao
 * @since 2020-12-28
 */
public final class JsonNodeObjectFactory extends DefaultObjectFactory {

  private final Properties properties = new Properties();
  private final Set<Class<? extends TreeNode>> jsonTypes =
      new HashSet<>(
          Arrays.asList(
              TreeNode.class,
              JsonNode.class,
              ContainerNode.class,
              ArrayNode.class,
              ObjectNode.class));
  private final JsonNodeFactory jsonNodeFactory;

  public JsonNodeObjectFactory(ObjectMapper objectMapper) {
    this.jsonNodeFactory = objectMapper.getNodeFactory();
  }

  @Override
  public void setProperties(Properties properties) {
    if (properties != null) {
      this.properties.putAll(properties);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T create(
      Class<T> type, List<Class<?>> constructorArgTypes, List<Object> constructorArgs) {
    if (jsonTypes.contains(type) && (constructorArgTypes == null || constructorArgs == null)) {
      if (type == ArrayNode.class) {
        // a dirty solution
        // &&
        // Arrays.stream(Thread.currentThread().getStackTrace()).map(StackTraceElement::getMethodName).anyMatch("convertToDeclaredCollection"::equals)) {
        return (T) jsonNodeFactory.arrayNode();
      }
      return (T) jsonNodeFactory.objectNode();
    }
    return super.create(type, constructorArgTypes, constructorArgs);
  }

  @Override
  public <T> boolean isCollection(Class<T> type) {
    return ArrayNode.class.isAssignableFrom(type) || super.isCollection(type);
  }
}
