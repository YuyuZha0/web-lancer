package com.tencent.weblancer.mybatis.jackson;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.StatementType;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.Ignore;
import org.junit.Test;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Collections;

/**
 * @author fishzhao
 * @since 2020-12-25
 */
public class JacksonTypeHandlerTest {

  @Test
  @Ignore
  public void test() {
    DataSource dataSource =
        new UnpooledDataSource(
            "com.mysql.cj.jdbc.Driver",
            "jdbc:mysql://9.134.75.60:3306/test?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=UTF8&zeroDateTimeBehavior=convertToNull&useSSL=true",
            "root",
            "ki2rt&O?SUlO");
    Environment environment = new Environment("test", new JdbcTransactionFactory(), dataSource);
    JacksonBindingConfiguration configuration = new JacksonBindingConfiguration();
    configuration.setEnvironment(environment);
    // configuration.addMapper(StudentMapper.class);
    configuration.addMappedStatement(
        new MappedStatement.Builder(
                configuration,
                "1",
                configuration
                    .getLanguageDriver(null)
                    .createSqlSource(
                        configuration,
                        "select name from t_student where id=#{id}  or id=2",
                        ObjectNode.class),
                SqlCommandType.SELECT)
            .statementType(StatementType.PREPARED)
            // .resultSetType(ResultSetType.DEFAULT)
            // .parameterMap(new ParameterMap.Builder(configuration,"1",ObjectNode.class, new
            // ArrayList<>()).build())
            .resultMaps(
                Collections.singletonList(
                    new ResultMap.Builder(configuration, "1", ObjectNode.class, new ArrayList<>())
                        .build()))
            .build());

    SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      sqlSession.select(
          "1",
          configuration.getObjectMapper().createObjectNode().put("id", 1),
          new ResultHandler() {
            @Override
            public void handleResult(ResultContext resultContext) {
              // System.out.println(resultContext.getResultCount());
              System.out.println(resultContext.getResultObject().getClass());
            }
          });
    }
  }
}
