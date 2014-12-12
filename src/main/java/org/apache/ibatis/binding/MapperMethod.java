/*
 *    Copyright 2009-2013 the original author or authors.
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
package org.apache.ibatis.binding;

import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.*;

/**
 * @author Clinton Begin
 * @author Eduardo Macarron
 * @author Lasse Voss
 */
public class MapperMethod {

  private final SqlCommand command;
  private final MethodSignature method;

  public MapperMethod(Class<?> mapperInterface, Method method, Configuration config) {
    this.command = new SqlCommand(config, mapperInterface, method);
    this.method = new MethodSignature(config, method);
  }

  /**
   * 执行的4中情况 insert|update|delete|select
   * @param sqlSession
   * @param args
   * @return
   */
  public Object execute(SqlSession sqlSession, Object[] args) {
    Object result;
    if (SqlCommandType.INSERT == command.getType()) {
      Object param = method.convertArgsToSqlCommandParam(args);
      result = rowCountResult(sqlSession.insert(command.getName(), param));
    } else if (SqlCommandType.UPDATE == command.getType()) {
      Object param = method.convertArgsToSqlCommandParam(args);
      result = rowCountResult(sqlSession.update(command.getName(), param));
    } else if (SqlCommandType.DELETE == command.getType()) {
      Object param = method.convertArgsToSqlCommandParam(args);
      result = rowCountResult(sqlSession.delete(command.getName(), param));
    } else if (SqlCommandType.SELECT == command.getType()) {
      if (method.returnsVoid() && method.hasResultHandler()) {
    	//如果存在结果处理器
        executeWithResultHandler(sqlSession, args);
        result = null;
      } else if (method.returnsMany()) {//如果结果有多条记录
        result = executeForMany(sqlSession, args);
      } else if (method.returnsMap()) {//如果结果为Map
        result = executeForMap(sqlSession, args);
      } else {//结果为一条记录
        Object param = method.convertArgsToSqlCommandParam(args);
        result = sqlSession.selectOne(command.getName(), param);
      }
    } else {
      throw new BindingException("Unknown execution method for: " + command.getName());
    }
    if (result == null && method.getReturnType().isPrimitive() && !method.returnsVoid()) {
      throw new BindingException("Mapper method '" + command.getName() 
          + " attempted to return null from a method with a primitive return type (" + method.getReturnType() + ").");
    }
    return result;
  }

  /**
   * 对方法返回值的类型进行一些检查，使得更安全
   * @param rowCount
   * @return
   */
  private Object rowCountResult(int rowCount) {
    final Object result;
    if (method.returnsVoid()) {
      result = null;
    } else if (Integer.class.equals(method.getReturnType()) || Integer.TYPE.equals(method.getReturnType())) {//返回值为大int或小int
      result = rowCount;
    } else if (Long.class.equals(method.getReturnType()) || Long.TYPE.equals(method.getReturnType())) {//返回值为大long或小long
      result = (long) rowCount;
    } else if (Boolean.class.equals(method.getReturnType()) || Boolean.TYPE.equals(method.getReturnType())) {//返回值为大boolean或小boolean
      result = (rowCount > 0);
    } else {
      throw new BindingException("Mapper method '" + command.getName() + "' has an unsupported return type: " + method.getReturnType());
    }
    return result;
  }

  /**
   * 结果处理器
   * @param sqlSession
   * @param args
   */
  private void executeWithResultHandler(SqlSession sqlSession, Object[] args) {
    MappedStatement ms = sqlSession.getConfiguration().getMappedStatement(command.getName());
    if (void.class.equals(ms.getResultMaps().get(0).getType())) {
      throw new BindingException("method " + command.getName() 
          + " needs either a @ResultMap annotation, a @ResultType annotation," 
          + " or a resultType attribute in XML so a ResultHandler can be used as a parameter.");
    }
    Object param = method.convertArgsToSqlCommandParam(args);
    if (method.hasRowBounds()) {
      RowBounds rowBounds = method.extractRowBounds(args);
      sqlSession.select(command.getName(), param, rowBounds, method.extractResultHandler(args));
    } else {
      sqlSession.select(command.getName(), param, method.extractResultHandler(args));
    }
  }

  /**
   * 多条记录
   * @param sqlSession
   * @param args
   * @return
   */
  private <E> Object executeForMany(SqlSession sqlSession, Object[] args) {
    List<E> result;
    Object param = method.convertArgsToSqlCommandParam(args);
    //带入RowBounds
    if (method.hasRowBounds()) {
      RowBounds rowBounds = method.extractRowBounds(args);
      result = sqlSession.<E>selectList(command.getName(), param, rowBounds);
    } else {
      result = sqlSession.<E>selectList(command.getName(), param);
    }
    // issue #510 Collections & arrays support
    if (!method.getReturnType().isAssignableFrom(result.getClass())) {
      if (method.getReturnType().isArray()) {
        return convertToArray(result);
      } else {
        return convertToDeclaredCollection(sqlSession.getConfiguration(), result);
      }
    }
    return result;
  }

