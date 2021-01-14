package com.tencent.weblancer.mybatis.jackson.typehandlers;

import com.fasterxml.jackson.databind.node.ShortNode;
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
@MappedJdbcTypes({JdbcType.TINYINT, JdbcType.SMALLINT})
public final class ShortNodeTypeHandler extends BaseJacksonTypeHandler<ShortNode> {

  @Override
  void setNonNullParameter(PreparedStatement ps, int i, ShortNode parameter, JdbcType jdbcType)
      throws SQLException {
    if (jdbcType == JdbcType.SMALLINT) {
      ps.setShort(i, (short) parameter.asInt());
    } else {
      ps.setByte(i, (byte) parameter.asInt());
    }
  }

  @Override
  ShortNode getNullableResult(ResultSet rs, String columnName) throws SQLException {
    short result = rs.getShort(columnName);
    return result == 0 && rs.wasNull() ? null : ShortNode.valueOf(result);
  }

  @Override
  ShortNode getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    short result = rs.getShort(columnIndex);
    return result == 0 && rs.wasNull() ? null : ShortNode.valueOf(result);
  }

  @Override
  ShortNode getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
    short result = cs.getShort(columnIndex);
    return result == 0 && cs.wasNull() ? null : ShortNode.valueOf(result);
  }
}
