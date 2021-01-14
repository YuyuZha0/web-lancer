package com.tencent.weblancer.mybatis.jackson.typehandlers;

import com.fasterxml.jackson.databind.node.BooleanNode;
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
@MappedJdbcTypes({JdbcType.BIT, JdbcType.BOOLEAN})
public final class BooleanNodeTypeHandler extends BaseJacksonTypeHandler<BooleanNode> {

  @Override
  void setNonNullParameter(PreparedStatement ps, int i, BooleanNode parameter, JdbcType jdbcType)
      throws SQLException {
    ps.setBoolean(i, parameter.asBoolean());
  }

  @Override
  BooleanNode getNullableResult(ResultSet rs, String columnName) throws SQLException {
    boolean result = rs.getBoolean(columnName);
    return !result && rs.wasNull() ? null : BooleanNode.valueOf(result);
  }

  @Override
  BooleanNode getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    boolean result = rs.getBoolean(columnIndex);
    return !result && rs.wasNull() ? null : BooleanNode.valueOf(result);
  }

  @Override
  BooleanNode getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
    boolean result = cs.getBoolean(columnIndex);
    return !result && cs.wasNull() ? null : BooleanNode.valueOf(result);
  }
}
