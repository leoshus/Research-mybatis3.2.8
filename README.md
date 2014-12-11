###Research-mybatis3.2.8
=====================
####官方WIKI:
------------
> [MyBatis](http://mybatis.github.io/mybatis-3/)
> [MyBatis-Spring](http://mybatis.github.io/spring/)

* MyBatis完成了Java对象到输入参数的映射以及结果集到java对象的转化工作。既解决了Java对象与输入参数和结果集的映射，又方便了用户手写使用SQL语句。
	>2014/11/28 ---start---
	> [org.apache.ibatis.session.Configuration](https://github.com/sdw2330976/Research-mybatis3.2.8/tree/master/mydoc/configuration.md)

####全局配置文件 
---------------
* MyBatis像其他ORM框架一样也具有自己的全局配置文件,以下是一个简单的配置示例：

```
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE configuration PUBLIC "-//mybatis.org//DTD Config 3.0//EN" "http://mybatis.org/dtd/mybatis-3-config.dtd">
<!-- 这里需要注意:每个标签的书写顺序否则DTD会报错 (properties?, settings?, typeAliases?, typeHandlers?, objectFactory?, 
 objectWrapperFactory?, plugins?, environments?, databaseIdProvider?, mappers?) -->
<configuration>
 	<!-- 属性配置  相同属性优先级顺序为：作为方法参数的properties > 资源/url中的properties > properties元素的子元素<property> -->
	<properties resource="com/sdw/soft/test/showcase/config.properties">
		<!-- <property name="username" value="sa"/>
		<property name="password" value="sa"/> -->
	</properties>
	<!-- 设置缓存和延迟加载等等运行时的行为方式 -->
	<settings>
		<!-- 设置超时时间，他决定驱动等待一个数据库响应时间 -->
		<setting name="defaultStatementTimeout" value="25000"/>
		<!-- 全局的映射器启用/禁用缓存 -->
		<setting name="cacheEnabled" value="true"/>
		<!-- 全局启用/禁用延迟加载 禁用时，所有关联对象都会即时加载 特定关联关系中可通过设置fetchType属性来覆盖该项的开关状态。 -->
		<setting name="lazyLoadingEnabled" value="false"/>
		<!-- 启用时，带延迟加载属性的对象的加载与否完全取决于对任意延迟属性的调用 反之 每种属性将会按需加载 -->
		<setting name="aggressiveLazyLoading" value="true"/>
		<!-- 是否允许单一语句返回多结果集 (需要兼容驱动) -->
		<setting name="multipleResultSetsEnabled" value="true"/>
		<!-- 使用列标签代替列名。不同的驱动在这方面有不同的表现 具体可以参考相关驱动文档或通过测试这两种不同的模式来观察所用驱动的结果 -->
		<setting name="useColumnLabel" value="true"/>
		<!-- 允许JDBC支持自动生成主键，需要驱动兼容。如果设置为true则这个设置强制使用自动生成主键，尽管一些驱动不兼容但仍能正常工作(如Derby) -->
		<setting name="useGeneratedKeys" value="false"/>
		<!-- 指定MyBatis是否以及如何自动映射指定的列到字段或属性。
		NONE表示取消自动映射；
		PARTIAL只会自动映射没有定义嵌套结果集映射的结果集。
		FULL会自动映射任意复杂的结果集(包括嵌套和其他情况) -->
		<setting name="autoMappingBehavior" value="PARTIAL"/>
		<!-- 配置默认的执行器。
		    SIMPLE就是普通的执行器；
		    REUSE执行器会重用预处理语句(PreparedStatements)
		    BATCH执行器将重用语句并执行批量更新 -->
		<setting name="defaultExecutorType" value="SIMPLE"/>
	</settings>
	<!-- 别名 -->
	<typeAliases>
		<!-- <typeAlias type="" alias=""/> -->
	</typeAliases>	
	<environments default="development">
		<environment id="development">
			<transactionManager type="JDBC"/>
			<!-- type分三种：
			     UNPOOLED:每次被请求时简单打开和关闭连接 
			     POOLED:实现池的概念将JDBC连接对象组织起来
			     JNDI:为了能在EJB或应用服务器-->
			<dataSource type="POOLED">
				<property name="driver" value="${driver}"/>
				<property name="url" value="${url}"/>
				<property name="username" value="${username}"/>
				<property name="password" value="${password}"/>
			</dataSource>
		</environment>
	</environments>
	<mappers>
		<mapper resource="com/sdw/soft/test/showcase/user.xml"/>
	</mappers>
</configuration>
```
其中
* properties---提供可用于整个配置文件的键值对属性信息
* settings-----用于设置MyBatis延迟加载、缓存等运行时的行为方式
* typeAliases--为java类型指定别名，方便在xml文件中用别名取代java类的全限定名
* typeHandlers--MyBatis无论在预处理语句(PreparedStatement)中为占位符赋值、亦或是从ResultSet中取出一个值，都会使用类型处理器将获取的值用合适的方式转化成java类型，
   MyBatis默认的类型处理器 详见[这里](http://mybatis.github.io/mybatis-3/zh/configuration.html#typeHandlers)
* objectFactory---用于创建结果对象，如果我们需要覆盖默认对象工厂的行为，可以继承DefaultObjectFactory实现自己的对象工厂
* plugins---------MyBatis允许在已映射语句执行的某一点进行拦截调用。详见[这里](http://mybatis.github.io/mybatis-3/zh/configuration.html#plugins)
* environments----满足MyBatis配置成适应多种环境，有助于将SQL映射应用到多种数据库中。例如我们的开发、测试、生产可能需要不同的配置
* mappers---------应用中用到的所有SQL映射文件都在这里列表，方便MyBatis管理
有了如上配置，MyBatis就可以与数据库建立连接，并应用给定的连接池和事务属性，MyBatis封装了这些操作，并暴露出SqlSessionFactory实例供开发者使用，
从而创建SQLSession实例进行业务逻辑操作，而不用反复书写JDBC相关的代码。

```
InputStream is = Resources.getResourceAsStream("com/sdw/soft/test/showcase/mybatis-config.xml");
SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(is);
if(session == null)
	session = sqlSessionFactory.openSession();
```
####代码配置
------------
当然我们除了使用全局配置文件，来配置MyBatis 我们亦可通过代码直接对MyBatis进行配置
```
DataSource ds = getDataSource();//获取数据源
TransactionFactory txFactory = new JdbcTransactionFactory();
Environment environment = new Environment("development",txFactory,ds);
Configuration configuration = new Configuration(environment);
configuration.addMapper(MapperTest.class);
SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
```

####设计模式
------------
* 1.[建造模式](https://github.com/sdw2330976/Research-mybatis3.2.8/tree/master/mydoc/designPattern/builder.md)XMLConfigBuilder/XMLMapperBuilder/XMLStatementBuilder/CacheBuilder,Environment,把建造的步骤分装到一个类里,且运用了fluent API模式。

mybatis3-cn /cnblogs search
