package com.tencent.weblancer.mybatis.jackson.typehandlers;

import com.fasterxml.jackson.databind.node.TextNode;
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
@MappedJdbcTypes({
  JdbcType.CHAR,
  JdbcType.VARCHAR,
  JdbcType.DATE,
  JdbcType.TIME,
  JdbcType.TIMESTAMP
})
public final class TextNodeTypeHandler extends BaseJacksonTypeHandler<TextNode> {

  private TextNode textNodeOrNull(String s) {
    return TextNode.valueOf(s);
  }

  @Override
  void setNonNullParameter(PreparedStatement ps, int i, TextNode parameter, JdbcType jdbcType)
      throws SQLException {
    ps.setString(i, parameter.asText());
  }

  @Override
  TextNode getNullableResult(ResultSet rs, String columnName) throws SQLException {
    return textNodeOrNull(rs.getString(columnName));
  }

  @Override
  TextNode getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    return textNodeOrNull(rs.getString(columnIndex));
  }

  @Override
  TextNode getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
    return textNodeOrNull(cs.getString(columnIndex));
  }
}
