package com.tencent.weblancer.mybatis.jackson.typehandlers;

import com.fasterxml.jackson.databind.node.FloatNode;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author fishzhao
 * @since 2020-12-25
 */
@MappedJdbcTypes(JdbcType.FLOAT)
public final class FloatNodeTypeHandler extends BaseJacksonTypeHandler<FloatNode> {


  @Override
  void setNonNullParameter(PreparedStatement ps, int i, FloatNode parameter, JdbcType jdbcType) throws SQLException {
    ps.setFloat(i, (float) parameter.asDouble());
  }

  @Override
  FloatNode getNullableResult(ResultSet rs, String columnName) throws SQLException {
    float result = rs.getFloat(columnName);
    return result == 0 && rs.wasNull() ? null : FloatNode.valueOf(result);
  }

  @Override
  FloatNode getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    float result = rs.getInt(columnIndex);
    return result == 0 && rs.wasNull() ? null : FloatNode.valueOf(result);
  }

  @Override
  FloatNode getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
    float result = cs.getFloat(columnIndex);
    return result == 0 && cs.wasNull() ? null : FloatNode.valueOf(result);
  }

}
