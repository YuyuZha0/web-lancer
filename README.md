### mybatis-jackson

```xml
<dependency>
    <groupId>com.tencent</groupId>
    <artifactId>mybatis-jackson</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```


#### Usages


```java
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.ibatis.annotations.Select;

/**
 * @author fishzhao
 * @since 2020-12-28
 */
public interface StudentMapper {

  @Select("select id, name from t_student")
  ArrayNode selectStudentList();

  @Select("select * from t_student where id=#{test[0].id} limit 1")
  ObjectNode selectStudent(JsonNode param);
}
```

```java
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
            "jdbc:mysql://127.0.0.1:3306/test?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=UTF8&zeroDateTimeBehavior=convertToNull&useSSL=true",
            "user",
            "******");
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
```