package com.tencent.weblancer.external.typehandlers;

import com.fasterxml.jackson.databind.node.DecimalNode;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author fishzhao
 * @since 2020-12-25
 */
@MappedJdbcTypes({JdbcType.REAL, JdbcType.DECIMAL, JdbcType.NUMERIC})
public final class DecimalNodeTypeHandler extends BaseJacksonTypeHandler<DecimalNode> {

  private DecimalNode decimalNodeOrNull(BigDecimal decimal) {
    if (decimal != null) {
      return DecimalNode.valueOf(decimal);
    }
    return null;
  }

  @Override
  void setNonNullParameter(PreparedStatement ps, int i, DecimalNode parameter, JdbcType jdbcType)
      throws SQLException {
    ps.setBigDecimal(i, new BigDecimal(parameter.asText()));
  }

  @Override
  DecimalNode getNullableResult(ResultSet rs, String columnName) throws SQLException {
    return decimalNodeOrNull(rs.getBigDecimal(columnName));
  }

  @Override
  DecimalNode getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    return decimalNodeOrNull(rs.getBigDecimal(columnIndex));
  }

  @Override
  DecimalNode getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
    return decimalNodeOrNull(cs.getBigDecimal(columnIndex));
  }
}
