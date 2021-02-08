package com.tencent.weblancer.external.meta;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.property.PropertyTokenizer;
import org.apache.ibatis.reflection.wrapper.ObjectWrapper;

import java.util.List;

/**
 * @author fishzhao
 * @since 2020-12-28
 */
public final class ArrayNodeWrapper implements ObjectWrapper {

  private final MetaObject metaObject;
  private final ArrayNode arrayNode;

  public ArrayNodeWrapper(MetaObject metaObject, ArrayNode arrayNode) {
    this.metaObject = metaObject;
    this.arrayNode = arrayNode;
  }

  @Override
  public Object get(PropertyTokenizer prop) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void set(PropertyTokenizer prop, Object value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String findProperty(String name, boolean useCamelCaseMapping) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String[] getGetterNames() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String[] getSetterNames() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Class<?> getSetterType(String name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Class<?> getGetterType(String name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean hasSetter(String name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean hasGetter(String name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public MetaObject instantiatePropertyValue(
      String name, PropertyTokenizer prop, ObjectFactory objectFactory) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isCollection() {
    return true;
  }

  @Override
  public void add(Object element) {
    add0(element);
  }

  @Override
  public <E> void addAll(List<E> element) {
    for (E e : element) {
      add0(e);
    }
  }

  private void add0(Object element) {
    if (element instanceof JsonNode) {
      arrayNode.add(((JsonNode) element));
    } else {
      arrayNode.add(arrayNode.pojoNode(element));
    }
  }
}
