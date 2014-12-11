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
* org.apache.ibatis.parsing.GenericTokenParser 用于对常用Token进行解析
```
//普通标识解析器 解析#{} ${}
public class GenericTokenParser {
  private final String openToken;//开始标识
  private final String closeToken;//结束标识
  private final TokenHandler handler;//token处理器
  //带参构造器 初始化属性
  public GenericTokenParser(String openToken, String closeToken, TokenHandler handler) {
    this.openToken = openToken;
    this.closeToken = closeToken;
    this.handler = handler;
  }
  /**
   * 其实就是将输入字符串中的以openToken和closeToken包裹的内容提取出来 交给 TokenHandler处理一下 
   * 然后再将处理结果拼装到原始字符串中
   * 详见org.apache.ibatis.parsing.GenericTokenParserTest
   * @param text
   * @return
   */
  public String parse(String text) {
    StringBuilder builder = new StringBuilder();
    if (text != null && text.length() > 0) {//如果传入的字符串有值
      char[] src = text.toCharArray();//将字符串转化成字符数组
      int offset = 0;
      int start = text.indexOf(openToken, offset);//传入字符串中是否存在openToken,若存在,返回位置
      while (start > -1) {
        if (start > 0 && src[start - 1] == '\\') {//判断openToken前是否存在转义字符'\'
          // the variable is escaped. remove the backslash.
        //如果openToken前存在转义字符，则当前的openToken不需要处理 对应的closeToken也不需要处理。接着查找下一个openToken
          builder.append(src, offset, start - offset - 1).append(openToken);
          offset = start + openToken.length();//重设offset值
        } else {
          int end = text.indexOf(closeToken, start);
          if (end == -1) {//若不存在closeToken，将剩余内容全部加入到builder中
            builder.append(src, offset, src.length - offset);
            offset = src.length;//重设offset
          } else {
            builder.append(src, offset, start - offset);
            offset = start + openToken.length();
            String content = new String(src, offset, end - offset);//取出openToken与closeToken包裹的内容
            builder.append(handler.handleToken(content));//调用TokenHandler处理取出的内容
            offset = end + closeToken.length();//重设offset
          }
        }
        start = text.indexOf(openToken, offset);//开始从offset开始查找下一个openToken 开启新的循环
      }
      //1.若最后一个closeToken后还有内容 ，将剩余内容追加到builder 
      //2.或者 输入字符串中压根就没有openToken
      if (offset < src.length) {
        builder.append(src, offset, src.length - offset);
      }
    }
    return builder.toString();
  }

}
```

* org.apache.ibatis.parsing.ParsingException 自定义解析异常

* org.apache.ibatis.parsing.PropertyParser 属性解析器 解析${}占位的属性值 也算是GenericTokenParser的完美应用吧

```
/**
 * 属性解析器 解析${}占位的属性
 * @author Clinton Begin
 */
public class PropertyParser {
  public static String parse(String string, Properties variables) {
    VariableTokenHandler handler = new VariableTokenHandler(variables);
    GenericTokenParser parser = new GenericTokenParser("${", "}", handler);
    return parser.parse(string);
  }
  private static class VariableTokenHandler implements TokenHandler {
    private Properties variables;
    public VariableTokenHandler(Properties variables) {
      this.variables = variables;
    }
    //通过key从properties文件中找到对应的value返回
    public String handleToken(String content) {
      if (variables != null && variables.containsKey(content)) {
        return variables.getProperty(content);
      }
      return "${" + content + "}";
    }
  }
}
```

* org.apache.ibatis.parsing.TokenHandler 没啥好说的 处理openToken和closeToken包裹内容的处理器接口 它的源码也很简单 就一个handle方法

```
public interface TokenHandler {
  String handleToken(String content);
}
```

* [org.apache.ibatis.parsing.XNode](https://github.com/sdw2330976/Research-mybatis3.2.8/blob/master/src/main/java/org/apache/ibatis/parsing/XNode.java)对于org.w3c.dom.Node的封装

* [org.apache.ibatis.parsing.XPathParser](https://github.com/sdw2330976/Research-mybatis3.2.8/blob/master/src/main/java/org/apache/ibatis/parsing/XPathParser.java)对于JDK XPath的封装