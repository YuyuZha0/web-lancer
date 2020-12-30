package com.tencent.mybatis.jackson;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.Test;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * @author fishzhao
 * @since 2020-12-25
 */
public class JacksonTypeHandlerTest {

  @Test
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
    configuration.addMapper(StudentMapper.class);

    SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      StudentMapper studentMapper = sqlSession.getMapper(StudentMapper.class);
      ObjectNode param = configuration.getObjectMapper().createObjectNode();
      param.putArray("test").addObject().put("id", 1);
      System.out.println(studentMapper.selectStudent(param));
      Map<String, Object> map = new HashMap<>();
      Map<String, Object> map1 = new HashMap<>();
      map1.put("id", 1);
      map.put("test", map1);
      System.out.println(studentMapper.selectStudentList());
    }
  }
}
