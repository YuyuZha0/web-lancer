package com.tencent.mybatis.jackson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BinaryNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DecimalNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.FloatNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ShortNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.tencent.mybatis.jackson.meta.JsonNodeObjectFactory;
import com.tencent.mybatis.jackson.meta.JsonNodeObjectWrapperFactory;
import com.tencent.mybatis.jackson.meta.JsonNodeReflectorFactory;
import com.tencent.mybatis.jackson.typehandlers.BinaryNodeTypeHandler;
import com.tencent.mybatis.jackson.typehandlers.BooleanNodeTypeHandler;
import com.tencent.mybatis.jackson.typehandlers.DecimalNodeTypeHandler;
import com.tencent.mybatis.jackson.typehandlers.DoubleNodeTypeHandler;
import com.tencent.mybatis.jackson.typehandlers.FloatNodeTypeHandler;
import com.tencent.mybatis.jackson.typehandlers.IntNodeTypeHandler;
import com.tencent.mybatis.jackson.typehandlers.LongNodeTypeHandler;
import com.tencent.mybatis.jackson.typehandlers.ShortNodeTypeHandler;
import com.tencent.mybatis.jackson.typehandlers.TextNodeTypeHandler;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.TypeAliasRegistry;
import org.apache.ibatis.type.TypeHandlerRegistry;

import java.util.Objects;

/**
 * @author fishzhao
 * @since 2020-12-30
 */
public final class JacksonBindingConfiguration extends Configuration {

  private final ObjectMapper objectMapper;

  public JacksonBindingConfiguration(Environment environment, ObjectMapper objectMapper) {
    super(environment);
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    initializeJsonMapping();
  }

  public JacksonBindingConfiguration(Environment environment) {
    this(environment, new ObjectMapper());
  }

  public JacksonBindingConfiguration(ObjectMapper objectMapper) {
    super();
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    initializeJsonMapping();
  }

  public JacksonBindingConfiguration() {
    this(new ObjectMapper());
  }

  private void initializeJsonMapping() {
    registerJacksonTypeHandlers();
    registerTypeAlias();
    setObjectWrapperFactory(new JsonNodeObjectWrapperFactory());
    setObjectFactory(new JsonNodeObjectFactory(objectMapper));
    setReflectorFactory(new JsonNodeReflectorFactory());
  }

  public ObjectMapper getObjectMapper() {
    return objectMapper;
  }

  private void registerJacksonTypeHandlers() {
    TypeHandlerRegistry registry = getTypeHandlerRegistry();
    new BinaryNodeTypeHandler().registerTo(registry);
    new BooleanNodeTypeHandler().registerTo(registry);
    new DecimalNodeTypeHandler().registerTo(registry);
    new DoubleNodeTypeHandler().registerTo(registry);
    new FloatNodeTypeHandler().registerTo(registry);
    new IntNodeTypeHandler().registerTo(registry);
    new LongNodeTypeHandler().registerTo(registry);
    new ShortNodeTypeHandler().registerTo(registry);
    new TextNodeTypeHandler().registerTo(registry);
  }

  private void registerTypeAlias() {
    TypeAliasRegistry registry = getTypeAliasRegistry();
    registry.registerAlias("JSON_NODE", JsonNode.class);
    registry.registerAlias("OBJECT_NODE", ObjectNode.class);
    registry.registerAlias("ARRAY_NODE", ArrayNode.class);
    registry.registerAlias("INT_NODE", IntNode.class);
    registry.registerAlias("TEXT_NODE", TextNode.class);
    registry.registerAlias("LONG_NODE", LongNode.class);
    registry.registerAlias("SHORT_NODE", ShortNode.class);
    registry.registerAlias("FLOAT_NODE", FloatNode.class);
    registry.registerAlias("DOUBLE_NODE", DoubleNode.class);
    registry.registerAlias("DECIMAL_NODE", DecimalNode.class);
    registry.registerAlias("BOOLEAN_NODE", BooleanNode.class);
    registry.registerAlias("BINARY_NODE", BinaryNode.class);
  }

  @Override
  public void addResultMap(ResultMap rm) {
    // dirty solution for arrayNode result handle problem
    if (ArrayNode.class.isAssignableFrom(rm.getType())) {
      rm =
          new ResultMap.Builder(this, rm.getId(), ObjectNode.class, rm.getResultMappings()).build();
    }
    super.addResultMap(rm);
  }
}
