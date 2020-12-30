package com.tencent.mybatis.jackson.typehandlers;

import com.fasterxml.jackson.databind.node.DoubleNode;
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
@MappedJdbcTypes(JdbcType.DOUBLE)
public final class DoubleNodeTypeHandler extends BaseJacksonTypeHandler<DoubleNode> {


  @Override
  void setNonNullParameter(PreparedStatement ps, int i, DoubleNode parameter, JdbcType jdbcType) throws SQLException {
    ps.setDouble(i, parameter.asDouble());
  }

  @Override
  DoubleNode getNullableResult(ResultSet rs, String columnName) throws SQLException {
    double result = rs.getDouble(columnName);
    return result == 0 && rs.wasNull() ? null : DoubleNode.valueOf(result);
  }

  @Override
  DoubleNode getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    double result = rs.getDouble(columnIndex);
    return result == 0 && rs.wasNull() ? null : DoubleNode.valueOf(result);
  }

  @Override
  DoubleNode getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
    double result = cs.getDouble(columnIndex);
    return result == 0 && cs.wasNull() ? null : DoubleNode.valueOf(result);
  }

}