  private <E> Object convertToDeclaredCollection(Configuration config, List<E> list) {
    Object collection = config.getObjectFactory().create(method.getReturnType());
    MetaObject metaObject = config.newMetaObject(collection);
    metaObject.addAll(list);
    return collection;
  }

  @SuppressWarnings("unchecked")
  private <E> E[] convertToArray(List<E> list) {
    E[] array = (E[]) Array.newInstance(method.getReturnType().getComponentType(), list.size());
    array = list.toArray(array);
    return array;
  }

  private <K, V> Map<K, V> executeForMap(SqlSession sqlSession, Object[] args) {
    Map<K, V> result;
    Object param = method.convertArgsToSqlCommandParam(args);
    if (method.hasRowBounds()) {
      RowBounds rowBounds = method.extractRowBounds(args);
      result = sqlSession.<K, V>selectMap(command.getName(), param, method.getMapKey(), rowBounds);
    } else {
      result = sqlSession.<K, V>selectMap(command.getName(), param, method.getMapKey());
    }
    return result;
  }

  //自定义一个自己的Map 有着更加严格的get方法 若没有对应的key 就会报错
  public static class ParamMap<V> extends HashMap<String, V> {

    private static final long serialVersionUID = -2212268410512043556L;

    @Override
    public V get(Object key) {
      if (!super.containsKey(key)) {
        throw new BindingException("Parameter '" + key + "' not found. Available parameters are " + keySet());
      }
      return super.get(key);
    }

  }

  //SQL命令
  public static class SqlCommand {

    private final String name;
    private final SqlCommandType type;

    public SqlCommand(Configuration configuration, Class<?> mapperInterface, Method method) throws BindingException {
      String statementName = mapperInterface.getName() + "." + method.getName();
      MappedStatement ms = null;
      if (configuration.hasStatement(statementName)) {
        ms = configuration.getMappedStatement(statementName);
      } else if (!mapperInterface.equals(method.getDeclaringClass().getName())) { // issue #35
    	//若不是这个Mapper接口的方法 再去查父类
        String parentStatementName = method.getDeclaringClass().getName() + "." + method.getName();
        if (configuration.hasStatement(parentStatementName)) {
          ms = configuration.getMappedStatement(parentStatementName);
        }
      }
      if (ms == null) {
        throw new BindingException("Invalid bound statement (not found): " + statementName);
      }
      name = ms.getId();
      type = ms.getSqlCommandType();
      if (type == SqlCommandType.UNKNOWN) {
        throw new BindingException("Unknown execution method for: " + name);
      }
    }

    public String getName() {
      return name;
    }

    public SqlCommandType getType() {
      return type;
    }
  }

  //方法签名
  public static class MethodSignature {

    private final boolean returnsMany;
    private final boolean returnsMap;
    private final boolean returnsVoid;
    private final Class<?> returnType;
    private final String mapKey;
    private final Integer resultHandlerIndex;
    private final Integer rowBoundsIndex;
    private final SortedMap<Integer, String> params;
    private final boolean hasNamedParameters;

    public MethodSignature(Configuration configuration, Method method) throws BindingException {
      this.returnType = method.getReturnType();
      this.returnsVoid = void.class.equals(this.returnType);
      this.returnsMany = (configuration.getObjectFactory().isCollection(this.returnType) || this.returnType.isArray());
      this.mapKey = getMapKey(method);
      this.returnsMap = (this.mapKey != null);
      this.hasNamedParameters = hasNamedParams(method);
      //以下重复循环2遍调用getUniqueParamIndex，是不是降低效率了
      //记下RowBounds是第几个参数
      this.rowBoundsIndex = getUniqueParamIndex(method, RowBounds.class);
      //记下ResultHandler是第几个参数
      this.resultHandlerIndex = getUniqueParamIndex(method, ResultHandler.class);
      this.params = Collections.unmodifiableSortedMap(getParams(method, this.hasNamedParameters));
    }

