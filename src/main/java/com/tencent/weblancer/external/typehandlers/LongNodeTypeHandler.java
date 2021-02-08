package com.tencent.weblancer.external.typehandlers;

import com.fasterxml.jackson.databind.node.LongNode;
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
@MappedJdbcTypes(JdbcType.BIGINT)
public final class LongNodeTypeHandler extends BaseJacksonTypeHandler<LongNode> {

  @Override
  void setNonNullParameter(PreparedStatement ps, int i, LongNode parameter, JdbcType jdbcType)
      throws SQLException {
    ps.setLong(i, parameter.asLong());
  }

  @Override
  LongNode getNullableResult(ResultSet rs, String columnName) throws SQLException {
    long result = rs.getLong(columnName);
    return result == 0 && rs.wasNull() ? null : LongNode.valueOf(result);
  }

  @Override
  LongNode getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    long result = rs.getLong(columnIndex);
    return result == 0 && rs.wasNull() ? null : LongNode.valueOf(result);
  }

  @Override
  LongNode getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
    long result = cs.getLong(columnIndex);
    return result == 0 && cs.wasNull() ? null : LongNode.valueOf(result);
  }
}
