### web-lancer

#### What is web-lancer?

**web-lancer is a lite framework that allows you to build web interfaces with the minimal work.**

#### Features

- The `sqlTemplate` syntax is inherited from `mybatis`, you can use it with nothing special.
- Use standard `jsonScmema` as parameter validation, forget the ugly parameter validation code.
- `Vert.x` based web application, you can gain all `NIO` benefits (however, `JDBC` with `executeBlocking`).
- The `web-lancer` framework is only for query use, so the update operation is not supported.

#### Simple Example


##### application.json

```json
{
  "serverPort": 8080, 
  "dataSources": [
    {
      "jdbcUrl": "jdbc:mysql://127.0.0.1:3306/common_data?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=UTF8&zeroDateTimeBehavior=convertToNull&useSSL=true",
      "username": "user",
      "password": "*****",
      "driverClassName": "com.mysql.cj.jdbc.Driver",
      "id": "default" // an explicted id is requied!
    }
  ],
  "interfaceDefinitionPath": [
    "/Users/fishzhao/IdeaProjects/web-lancer/conf/interface.json"
  ]
}
```

##### interface.json


```json
{
  "dataSourceId": "default",
  "httpMethods": [
    "GET",
    "POST"
  ],
  "uri": "/getKaInfo",
  "parameterValidation": {
    "type": "object",
    "properties": {
      "kaId": {
        "type": "integer"
      },
      "chanWxappScene": {
        "type": "array",
        "items": {
          "type": "integer"
        }
      }
    }
  },
  "parameterScopes": [
    "BODY",
    "QUERY_STRING"
  ],
  // is set to "true", the reseult data will be a json object instead of an array 
  "unwrapArray": false,
  "sqlScriptsSegments": [
    "<script>",
    "select * from channel_wxapp",
    "<where>",
    "<if test=\"kaId != null\">",
    "ka_id = #{kaId}",
    "</if>",
    "<if test=\"chanWxappScene != null\">",
    "AND chan_wxapp_scene in",
    "<foreach item=\"item\" index=\"index\" collection=\"chanWxappScene\" open=\"(\" separator=\",\" close=\")\">",
    "#{item}",
    "</foreach>",
    "</if>",
    "</where>",
    "limit 100",
    "</script>"
  ]
}
```

To start the web server, use the following command line:

```bash
java -jar web-lancer-0.0.1-SNAPSHOT.jar application.json 
```

then you can make a request to the server:

```bash
curl 127.0.0.1:8080/getKaInfo -X POST -d '{"kaId":1106}'
```