    public Object convertArgsToSqlCommandParam(Object[] args) {
      final int paramCount = params.size();
      if (args == null || paramCount == 0) {//没有参数
        return null;
      } else if (!hasNamedParameters && paramCount == 1) {//存在一个参数
        return args[params.keySet().iterator().next()];
      } else {
    	//否则，返回一个ParamMap,修改参数名,参数名就是其位置
        final Map<String, Object> param = new ParamMap<Object>();
        int i = 0;
        for (Map.Entry<Integer, String> entry : params.entrySet()) {
        	//1 先加一个#{1},#{2},#{3}...参数
          param.put(entry.getValue(), args[entry.getKey()]);
          // issue #71, add param names as param1, param2...but ensure backward compatibility
          final String genericParamName = "param" + String.valueOf(i + 1);
          if (!param.containsKey(genericParamName)) {
        	//2.再加一个#{param1},#{param2}...参数
        	//你可以传递多个参数给一个映射器方法，如果你这样做了
        	//默认情况下它们将会以它们在参数列表中的位置来命名,比如:#{param1},#{param2}等。
        	//如果你想改变参数的名称(只在多参数情况下),那么你可以使用注解@Param("paramName")
            param.put(genericParamName, args[entry.getKey()]);
          }
          i++;
        }
        return param;
      }
    }

    public boolean hasRowBounds() {
      return (rowBoundsIndex != null);
    }

    public RowBounds extractRowBounds(Object[] args) {
      return (hasRowBounds() ? (RowBounds) args[rowBoundsIndex] : null);
    }

    public boolean hasResultHandler() {
      return (resultHandlerIndex != null);
    }

    public ResultHandler extractResultHandler(Object[] args) {
      return (hasResultHandler() ? (ResultHandler) args[resultHandlerIndex] : null);
    }

    public String getMapKey() {
      return mapKey;
    }

    public Class<?> getReturnType() {
      return returnType;
    }

    public boolean returnsMany() {
      return returnsMany;
    }

    public boolean returnsMap() {
      return returnsMap;
    }

    public boolean returnsVoid() {
      return returnsVoid;
    }

    private Integer getUniqueParamIndex(Method method, Class<?> paramType) {
      Integer index = null;
      final Class<?>[] argTypes = method.getParameterTypes();
      for (int i = 0; i < argTypes.length; i++) {
        if (paramType.isAssignableFrom(argTypes[i])) {
          if (index == null) {
            index = i;
          } else {
            throw new BindingException(method.getName() + " cannot have multiple " + paramType.getSimpleName() + " parameters");
          }
        }
      }
      return index;
    }

    /**
     * 查找方法返回值是否为Map类型 如果是则判断该method上是否存在MapKey注解
     * 如果存在MapKey注解，则将这个注解的值作为map的key
     * @param method
     * @return
     */
    private String getMapKey(Method method) {
      String mapKey = null;
      if (Map.class.isAssignableFrom(method.getReturnType())) {
        final MapKey mapKeyAnnotation = method.getAnnotation(MapKey.class);
        if (mapKeyAnnotation != null) {
          mapKey = mapKeyAnnotation.value();
        }
      }
      return mapKey;
    }
    //获取method中的参数 并放入到TreeMap
    private SortedMap<Integer, String> getParams(Method method, boolean hasNamedParameters) {
    	//使用一个TreeMap,保证方法中的参数 能够还是保持参数的先后顺序
      final SortedMap<Integer, String> params = new TreeMap<Integer, String>();
      //返回method中的参数数组
      final Class<?>[] argTypes = method.getParameterTypes();
      for (int i = 0; i < argTypes.length; i++) {
    	  //参数不是RowBounds与ResultHandler类型
        if (!RowBounds.class.isAssignableFrom(argTypes[i]) && !ResultHandler.class.isAssignableFrom(argTypes[i])) {
        	//参数的名字默认为0,1,2 这也是为什么xml里面可以使用#{}来标识参数的原因
          String paramName = String.valueOf(params.size());
          if (hasNamedParameters) {
        	  //使用注解@Param来重命名参数
            paramName = getParamNameFromAnnotation(method, i, paramName);
          }
          params.put(i, paramName);
        }
      }
      return params;
    }

    private String getParamNameFromAnnotation(Method method, int i, String paramName) {
    	//取出第i个参数上的所有Annotation
      final Object[] paramAnnos = method.getParameterAnnotations()[i];
      for (Object paramAnno : paramAnnos) {
        if (paramAnno instanceof Param) {//如果参数上存在Param注解 这将Param注解中的value值作为参数的名称
          paramName = ((Param) paramAnno).value();
        }
      }
      return paramName;
    }

    private boolean hasNamedParams(Method method) {
      boolean hasNamedParams = false;
      final Object[][] paramAnnos = method.getParameterAnnotations();
      for (Object[] paramAnno : paramAnnos) {
        for (Object aParamAnno : paramAnno) {
          if (aParamAnno instanceof Param) {
        	//查找@Param注解，一般不会用注解吧，可以忽略
            hasNamedParams = true;
            break;
          }
        }
      }
      return hasNamedParams;
    }

  }

}
