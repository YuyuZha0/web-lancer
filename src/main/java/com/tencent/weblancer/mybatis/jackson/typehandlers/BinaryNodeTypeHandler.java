package com.tencent.weblancer.mybatis.jackson.typehandlers;

import com.fasterxml.jackson.databind.node.BinaryNode;
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
@MappedJdbcTypes({JdbcType.BLOB, JdbcType.LONGVARBINARY})
public final class BinaryNodeTypeHandler extends BaseJacksonTypeHandler<BinaryNode> {


  private BinaryNode bytesNodeOrNull(byte[] bytes) {
    return BinaryNode.valueOf(bytes);
  }

  @Override
  void setNonNullParameter(PreparedStatement ps, int i, BinaryNode parameter, JdbcType jdbcType) throws SQLException {
    byte[] bytes = parameter.binaryValue();
    ps.setBytes(i, bytes);
  }

  @Override
  BinaryNode getNullableResult(ResultSet rs, String columnName) throws SQLException {
    return bytesNodeOrNull(rs.getBytes(columnName));
  }

  @Override
  BinaryNode getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    return bytesNodeOrNull(rs.getBytes(columnIndex));
  }

  @Override
  BinaryNode getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
    return bytesNodeOrNull(cs.getBytes(columnIndex));
  }
}
