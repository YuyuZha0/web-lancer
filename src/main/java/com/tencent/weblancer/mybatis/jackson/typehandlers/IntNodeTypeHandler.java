package com.tencent.weblancer.mybatis.jackson.typehandlers;

import com.fasterxml.jackson.databind.node.IntNode;
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
@MappedJdbcTypes({JdbcType.INTEGER})
public final class IntNodeTypeHandler extends BaseJacksonTypeHandler<IntNode> {

  @Override
  void setNonNullParameter(PreparedStatement ps, int i, IntNode parameter, JdbcType jdbcType) throws SQLException {
    ps.setInt(i, parameter.asInt());
  }

  @Override
  IntNode getNullableResult(ResultSet rs, String columnName) throws SQLException {
    int result = rs.getInt(columnName);
    return result == 0 && rs.wasNull() ? null : IntNode.valueOf(result);
  }

  @Override
  IntNode getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    int result = rs.getInt(columnIndex);
    return result == 0 && rs.wasNull() ? null : IntNode.valueOf(result);
  }

  @Override
  IntNode getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
    int result = cs.getInt(columnIndex);
    return result == 0 && cs.wasNull() ? null : IntNode.valueOf(result);
  }

}
