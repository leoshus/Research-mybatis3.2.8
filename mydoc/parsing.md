`parsing`顾名思义：就是解析的意思。`org.apache.ibatis.parsing`包下的类具体完成了对于MyBatis的配置文件的解析工作。

*使用MyBatis之前我们会先加载MyBatis的配置文件:

```
InputStream is = Resources.getResourceAsStream("com/sdw/soft/test/showcase/mybatis-config.xml");
SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(is);
```
从SqlSessionFactoryBuilder.build()方法中我们可以看到MyBatis是利用了XMLConfigBuilder来对其配置文件进行解析的。
```
/**
   * 比较常用的方法
   * @param reader 以字符流读入配置文件
   * @param environment 决定使用哪个环境(测试、开发、生产)  包括了数据源 和 事务管理器的配置
   * @param properties 方法参数properties，配置文件中仍然可以使用${} 来为properties中的值占位
   * @return
   */
  public SqlSessionFactory build(Reader reader, String environment, Properties properties) {
    try {
    //使用XMLConfigBuilder解析MyBatis配置文件
      XMLConfigBuilder parser = new XMLConfigBuilder(reader, environment, properties);
      return build(parser.parse());
    } catch (Exception e) {
    	//这里捕获异常 并包装成自己的异常
      throw ExceptionFactory.wrapException("Error building SqlSession.", e);
    } finally {
      ErrorContext.instance().reset();
      try {
        reader.close();
      } catch (IOException e) {
        // Intentionally ignore. Prefer previous error.
      }
    }
  }
```
那么,先让我们来认识一下`org.apache.ibatis.parsing`包下的类吧。
