/*
 *    Copyright 2009-2012 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.session;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Properties;

import org.apache.ibatis.builder.xml.XMLConfigBuilder;
import org.apache.ibatis.exceptions.ExceptionFactory;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.session.defaults.DefaultSqlSessionFactory;

/*
 * Builds {@link SqlSession} instances.
 *
 */
/**
 * @author Clinton Begin
 * @desription 构建SqlSessionFactory  工厂模式
 */
public class SqlSessionFactoryBuilder {

  //以字符流的形式读入配置文件
  public SqlSessionFactory build(Reader reader) {
    return build(reader, null, null);
  }

  public SqlSessionFactory build(Reader reader, String environment) {
    return build(reader, environment, null);
  }
  //作为方法参数的properties > 资源/url中的properties > properties元素的子元素<property>
  public SqlSessionFactory build(Reader reader, Properties properties) {
    return build(reader, null, properties);
  }

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

  public SqlSessionFactory build(InputStream inputStream) {
    return build(inputStream, null, null);
  }

  public SqlSessionFactory build(InputStream inputStream, String environment) {
    return build(inputStream, environment, null);
  }

  public SqlSessionFactory build(InputStream inputStream, Properties properties) {
    return build(inputStream, null, properties);
  }

  public SqlSessionFactory build(InputStream inputStream, String environment, Properties properties) {
    try {
      XMLConfigBuilder parser = new XMLConfigBuilder(inputStream, environment, properties);
      return build(parser.parse());
    } catch (Exception e) {
      throw ExceptionFactory.wrapException("Error building SqlSession.", e);
    } finally {
      ErrorContext.instance().reset();
      try {
        inputStream.close();
      } catch (IOException e) {
        // Intentionally ignore. Prefer previous error.
      }
    }
  }
    
  public SqlSessionFactory build(Configuration config) {
    return new DefaultSqlSessionFactory(config);
  }

}
