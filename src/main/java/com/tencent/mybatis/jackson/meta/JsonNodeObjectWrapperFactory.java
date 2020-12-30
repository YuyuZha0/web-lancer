package com.tencent.mybatis.jackson.meta;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.wrapper.ObjectWrapper;
import org.apache.ibatis.reflection.wrapper.ObjectWrapperFactory;

/**
 * @author fishzhao
 * @since 2020-12-28
 */
public final class JsonNodeObjectWrapperFactory implements ObjectWrapperFactory {

  @Override
  public boolean hasWrapperFor(Object object) {
    return ContainerNode.class.isAssignableFrom(object.getClass());
  }

  @Override
  public ObjectWrapper getWrapperFor(MetaObject metaObject, Object object) {
    if (object instanceof ObjectNode) {
      return new ObjectNodeWrapper(metaObject, ((ObjectNode) object));
    }
    if (object instanceof ArrayNode) {
      return new ArrayNodeWrapper(metaObject, (ArrayNode) object);
    }
    throw new IllegalStateException("impossible");
  }
}
