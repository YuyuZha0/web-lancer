package com.tencent.weblancer.mybatis.jackson.meta;

import org.apache.ibatis.reflection.Reflector;
import org.apache.ibatis.reflection.ReflectorFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author fishzhao
 * @since 2020-12-29
 */
public class JsonNodeReflectorFactory implements ReflectorFactory {

  private final ConcurrentMap<Class<?>, Reflector> reflectorMap = new ConcurrentHashMap<>();
  private boolean classCacheEnabled = true;

  public JsonNodeReflectorFactory() {
  }

  @Override
  public boolean isClassCacheEnabled() {
    return classCacheEnabled;
  }

  @Override
  public void setClassCacheEnabled(boolean classCacheEnabled) {
    this.classCacheEnabled = classCacheEnabled;
  }

  @Override
  public Reflector findForClass(Class<?> type) {
    if (classCacheEnabled) {
      // synchronized (type) removed see issue #461
      return reflectorMap.computeIfAbsent(type, JsonNodeReflector::new);
    } else {
      return new JsonNodeReflector(type);
    }
  }

}
