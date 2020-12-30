package com.tencent.mybatis.jackson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.ibatis.annotations.Select;

/**
 * @author fishzhao
 * @since 2020-12-28
 */
public interface StudentMapper {

  @Select("select id, name from t_student")
  ArrayNode selectStudentList();

  @Select("select * from t_student where id=#{test[0].id} limit 1")
  ObjectNode selectStudent(JsonNode param);
}
