package com.tencent.weblancer.mybatis.jackson.meta;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.property.PropertyTokenizer;
import org.apache.ibatis.reflection.wrapper.ObjectWrapper;

import java.util.List;

/**
 * @author fishzhao
 * @since 2020-12-28
 */
public final class ObjectNodeWrapper implements ObjectWrapper {

  private final MetaObject metaObject;
  private final ObjectNode objectNode;

  public ObjectNodeWrapper(MetaObject metaObject, ObjectNode objectNode) {
    this.metaObject = metaObject;
    this.objectNode = objectNode;
  }

  @Override
  public Object get(PropertyTokenizer prop) {
    String name = prop.getName();
    String index = prop.getIndex();
    if (index != null) {
      return getArrayValue(name, index);
    }
    return objectNode.get(name);
  }

  private Object getArrayValue(String name, String index) {
    if (name.isEmpty()) {
      return objectNode;
    }
    JsonNode array = objectNode.get(name);
    if (array == null) {
      return null;
    }
    if (array.isArray()) {
      return array.get(Integer.parseInt(index));
    }
    return array.get(index);
  }

  @Override
  public void set(PropertyTokenizer prop, Object value) {
    String name = prop.getName();
    String index = prop.getIndex();
    if (index != null) {
      setArrayValue(name, index, (JsonNode) value);
      return;
    }
    objectNode.set(name, ((JsonNode) value));
  }

  private void setArrayValue(String name, String index, JsonNode value) {
    if (name.isEmpty()) {
      objectNode.set(index, value);
      return;
    }
    JsonNode array = objectNode.get(name);
    int nIndex = Integer.parseInt(index);
    if (array == null) {
      objectNode.putArray(name).set(nIndex, value);
      return;
    }
    if (array.isArray()) {
      ((ArrayNode) array).set(nIndex, value);
      return;
    }
    if (array.isObject()) {
      ((ObjectNode) array).set(index, value);
      return;
    }
    throw new IllegalArgumentException(
        "illegal json type: " + array.getNodeType() + " for index " + index);
  }

  @Override
  public String findProperty(String name, boolean useCamelCaseMapping) {
    return name;
  }

  @Override
  public String[] getGetterNames() {
    return Lists.newArrayList(objectNode.fieldNames()).toArray(new String[0]);
  }

  @Override
  public String[] getSetterNames() {
    return getGetterNames();
  }

  @Override
  public Class<?> getSetterType(String name) {
    //    PropertyTokenizer prop = new PropertyTokenizer(name);
    //    if (prop.hasNext()) {
    //      MetaObject metaValue = metaObject.metaObjectForProperty(prop.getIndexedName());
    //      if (metaValue == SystemMetaObject.NULL_META_OBJECT) {
    //        return JsonNode.class;
    //      } else {
    //        return metaValue.getSetterType(prop.getChildren());
    //      }
    //    } else {
    //      JsonNode child = objectNode.get(name);
    //      if (child != null) {
    //        return child.getClass();
    //      } else {
    //        return JsonNode.class;
    //      }
    //    }
    return JsonNode.class;
  }

  @Override
  public Class<?> getGetterType(String name) {
    //    PropertyTokenizer prop = new PropertyTokenizer(name);
    //    if (prop.hasNext()) {
    //      MetaObject metaValue = metaObject.metaObjectForProperty(prop.getIndexedName());
    //      if (metaValue == SystemMetaObject.NULL_META_OBJECT) {
    //        return JsonNode.class;
    //      } else {
    //        return metaValue.getGetterType(prop.getChildren());
    //      }
    //    } else {
    //      JsonNode child = objectNode.get(name);
    //      if (child != null) {
    //        return child.getClass();
    //      } else {
    //        return JsonNode.class;
    //      }
    //    }
    return JsonNode.class;
  }

  @Override
  public boolean hasSetter(String name) {
    return true;
  }

  @Override
  public boolean hasGetter(String name) {
    PropertyTokenizer prop = new PropertyTokenizer(name);
    if (prop.hasNext()) {
      if (objectNode.hasNonNull(prop.getIndexedName())) {
        MetaObject metaValue = metaObject.metaObjectForProperty(prop.getIndexedName());
        if (metaValue == SystemMetaObject.NULL_META_OBJECT) {
          return true;
        } else {
          return metaValue.hasGetter(prop.getChildren());
        }
      } else {
        return false;
      }
    } else {
      return objectNode.hasNonNull(prop.getName());
    }
  }

  @Override
  public MetaObject instantiatePropertyValue(
      String name, PropertyTokenizer prop, ObjectFactory objectFactory) {
    ObjectNode node = objectNode.objectNode();
    set(prop, node);
    return MetaObject.forObject(
        node,
        metaObject.getObjectFactory(),
        metaObject.getObjectWrapperFactory(),
        metaObject.getReflectorFactory());
  }

  @Override
  public boolean isCollection() {
    return false;
  }

  @Override
  public void add(Object element) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <E> void addAll(List<E> element) {
    throw new UnsupportedOperationException();
  }
}
