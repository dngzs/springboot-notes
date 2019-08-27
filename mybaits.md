# mybaits源码分析系列

### 一、Configuration的创建

###  1.SqlSessionFactory

#### 1.1 接口定义和结构

在mybatis中SqlSessionFactory是真的很重要，可以这样说，他就是mybatis门面创建了工具类了，怀着一颗好奇的新，我决定去看看他的借口定义和继承结构图

```java
public interface SqlSessionFactory {

  SqlSession openSession();

  SqlSession openSession(boolean autoCommit);
  SqlSession openSession(Connection connection);
  SqlSession openSession(TransactionIsolationLevel level);

  SqlSession openSession(ExecutorType execType);
  SqlSession openSession(ExecutorType execType, boolean autoCommit);
  SqlSession openSession(ExecutorType execType, TransactionIsolationLevel level);
  SqlSession openSession(ExecutorType execType, Connection connection);

  Configuration getConfiguration();

}
```

可以看到，SqlSessionFactory借口是非常的简单，除了最后一个方法，都是用来获取SqlSession的，至于sqlsession，当然这个算是mybatis抛给我们开发者去调用的一个门面了，我后续会去分析。接口定义看的差不多了，我决定再去看看继承结构图

![](mybatisimg\1.png)

SqlSessionFactory实现类：DefaultSqlSessionFactory和SqlSessionManager

#### 1.2 DefaultSqlSessionFactory和SqlSessionManager源码分析

##### 1.2.1 SqlSessionManager

SqlSessionManager同时实现了SqlSessionFactory和sqlSession接口

```java
ublic class SqlSessionManager implements SqlSessionFactory, SqlSession {

  //这里的不用想都是DefaultSqlSessionFactory
  private final SqlSessionFactory sqlSessionFactory;
  //这个是通过SqlSessionInterceptor  jdk代理来的
  private final SqlSession sqlSessionProxy;

  private ThreadLocal<SqlSession> localSqlSession = new ThreadLocal<SqlSession>();

  public static SqlSessionManager newInstance(SqlSessionFactory sqlSessionFactory) {
    return new SqlSessionManager(sqlSessionFactory);
  }

  private SqlSessionManager(SqlSessionFactory sqlSessionFactory) {
    this.sqlSessionFactory = sqlSessionFactory;
    //可以看到这个SqlSession是代理来的
    this.sqlSessionProxy = (SqlSession) Proxy.newProxyInstance(
        SqlSessionFactory.class.getClassLoader(),
        new Class[]{SqlSession.class},
        new SqlSessionInterceptor());
  }

  public void startManagedSession() {
    this.localSqlSession.set(openSession());
  }

  public void startManagedSession(boolean autoCommit) {
    this.localSqlSession.set(openSession(autoCommit));
  }

  public boolean isManagedSessionStarted() {
    return this.localSqlSession.get() != null;
  }


  public SqlSession openSession(TransactionIsolationLevel level) {
    return sqlSessionFactory.openSession(level);
  }

  public Configuration getConfiguration() {
    return sqlSessionFactory.getConfiguration();
  }

  public <T> T selectOne(String statement) {
    return sqlSessionProxy.<T> selectOne(statement);
  }

  public <T> T selectOne(String statement, Object parameter) {
    return sqlSessionProxy.<T> selectOne(statement, parameter);
  }

  public <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey, RowBounds rowBounds) {
    return sqlSessionProxy.<K, V> selectMap(statement, parameter, mapKey, rowBounds);
  }

  public void select(String statement, ResultHandler handler) {
    sqlSessionProxy.select(statement, handler);
  }

  public int insert(String statement, Object parameter) {
    return sqlSessionProxy.insert(statement, parameter);
  }

  public int update(String statement) {
    return sqlSessionProxy.update(statement);
  }

  public int delete(String statement) {
    return sqlSessionProxy.delete(statement);
  }
    
  public <T> T getMapper(Class<T> type) {
    return getConfiguration().getMapper(type, this);
  }

  public Connection getConnection() {
    final SqlSession sqlSession = localSqlSession.get();
    if (sqlSession == null) throw new SqlSessionException("Error:  Cannot get connection.  No managed session is started.");
    return sqlSession.getConnection();
  }


  private class SqlSessionInterceptor implements InvocationHandler {
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      final SqlSession sqlSession = SqlSessionManager.this.localSqlSession.get();
      if (sqlSession != null) {
        try {
          return method.invoke(sqlSession, args);
        } catch (Throwable t) {
          throw ExceptionUtil.unwrapThrowable(t);
        }
      } else {
        final SqlSession autoSqlSession = openSession();
        try {
          final Object result = method.invoke(autoSqlSession, args);
          autoSqlSession.commit();
          return result;
        } catch (Throwable t) {
          autoSqlSession.rollback();
          throw ExceptionUtil.unwrapThrowable(t);
        } finally {
          autoSqlSession.close();
        }
      }
    }
  }
//....部分源码省略
}

```

在SqlSessionManager中存在内部类,该内部类的作用就是代理sqlSession以完成复用SqlSession的目的

```java
private class SqlSessionInterceptor implements InvocationHandler {
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      
      final SqlSession sqlSession = SqlSessionManager.this.localSqlSession.get();
      //存在线程局部变量sqlSession（使得在同一个线程内，达到复用sqlSession的效果，对于事务而言，需要手动处理）
      if (sqlSession != null) {
        try {
          return method.invoke(sqlSession, args);
        } catch (Throwable t) {
          throw ExceptionUtil.unwrapThrowable(t);
        }
      } else {
        //如果不存在线程局部变量，就创建一个可以自定提交，回滚关闭的SqlSession
        final SqlSession autoSqlSession = openSession();
        try {
          final Object result = method.invoke(autoSqlSession, args);
          autoSqlSession.commit();
          return result;
        } catch (Throwable t) {
          autoSqlSession.rollback();
          throw ExceptionUtil.unwrapThrowable(t);
        } finally {
          autoSqlSession.close();
        }
      }
    }
  }
```

##### 1.2.2 SqlSessionManager的使用

![](mybatisimg\2.png)

可以看到多次执行sql后再执行提交或者回滚操作，其实底层就是用的动态代理+threadLocal来完成的，同一个线程用的同一个sqlSession。

> ​     其实SqlSessionManager基本属于废弃状态，为什么这么好用的一个线程安全的东西，会沦落到废弃的程度呢？这是一个问题，我想后续看了下面的内容后再揭晓，大家也不妨思考下

##### 1.2.3 DefaultSqlSessionFactory

想比起SqlSessionManager，DefaultSqlSessionFactory的代码就简单多了,其实就是每次都生成一个新的SqlSession，这里就不一一来分析源码了，按照重要的部分来阅读下

```java
public class DefaultSqlSessionFactory implements SqlSessionFactory {

  private final Configuration configuration;

  public DefaultSqlSessionFactory(Configuration configuration) {
    //初始化的时候进塞进来了，获取configuration的过程我们下面再看，比较复杂
    this.configuration = configuration;
  }

  @Override
  public SqlSession openSession() {
    return openSessionFromDataSource(configuration.getDefaultExecutorType(), null, false);
  }

  @Override
  public SqlSession openSession(boolean autoCommit) {
    return openSessionFromDataSource(configuration.getDefaultExecutorType(), null, autoCommit);
  }

  @Override
  public SqlSession openSession(ExecutorType execType) {
    return openSessionFromDataSource(execType, null, false);
  }

 //...省略部分代码
    
  @Override
  public Configuration getConfiguration() {
    return configuration;
  }

  private SqlSession openSessionFromDataSource(ExecutorType execType, TransactionIsolationLevel level, boolean autoCommit) {
    Transaction tx = null;
    try {
      final Environment environment = configuration.getEnvironment();
      //从环境中获取TransactionFactory对象
      final TransactionFactory transactionFactory = getTransactionFactoryFromEnvironment(environment);
      //获取事务管理器
      tx = transactionFactory.newTransaction(environment.getDataSource(), level, autoCommit);
      //创建比较重要的Executor对象
      final Executor executor = configuration.newExecutor(tx, execType);
      //通过Executor对象，获取到DefaultSqlSession
      return new DefaultSqlSession(configuration, executor, autoCommit);
    } catch (Exception e) {
      //如果发生异常，就关闭掉事务管理器（也就是关闭其中的链接）
      closeTransaction(tx); // may have fetched a connection so lets call close()
      throw ExceptionFactory.wrapException("Error opening session.  Cause: " + e, e);
    } finally {
      ErrorContext.instance().reset();
    }
  }

 //...省略部分代码
}
```

源码可以看到，DefaultSqlSessionFactory也是线程安全的。

##### 1.2.4  总结

我们可以看到SqlSessionManager其实是对DefaultSqlSessionFactory的一个封装，在SqlSessionManager的内部维护了一个SqlSessionFactory的成员变量

```java
private final SqlSessionFactory sqlSessionFactory;
```

在newInstance的时候，通过SqlSessionFactoryBuilder（）来创建的一个DefaultSqlSessionFactory

```java
public static SqlSessionManager newInstance(Reader reader) {
  return new SqlSessionManager(new SqlSessionFactoryBuilder().build(reader, null, null));
}
```

与DefaultSqlSessionFactory不同的是:

1. SqlSessionManager提供了一个本地线程变量，每当通过startManagedSession()获得session实例的时候，会在本地线程保存session实例

```java
public void startManagedSession() {
  this.localSqlSession.set(openSession());
}

@Override
public SqlSession openSession() {
  return sqlSessionFactory.openSession();
}
```

2. SqlSessionManager实现了SqlSession接口，那么SqlSessionManager就有SqlSessionFactory和SqlSession的功能了，通过SqlSessionManager，开发者不需要知道SqlSessionFactory是怎么创建的，直接可以面向SqlSession来编程

​      ==这样，在同一个线程实现不同的sql操作，可以复用本地线程session，避免了DefaultSqlSessionFactory实现的每一个sql操作都要创建新的session实例。==



##### 1.2.5  其他

  我们已经分析完了mybaits包里面的SqlSessionFactory创建session的过程，那么和spring整合是怎么样创建session的，我决定后续再分析，先理解整体脉络吧



### 2. Configuration是怎么创建的

在上面的例子中我们知道，生成一个SqlSessionFactory是通过下面的这一行代码

```java
public static SqlSessionManager newInstance(Reader reader) {
  return new SqlSessionManager(new SqlSessionFactoryBuilder().build(reader, null, null));
}
```

那么我们来着重看下这段代码，可以说这段代码是整个mybatis比较核心的代码了

```java
new SqlSessionFactoryBuilder().build(reader, null, null)
```

深入进去，我们可以看到，mybaits通过XMLConfigBuilder来解析我们的配置文件，调用

XMLConfigBuilder#parse()方法来解析

```java
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

```

XMLConfigBuilder#parse()

```java
public Configuration parse() {
  if (parsed) {
    throw new BuilderException("Each XMLConfigBuilder can only be used once.");
  }
  parsed = true;
  parseConfiguration(parser.evalNode("/configuration"));
  return configuration;
}
```

重点来了，我们看到，通过XMLConfigBuilder#parse()我们得到了Configuration对象，然后通过

```java
return new DefaultSqlSessionFactory(config);
```

我们就获取到DefaultSqlSessionFactory对象了，可以看到，这个Configuration是多么的重要，接下来，我们来正式研究这段代码

```java
public Configuration parse() {
  if (parsed) {
    throw new BuilderException("Each XMLConfigBuilder can only be used once.");
  }
  parsed = true;
  parseConfiguration(parser.evalNode("/configuration"));
  return configuration;
}
```

别急，在分析之前，我们先来看下mybatis是怎么样解析配置文件的  ==XMLConfigBuilder==

![](mybatisimg\3.png)

可以看到XMLConfigBuilder是继承自BaseBuilder的，而且BaseBuilder也是有几个实现的，这些实现后面我们都会接触到，先来看看XMLConfigBuilder，在XMLConfigBuilder中，一共存在四个成员变量

```java
  //解析标识，Configuration作为全局唯变量，不能初始化多次，true代表已经解析过了，fasle代表没有解析过
  private boolean parsed;
  //主要用来解析xml的
  private final XPathParser parser;
  //环境参数，主要来解析当前所在的环境
  private String environment;
  //反射工厂类
  private final ReflectorFactory localReflectorFactory = new DefaultReflectorFactory();
```

这里我们来讲个知识点，在mybatis中，解析xml用的xpath来解析的，有兴趣的小伙伴可以去了解下xpath，我这里就不啰嗦了

进入正题

```java
public Configuration parse() {
  if (parsed) {
    throw new BuilderException("Each XMLConfigBuilder can only be used once.");
  }
  //标注开始解析
  parsed = true;
  //从跟节点<configuration>开始解析
  parseConfiguration(parser.evalNode("/configuration"));
  return configuration;
}
```

开始解析Configuration下的节点数据

```java
private void parseConfiguration(XNode root) {
    try {
      //issue #117 read properties first
      //解析properties
      propertiesElement(root.evalNode("properties"));
      //settings
      Properties settings = settingsAsProperties(root.evalNode("settings"));
      loadCustomVfs(settings);
      //别名解析
      typeAliasesElement(root.evalNode("typeAliases"));
      pluginElement(root.evalNode("plugins"));
      objectFactoryElement(root.evalNode("objectFactory"));
      objectWrapperFactoryElement(root.evalNode("objectWrapperFactory"));
      reflectorFactoryElement(root.evalNode("reflectorFactory"));
      settingsElement(settings);
      // read it after objectFactory and objectWrapperFactory issue #631
      environmentsElement(root.evalNode("environments"));
      databaseIdProviderElement(root.evalNode("databaseIdProvider"));
      typeHandlerElement(root.evalNode("typeHandlers"));
      mapperElement(root.evalNode("mappers"));
    } catch (Exception e) {
      throw new BuilderException("Error parsing SQL Mapper Configuration. Cause: " + e, e);
    }
  }
```

接下来我们一个个看，怎么具体解析对应的值的，我们再参照下官方文档的内容对比这看

#### 2.1 properties的读取

在mybaits官网有如下的[一段话](http://www.mybatis.org/mybatis-3/zh/configuration.html#properties)

> 如果属性在不只一个地方进行了配置，那么 MyBatis 将按照下面的顺序来加载：
>
> - 在 properties 元素体内指定的属性首先被读取。
> - 然后根据 properties 元素中的 resource 属性读取类路径下属性文件或根据 url 属性指定的路径读取属性文件，并覆盖已读取的同名属性。
> - 最后读取作为方法参数传递的属性，并覆盖已读取的同名属性。
>
> 因此，通过方法参数传递的属性具有最高优先级，resource/url 属性中指定的配置文件次之，最低优先级的是 properties 属性中指定的属性。

```xml
<properties resource="mybatis.properties">
    <property name="lang" value="java"/>
    <property name="name" value="lisi"/>
    <property name="age" value="18"/>
</properties>
```

```java
private void propertiesElement(XNode context) throws Exception {
  if (context != null) {
    //从properties 元素体内读取,对着上面的代码，也就是读取lang、name、age这些值
    Properties defaults = context.getChildrenAsProperties();
     
    //------------------------------------------------------------------
    String resource = context.getStringAttribute("resource");
    String url = context.getStringAttribute("url");
    if (resource != null && url != null) {
      throw new BuilderException("The properties element cannot specify both a URL and a resource based property file reference.  Please specify one or the other.");
    }
     //然后从resource何url中读取，如果读取到的key和上面的重复，putall后会覆盖前面的值，也验证了官网的话
    if (resource != null) {
      defaults.putAll(Resources.getResourceAsProperties(resource));
    } else if (url != null) {
      defaults.putAll(Resources.getUrlAsProperties(url));
    }
    //------configuration中的值是从SqlSessionFactoryBuilder().build()传递进来的----------
    Properties vars = configuration.getVariables();
    if (vars != null) {
      //putall后会覆盖前面的值
      defaults.putAll(vars);
    }
    //设置进XPathParser对象中
    parser.setVariables(defaults);
    //将读取到的值重新设置进configuration的Variables对象中
    configuration.setVariables(defaults);
  }
}

 public Properties getChildrenAsProperties() {
    Properties properties = new Properties();
    for (XNode child : getChildren()) {
      String name = child.getStringAttribute("name");
      String value = child.getStringAttribute("value");
      if (name != null && value != null) {
        properties.setProperty(name, value);
      }
    }
    return properties;
  }
```

#### 2.2 settings的读取

settings对于mybatis来说，是特别重要了，它们会改变 MyBatis 的运行时行为

比较常见的我列举下

| **设置名**               | **描述**                                                     | **有效值**                                      | 默认值                |
| ------------------------ | ------------------------------------------------------------ | ----------------------------------------------- | --------------------- |
| cacheEnabled             | 二级缓存控制器                                               | true\|false                                     | true(==建议关闭==)    |
| lazyLoadingEnabled       | 延迟加载的全局开关。当开启时，所有关联对象都会延迟加载。 特定关联关系中可通过设置 `fetchType`属性来覆盖该项的开关状态。 | true\|false                                     | false                 |
| aggressiveLazyLoading    | 当开启时，任何方法的调用都会加载该对象的所有属性。 否则，每个属性会按需加载（参考 `lazyLoadTriggerMethods`)。 |                                                 |                       |
| jdbcTypeForNull          | 当没有为参数提供特定的 JDBC 类型时，为空值指定 JDBC 类型。 某些驱动需要指定列的 JDBC 类型，多数情况直接用一般类型即可，比如 NULL、VARCHAR 或 OTHER。 | JdbcType 常量，常用值：NULL, VARCHAR 或 OTHER。 | OTHER                 |
| mapUnderscoreToCamelCase | 是否开启自动驼峰命名规则（camel case）映射，即从经典数据库列名 A_COLUMN 到经典 Java 属性名 aColumn 的类似映射。 | true\|false                                     | false                 |
| defaultStatementTimeout  | 设置超时时间，它决定驱动等待数据库响应的秒数。               | 任意正整数                                      | 未设置（null）        |
| defaultExecutorType      | 配置默认的执行器。SIMPLE 就是普通的执行器；REUSE 执行器会重用预处理语句（prepared statements）； BATCH 执行器将重用语句并执行批量更新。 | SIMPLE REUSE BATCH                              | SIMPLE(==建议REUSE==) |

settings配置的获取和校验

```java
 private Properties settingsAsProperties(XNode context) {
    //如果没有配置那就直接设置一个空值
    if (context == null) {
      return new Properties();
    }
    //读取配置
    Properties props = context.getChildrenAsProperties();
    // 通过set方法检查这些个设置是不是都是configuration的属性
    MetaClass metaConfig = MetaClass.forClass(Configuration.class, localReflectorFactory);
    for (Object key : props.keySet()) {
      if (!metaConfig.hasSetter(String.valueOf(key))) {
        throw new BuilderException("The setting " + key + " is not known.  Make sure you spelled it correctly (case sensitive).");
      }
    }
    return props;
  }
```

> 这里说个插曲，在mybaits3.3.1版本，这里有一块代码是这样写的
>
> ```java
> Properties settings = settingsAsPropertiess(root.evalNode("settings"));
> //issue #117 read properties first
> propertiesElement(root.evalNode("properties"));
> ```
>
> 这里先去读取了settings的配置，properties后去读取，导致一个什么问题
>
> ```xml
> <configuration>
>     <properties resource="META-INF/mybatis/mybatis-config.properties" />
> 
>     <settings>
>         <setting name="jdbcTypeForNull" value="${jdbcTypeForNull}" />
>         <setting name="jdbcTypeForNull" value="NULL" />
>         <setting name="mapUnderscoreToCamelCase" value="true" />
>         <setting name="defaultFetchSize" value="100" />
>     </settings>
> 
>     <!-- omitted -->
> </configuration>
> 
> ```
>
> <!--mybatis-config.properties -->
>
> ```properties
> jdbcTypeForNull=NULL
> ```
>
> ${jdbcTypeForNull}东西读取不到了，报错
>
> IllegalArgumentException: No enum constant org.apache.ibatis.type.JdbcType.${jdbcTypeForNull}
>
> 我们在3.4.2看到了这个修复代码，因为3.3.1是最后一个3.3.x版本，所以作者在就近的版本修复了这个bug
>
> ```
> propertiesElement(root.evalNode("properties"));
> Properties settings = settingsAsProperties(root.evalNode("settings"));
> ```
>
> 是的你没看错，就是调整了下这两个获取的顺序，就行了
>
> 原因也很简单：先去读取properties，通过Xnode传递XPathParser中的Variables，再根据VariableTokenHandler来解析${jdbcTypeForNull}，就拿到了properties中的数据
>
> ```java
> public static String parse(String string, Properties variables) {
>   VariableTokenHandler handler = new VariableTokenHandler(variables);
>   GenericTokenParser parser = new GenericTokenParser("${", "}", handler);
>   return parser.parse(string);
> }
> ```

#### 2.3 vfsImpl

VFS含义是虚拟文件系统；主要是通过程序能够方便读取本地文件系统、FTP文件系统等系统中的文件资源

此内容暂时略过（后续有空再说，自定义的概率不大）

```java
loadCustomVfs(settings);

private void loadCustomVfs(Properties props) throws ClassNotFoundException {
  String value = props.getProperty("vfsImpl");
  if (value != null) {
    String[] clazzes = value.split(",");
    for (String clazz : clazzes) {
      if (!clazz.isEmpty()) {
        @SuppressWarnings("unchecked")
        Class<? extends VFS> vfsImpl = (Class<? extends VFS>)Resources.classForName(clazz);
        configuration.setVfsImpl(vfsImpl);
      }
    }
  }
}
```

#### 2.4 类型别名typeAliases

类型别名是为 Java 类型设置一个短的名字。 它只和 XML 配置有关，存在的意义仅在于用来减少类完全限定名的冗余

举个例子

在没有typeAliases的时候，你写个返回值可能要怎么在xml中配置

```java
select id="getWithAgeAndId" resultType="com.best.po.User"
```

   resultType =  com.best.po.User 这样写会让你很难受

那么如果加了typeAliases：

 resultType =  user

当然也可以指定一个包名，MyBatis 会在包名下面搜索需要的 Java Bean，比如：

```
<typeAliases>
  <package name="com.best.po"/>
</typeAliases>
```

如果你设置的包，那么mybaits会使用类的首字母小写名（com.best.po.User  =  user）来作为别名使用，当然你也可以通过

```java
@Alias("user")
public class User {
 ...
}
```

当然mybaits也帮自动我们注册了一些常用类型的别名，这里就不列举了，太多了，如果有需要可以再[这里查看](http://www.mybatis.org/mybatis-3/zh/configuration.html#properties)

分析完理论，我们通过源码层面来加强下认识

```java
typeAliasesElement(root.evalNode("typeAliases"));
```

```java
private void typeAliasesElement(XNode parent) {
  if (parent != null) {
    for (XNode child : parent.getChildren()) {
      //如果是package节点了
      if ("package".equals(child.getName())) {
        //获取包
        String typeAliasPackage = child.getStringAttribute("name");
        //从configuration中获取到已经初始化好的TypeAliasRegistry来注册别名
        configuration.getTypeAliasRegistry().registerAliases(typeAliasPackage);
      } else {
        //否则那就是typeAlias节点了
        String alias = child.getStringAttribute("alias");
        String type = child.getStringAttribute("type");
        try {
          Class<?> clazz = Resources.classForName(type);
           //注册
          if (alias == null) {
            //如果没有配置别名，如果存在注解Alias，则取注解的，否则取clazz的类名并转换成小写
            typeAliasRegistry.registerAlias(clazz);
          } else {
            //这里有个细节，如果配置的别名不是空,则以配置的为准（这里都会全部转换成小写）
            typeAliasRegistry.registerAlias(alias, clazz);
          }
        } catch (ClassNotFoundException e) {
          throw new BuilderException("Error registering typeAlias for '" + alias + "'. Cause: " + e, e);
        }
      }
    }
  }
}
```

```java
public void registerAlias(Class<?> type) {
  String alias = type.getSimpleName();
  //如果注解存在，则取注解的
  Alias aliasAnnotation = type.getAnnotation(Alias.class);
  if (aliasAnnotation != null) {
    alias = aliasAnnotation.value();
  } 
  registerAlias(alias, type);
}
```

至于包下面是怎么处理的，我们在来看看这里的代码

```java
 public void registerAliases(String packageName){
     //取包下,父类是Object的类
     registerAliases(packageName, Object.class);
  }

  public void registerAliases(String packageName, Class<?> superType){
    //通过resolverUtil工具类，获取到该包下object为父类的所有类
    ResolverUtil<Class<?>> resolverUtil = new ResolverUtil<Class<?>>();
    resolverUtil.find(new ResolverUtil.IsA(superType), packageName);
    Set<Class<? extends Class<?>>> typeSet = resolverUtil.getClasses();
    for(Class<?> type : typeSet){
      // Ignore inner classes and interfaces (including package-info.java)
      //跳过匿名内部类、接口、以及成员内部类
      if (!type.isAnonymousClass() && !type.isInterface() && !type.isMemberClass()) {
          //调用上面方法注册
          registerAlias(type);
      }
    }
  }
```

#### 2.5 插件

直接略过，后期会专门抽空大讲，简单看下注册到哪里去了

```java
private void pluginElement(XNode parent) throws Exception {
    if (parent != null) {
      for (XNode child : parent.getChildren()) {
        String interceptor = child.getStringAttribute("interceptor");
        Properties properties = child.getChildrenAsProperties();
        Interceptor interceptorInstance = (Interceptor) resolveClass(interceptor).newInstance();
        interceptorInstance.setProperties(properties);
        //把解析到的插件注入到configuration中去了
        configuration.addInterceptor(interceptorInstance);
      }
    }
  }
```

#### 2.6 ObjectFactory

MyBatis 每次创建结果对象的新实例时，它都会使用一个对象工厂（ObjectFactory）实例来完成，默认的对象工厂需要做的仅仅是实例化目标类，要么通过默认构造方法，要么在参数映射存在的时候通过参数构造方法来实例化，如果想覆盖对象工厂的默认行为，则可以通过创建自己的对象工厂来实现

个人认为自定义的情况基本没有，所以就不展开讨论了

```java
private void objectFactoryElement(XNode context) throws Exception {
  if (context != null) {
    String type = context.getStringAttribute("type");
    //拿到Properties对象
    Properties properties = context.getChildrenAsProperties();
    //通过别名查找的方式实例化
    ObjectFactory factory = (ObjectFactory) resolveClass(type).newInstance();
    factory.setProperties(properties);
    //然后注册到configuration中去
    configuration.setObjectFactory(factory);
  }
}
```

#### 2.7 settings的配置不存在，则使用默认设置

```java
private void settingsElement(Properties props) throws Exception {
  configuration.setAutoMappingBehavior(AutoMappingBehavior.valueOf(props.getProperty("autoMappingBehavior", "PARTIAL")));
  configuration.setAutoMappingUnknownColumnBehavior(AutoMappingUnknownColumnBehavior.valueOf(props.getProperty("autoMappingUnknownColumnBehavior", "NONE")));
  configuration.setCacheEnabled(booleanValueOf(props.getProperty("cacheEnabled"), true));
  configuration.setProxyFactory((ProxyFactory) createInstance(props.getProperty("proxyFactory")));
  configuration.setLazyLoadingEnabled(booleanValueOf(props.getProperty("lazyLoadingEnabled"), false));
  configuration.setAggressiveLazyLoading(booleanValueOf(props.getProperty("aggressiveLazyLoading"), false));
  configuration.setMultipleResultSetsEnabled(booleanValueOf(props.getProperty("multipleResultSetsEnabled"), true));
  configuration.setUseColumnLabel(booleanValueOf(props.getProperty("useColumnLabel"), true));
  configuration.setUseGeneratedKeys(booleanValueOf(props.getProperty("useGeneratedKeys"), false));
  configuration.setDefaultExecutorType(ExecutorType.valueOf(props.getProperty("defaultExecutorType", "SIMPLE")));
  configuration.setDefaultStatementTimeout(integerValueOf(props.getProperty("defaultStatementTimeout"), null));
  configuration.setDefaultFetchSize(integerValueOf(props.getProperty("defaultFetchSize"), null));
  configuration.setMapUnderscoreToCamelCase(booleanValueOf(props.getProperty("mapUnderscoreToCamelCase"), false));
  configuration.setSafeRowBoundsEnabled(booleanValueOf(props.getProperty("safeRowBoundsEnabled"), false));
  configuration.setLocalCacheScope(LocalCacheScope.valueOf(props.getProperty("localCacheScope", "SESSION")));
  configuration.setJdbcTypeForNull(JdbcType.valueOf(props.getProperty("jdbcTypeForNull", "OTHER")));
  configuration.setLazyLoadTriggerMethods(stringSetValueOf(props.getProperty("lazyLoadTriggerMethods"), "equals,clone,hashCode,toString"));
  configuration.setSafeResultHandlerEnabled(booleanValueOf(props.getProperty("safeResultHandlerEnabled"), true));
  configuration.setDefaultScriptingLanguage(resolveClass(props.getProperty("defaultScriptingLanguage")));
  @SuppressWarnings("unchecked")
  Class<? extends TypeHandler> typeHandler = (Class<? extends TypeHandler>)resolveClass(props.getProperty("defaultEnumTypeHandler"));
  configuration.setDefaultEnumTypeHandler(typeHandler);
  configuration.setCallSettersOnNulls(booleanValueOf(props.getProperty("callSettersOnNulls"), false));
  configuration.setUseActualParamName(booleanValueOf(props.getProperty("useActualParamName"), true));
  configuration.setReturnInstanceForEmptyRow(booleanValueOf(props.getProperty("returnInstanceForEmptyRow"), false));
  configuration.setLogPrefix(props.getProperty("logPrefix"));
  @SuppressWarnings("unchecked")
  Class<? extends Log> logImpl = (Class<? extends Log>)resolveClass(props.getProperty("logImpl"));
  configuration.setLogImpl(logImpl);
  configuration.setConfigurationFactory(resolveClass(props.getProperty("configurationFactory")));
}
```

可以看到，mybaits这边这些基本都是默认设置了，大家可以再这里找到settings的默认设置

#### 2.8 环境配置（environments）

MyBatis 可以配置成适应多种环境，这种机制有助于将 SQL 映射应用于多种数据库之中， 现实情况下有多种理由需要这么做。例如，开发、测试和生产环境需要有不同的配置；或者想在具有相同 Schema 的多个生产数据库中 使用相同的 SQL 映射。有许多类似的使用场景。

**不过要记住：尽管可以配置多个环境，但每个 SqlSessionFactory 实例只能选择一种环境。**

环境中有几个比较关键的设置，我们看下这个配置

```xml
<environments default="mysql">
        <environment id="mysql">
            <transactionManager type="jdbc"></transactionManager>
            <dataSource type="POOLED">
                <property name="driver" value="com.mysql.jdbc.Driver"></property>
                <property name="url" value="jdbc:mysql://127.0.0.1:3306/chat"></property>
                <property name="username" value="root"></property>
                <property name="password" value="root"></property>
            </dataSource>
        </environment>
</environments>
```

- 默认使用的环境 ID（比如：default="development"）。
- 每个 environment 元素定义的环境 ID（比如：id="development"）。

- 事务管理器的配置（比如：type="JDBC"）。
- 数据源的配置（比如：type="POOLED"）

还有一个 注意的点，default的取值是每个环境中的某一个id,而不是让你随便命名的

好了，简单的就理解到这里吧，我们来分析下源码，源码中可能会产生几个可能大家平时都可以见到的对象，这里我们在使用的时候来分析下这些组件

```java
 environmentsElement(root.evalNode("environments"));
```

```java
 private void environmentsElement(XNode context) throws Exception {
    if (context != null) {
      if (environment == null) {
        //获取default值
        environment = context.getStringAttribute("default");
      }
      for (XNode child : context.getChildren()) {
        //获取子元素（environment）的id值
        String id = child.getStringAttribute("id");
        //判断这个id值是不是和default的值一样，如果一样，继续解析
        if (isSpecifiedEnvironment(id)) {
          //获取TransactionFactory，通过<transactionManager>节点配置的
          TransactionFactory txFactory = transactionManagerElement(child.evalNode("transactionManager"));
          //获取DataSourceFactory，通过<dataSource>节点配置的
          DataSourceFactory dsFactory = dataSourceElement(child.evalNode("dataSource"));
          DataSource dataSource = dsFactory.getDataSource();
          //new一个Environment.Builder对象，这里用了建造者模式
          Environment.Builder environmentBuilder = new Environment.Builder(id)
               //设置了TransactionFactory
              .transactionFactory(txFactory)
              //设置了dataSource数据源
              .dataSource(dataSource);
          //然后将environment加入configuration对象中
          configuration.setEnvironment(environmentBuilder.build());
        }
      }
    }
  }
```

#### 2.9 typeHandlers

typeHandler算是比较重要的一个东西了，其实我们程序能够正常运行，是因为mybatis帮助我们配置了很多默认的typeHandler，那么typeHandler有什么作用，我们来了解一下

1. 在预处理语句（PreparedStatement）中设置一个参数时，mybatis会用到typeHandler
2. 从结果集中取出一个值时还是会用到typeHandler

那么到底是干啥的，其实就是jdbcType和java类型的映射，我的理解，那么我们去看看源码，到底是不是这样的，可以看到typeHandler的配置和别名的配置基本类似，可以用扫包

```java
typeHandlerElement(root.evalNode("typeHandlers"));

private void typeHandlerElement(XNode parent) throws Exception {
    if (parent != null) {
      for (XNode child : parent.getChildren()) {
         //子节点为package时，获取其name属性的值，然后自动扫描package下的自定义typeHandler
        if ("package".equals(child.getName())) {
          String typeHandlerPackage = child.getStringAttribute("name");
          //注册到configuration里的typeHandlerRegistry成员中
          typeHandlerRegistry.register(typeHandlerPackage);
        } else {
          //子节点为typeHandler时， 可以指定javaType属性， 也可以指定jdbcType, 也可两者都指定
          //javaType 是指定java类型
          //jdbcType 是指定jdbc类型（数据库类型： 如varchar）
          String javaTypeName = child.getStringAttribute("javaType");
          String jdbcTypeName = child.getStringAttribute("jdbcType");
          String handlerTypeName = child.getStringAttribute("handler");
          Class<?> javaTypeClass = resolveClass(javaTypeName);
          JdbcType jdbcType = resolveJdbcType(jdbcTypeName);
          Class<?> typeHandlerClass = resolveClass(handlerTypeName);
          if (javaTypeClass != null) {
            if (jdbcType == null) {
              typeHandlerRegistry.register(javaTypeClass, typeHandlerClass);
            } else {
              typeHandlerRegistry.register(javaTypeClass, jdbcType, typeHandlerClass);
            }
          } else {
           
            typeHandlerRegistry.register(typeHandlerClass);
          }
        }
      }
    }
  }
```

我们来分析下扫包的情况吧，下面的else其实是一样的注入方式

这段扫包的代码是否似曾相识，不过还是有点不一样的

```java
public void register(String packageName) {
  //通过ResolverUtil扫描到包下的TypeHandler类的所有子类
  ResolverUtil<Class<?>> resolverUtil = new ResolverUtil<Class<?>>();
  resolverUtil.find(new ResolverUtil.IsA(TypeHandler.class), packageName);
  Set<Class<? extends Class<?>>> handlerSet = resolverUtil.getClasses();
  for (Class<?> type : handlerSet) {
    //忽略掉匿名内部类，接口和抽象方法
    if (!type.isAnonymousClass() && !type.isInterface() && !Modifier.isAbstract(type.getModifiers())) {
      //开始注册（继续看这段）
      register(type);
    }
  }
}
```

```java
 public void register(Class<?> typeHandlerClass) {
    boolean mappedTypeFound = false;
     //查看是否有MappedTypes注解，该注解来指定与其关联的 Java 类型列表
    MappedTypes mappedTypes = typeHandlerClass.getAnnotation(MappedTypes.class);
    //如果有
    if (mappedTypes != null) {
      //拿到java类型，开始注册，并且把mappedTypeFound设置成true,表示找到了
      for (Class<?> javaTypeClass : mappedTypes.value()) {
        register(javaTypeClass, typeHandlerClass);
        mappedTypeFound = true;
      }
    }
    //如果没有注册到
    if (!mappedTypeFound) {
      //注册前，先获取到typeHandlerClass的实例（javaTypeClass设置成空）
      register(getInstance(null, typeHandlerClass));
    }
}

```

```java
//继续看注册方法（重载方法）
public <T> void register(TypeHandler<T> typeHandler) {
  boolean mappedTypeFound = false;
  //查看是否有MappedTypes注解，该注解来指定与其关联的 Java 类型列表
  MappedTypes mappedTypes = typeHandler.getClass().getAnnotation(MappedTypes.class);
  if (mappedTypes != null) {
    for (Class<?> handledType : mappedTypes.value()) {
      register(handledType, typeHandler);
      mappedTypeFound = true;
    }
  }
  // @since 3.1.0 - 尝试自动发现映射类型
  if (!mappedTypeFound && typeHandler instanceof TypeReference) {
    try {
      TypeReference<T> typeReference = (TypeReference<T>) typeHandler;
      //这里的typeReference.getRawType()其实就是获取typeHandler上的泛型，可以看看TypeReference的构造函数
      register(typeReference.getRawType(), typeHandler);
      mappedTypeFound = true;
    } catch (Throwable t) {
      // maybe users define the TypeReference with a different type and are not assignable, so just ignore it
    }
  }
  if (!mappedTypeFound) {
    register((Class<T>) null, typeHandler);
  }
}
```

```java
private <T> void register(Type javaType, TypeHandler<? extends T> typeHandler) {
  //获取MappedJdbcTypes类型（jdbcType）
  MappedJdbcTypes mappedJdbcTypes = typeHandler.getClass().getAnnotation(MappedJdbcTypes.class);
  if (mappedJdbcTypes != null) {
    for (JdbcType handledJdbcType : mappedJdbcTypes.value()) {
      //如果找到，就开始注册
      register(javaType, handledJdbcType, typeHandler);
    }
    if (mappedJdbcTypes.includeNullJdbcType()) {
      register(javaType, null, typeHandler);
    }
  } else {
    register(javaType, null, typeHandler);
  }
}
```

```java
private void register(Type javaType, JdbcType jdbcType, TypeHandler<?> handler) {
  if (javaType != null) {
    //从map中获取，如果是空或者空集合
    Map<JdbcType, TypeHandler<?>> map = TYPE_HANDLER_MAP.get(javaType);
    if (map == null || map == NULL_TYPE_HANDLER_MAP) {
      //就new一个
      map = new HashMap<JdbcType, TypeHandler<?>>();
      TYPE_HANDLER_MAP.put(javaType, map);
    }
    //map加入将 key-value:jdbcType->对应的handler
    map.put(jdbcType, handler);
  }
  //然后将将key-value:javaType->handler.class,handler实例加入ALL_TYPE_HANDLERS_MAP
  ALL_TYPE_HANDLERS_MAP.put(handler.getClass(), handler);
}
```

基本上源码就分析完成了，就是解析到自定义的handlerType到typeHandlerRegistry的一个过程，当然typeHandlerRegistry在初始化的时候，已经帮我们注册了很多mybatis自带的了，这里我们来看下比较特殊的几个handlerType

##### 2.9.1  EnumTypeHandler和 EnumOrdinalTypeHandler

若想映射枚举类型 `Enum`，则需要从 `EnumTypeHandler` 或者 `EnumOrdinalTypeHandler` 中选一个来使用。

EnumTypeHandler是通过Enum的name()来存储的

EnumOrdinalTypeHandler是通过Enum的ordinal()来存储的，也就是索引，注意，这里的索引是从0开始的

至于源码就不看了，很简单，如果自己定义了一些枚举，可能用不了，但是可以自定义TypeHandler来处理自定义的枚举类

### 3.mapper映射器

接下来就是比较重要的内容了，因为内容很多，故单独提一个标题来说明

解析 mapper，至于mapper，相比大家再熟悉不过了

```xml
<mappers>
        <mapper resource="UserMapper.xml"></mapper>
        <mapper url="C:\spring\mybatis-dev\src\main\resources.UserMapper.xml"/>
        <mapper resource="com.best.dao.UserMapper"/>
    </mappers>
```

上面这就是一个mapper的配置，告诉mybatis，你的mapper文件存放在哪里，下面我们着重看下注册mapper的整个源码过程

```java
mapperElement(root.evalNode("mappers"));
```

```java
private void mapperElement(XNode parent) throws Exception {
  if (parent != null) {
    //获取mappers的子节点mapper列表
    for (XNode child : parent.getChildren()) {
      //如果是package
      if ("package".equals(child.getName())) {
        String mapperPackage = child.getStringAttribute("name");
        //这里会扫包
        configuration.addMappers(mapperPackage);
      } else {
        //获取resource、class、url
        String resource = child.getStringAttribute("resource");
        String url = child.getStringAttribute("url");
        String mapperClass = child.getStringAttribute("class");
        //如果resource（mapper.xml的路径）不是空，其他都是空
        if (resource != null && url == null && mapperClass == null) {
          ErrorContext.instance().resource(resource);
          //获取文件流
          InputStream inputStream = Resources.getResourceAsStream(resource);
          //开始解析xml文件
          XMLMapperBuilder mapperParser = new XMLMapperBuilder(inputStream, configuration, resource, configuration.getSqlFragments());
          //开始解析mapper xml文件
          mapperParser.parse();
        //url不是空，其他都是空
        } else if (resource == null && url != null && mapperClass == null) {  
          ErrorContext.instance().resource(url);
          //也是获取文件流
          InputStream inputStream = Resources.getUrlAsStream(url);
          XMLMapperBuilder mapperParser = new XMLMapperBuilder(inputStream, configuration, url, configuration.getSqlFragments());
          mapperParser.parse();
        //class不是空，其他都是空
        } else if (resource == null && url == null && mapperClass != null) {  //获取class对象
          Class<?> mapperInterface = Resources.classForName(mapperClass);
           //直接加入
          configuration.addMapper(mapperInterface);
        } else {
          throw new BuilderException("A mapper element may only specify a url, resource or class, but not more than one.");
        }
      }
    }
  }
}
```

我们先来分析下不为空resource的情况下，创建XMLMapperBuilder对象来解析mapper文件，在new XMLMapperBuilder的过程中，会生成一个助理类

```java
public class MapperBuilderAssistant extends BaseBuilder {

  private String currentNamespace;
  private final String resource;
  private Cache currentCache;
  private boolean unresolvedCacheRef; // issue #676
  //...省略
}
```

他继承了BaseBuilder，而BaseBuilder里面存放了Configuration对象，TypeAliasRegistry--别名注册中心、TypeHandlerRegistry------类型注册中心；同时在MapperBuilderAssistant这里面还保存了缓存，当前的namespace以及加载路径等，也是比较重要的一个类，我们在接下来的分析的过程中还是会来分析它的

这里面保存了缓存，当前的namespace以及加载路径等，也是比较重要的一个类，我们在接下来的分析的过程中还是会来分析它的

```java
XMLMapperBuilder mapperParser = new XMLMapperBuilder(inputStream, configuration, resource, configuration.getSqlFragments());
mapperParser.parse();
```

解析开始

```java
public void parse() {
  //判断下是不是已经解析过这个资源了
  if (!configuration.isResourceLoaded(resource)) {
    configurationElement(parser.evalNode("/mapper"));
    configuration.addLoadedResource(resource);
    bindMapperForNamespace();
  }

  parsePendingResultMaps();
  parsePendingCacheRefs();
  parsePendingStatements();
}
```

```java
//开始解析mapper节点
configurationElement(parser.evalNode("/mapper"));

private void configurationElement(XNode context) {
    try {
      //拿到namespace
      String namespace = context.getStringAttribute("namespace");
      if (namespace == null || namespace.equals("")) {
        throw new BuilderException("Mapper's namespace cannot be empty");
      }
      //namespace设置到builderAssistant对象中
      builderAssistant.setCurrentNamespace(namespace);
      //开始解析缓存引用
      cacheRefElement(context.evalNode("cache-ref"));
      //解析缓存
      cacheElement(context.evalNode("cache"));
      //解析parameterMap节点
      parameterMapElement(context.evalNodes("/mapper/parameterMap"));
      //解析resultMap节点
      resultMapElements(context.evalNodes("/mapper/resultMap"));
      //解析sql节点
      sqlElement(context.evalNodes("/mapper/sql"));
      //解析curd节点
      buildStatementFromContext(context.evalNodes("select|insert|update|delete"));
    } catch (Exception e) {
      throw new BuilderException("Error parsing Mapper XML. The XML location is '" + resource + "'. Cause: " + e, e);
    }
  }

```

接下来我们一个个来看看

#### 3.1 cache-ref节点的解析

```java
private void cacheRefElement(XNode context) {
  if (context != null) {
     //加入到configuration中的cacheRefMap的HashMap中保存起来
     //key-value:当前Namespace ----  被引用的namespace                   
    configuration.addCacheRef(builderAssistant.getCurrentNamespace(), context.getStringAttribute("namespace"));
    //new CacheRefResolver对象
    CacheRefResolver cacheRefResolver = new CacheRefResolver(builderAssistant, context.getStringAttribute("namespace"));
    try {
      //这里是准备干啥呢？
      cacheRefResolver.resolveCacheRef();
    } catch (IncompleteElementException e) {
      //如果异常就加入到IncompleteCacheRef LinkedList中
      configuration.addIncompleteCacheRef(cacheRefResolver);
    }
  }
}
```

CacheRefResolver对象里存放了MapperBuilderAssistant和被引用的namespace  

```java
public class CacheRefResolver {
  private final MapperBuilderAssistant assistant;
  private final String cacheRefNamespace;

  public CacheRefResolver(MapperBuilderAssistant assistant, String cacheRefNamespace) {
    this.assistant = assistant;
    this.cacheRefNamespace = cacheRefNamespace;
  }

  public Cache resolveCacheRef() {
    return assistant.useCacheRef(cacheRefNamespace);
  }
}
```

```java
public Cache useCacheRef(String namespace) {
  //被引用的namespace不能为空
  if (namespace == null) {
    throw new BuilderException("cache-ref element requires a namespace attribute.");
  }
  try {
    unresolvedCacheRef = true;
    //根据namespace中的Cache对象
    Cache cache = configuration.getCache(namespace);
    //如果cache是空的就直接抛错
    if (cache == null) {
      throw new IncompleteElementException("No cache for namespace '" + namespace + "' could be found.");
    }
    //把当前的cache指向引用的namespace cache
    currentCache = cache;
    unresolvedCacheRef = false;
    return cache;
  } catch (IllegalArgumentException e) {
    //一般第一次进来这里都会异常，请看上面的configuration.addIncompleteCacheRef(cacheRefResolver);
    throw new IncompleteElementException("No cache for namespace '" + namespace + "' could be found.", e);
  }
}
```

其实想想这个xml配置是干什么用的就知道了，无非就是引用其他namespace的缓存，但是要知道，第一次进来，缓存还没有生成呢？因为cacheRef节点是第一个被解析的组件，这时候configuration.getCache(namespace);肯定是空的，所以这里抛出异常，并把CacheRefResolver对象加入到configuration中去了

```java
//如果异常就加入到IncompleteCacheRef LinkedList中
configuration.addIncompleteCacheRef(cacheRefResolver);
```

方便后续有了cache再去赋值，赋值的地方在

 XMLMapperBuilder#parsePendingCacheRefs();

#### 3.2 cache节点解析

cache，也就是对给定命名空间的缓存配置，这里的主要讲的是二级缓存，namespace级别的，开启二级缓存，需要两个配置

1. 全局开关配置

```java
<setting name="cacheEnabled" value="true"/>
```

![](mybatisimg\4.png)

> 当然默认情况下cacheEnabled就是true,这在2.7节就已经说过了

2. mapper配置cache节点

```xml
<cache eviction="LRU"
       blocking="false"
       flushInterval="60000"
       readOnly="false"
       size="1024"
       type="PERPETUAL">
   <property name="name" value="zhangsan"/>
</cache>
```

这样二级缓存就生效了，缺一不可，在<cache>节点上，有几个参数需要注意下，他们分别代表不同的意思

> 1. eviction存在如下几种表现形式（默认引用LRU）
>     typeAliasRegistry.registerAlias("PERPETUAL", PerpetualCache.class)  PERPETUAL 永久缓存
>     typeAliasRegistry.registerAlias("FIFO", FifoCache.class);      FIFO  先进先出的缓存方式
>     typeAliasRegistry.registerAlias("LRU", LruCache.class);        LRU   保存最近常用的
>     typeAliasRegistry.registerAlias("SOFT", SoftCache.class);      SOFT  软引用的缓存策略
>     typeAliasRegistry.registerAlias("WEAK", WeakCache.class);  SOFT  弱引用的缓存策略
>
> 2. blocking 是否阻塞（默认是false）
>    当指定为true时将采用BlockingCache进行封装，blocking，阻塞的意思，使用BlockingCache会在查询缓存时锁住对应的Key，
>    如果缓存命中了则会释放对应的锁，否则会在查询数据库以后再释放锁这样可以阻止并发情况下多个线程同时查询数据，
>    详情可参考BlockingCache的源码。
> 3. flushInterval (默认空)
>    （清空缓存的时间间隔）: 单位毫秒，可以被设置为任意的正整数。  默认情况是不设置，也就是没有刷新间隔，
>    缓存仅仅调用语句时刷新
> 4. readOnly 是否只读（默认fasle）
>        true代表只读，这样调用者拿到的和缓存是同一个地址引用，不安全，但是性能搞
>        fasle非只读，这样会通过缓存序列化克隆一个新的对象，安全，但是性能差，默认也是fase
> 5. size 缓存引用对象的个数(默认1024)
>      要记住你缓存的对象数目和你运行环境的可用内存资源数目。默认值是1024
> 6. type:自定义缓存策略（默认PERPETUAL，采用hashMap缓存）
>     可指定使用的缓存类，mybatis默认使用HashMap进行缓存

知道了大体意思，我们来看下解析的动作

```java
cacheElement(context.evalNode("cache"));

private void cacheElement(XNode context) throws Exception {
    if (context != null) {
        //解析type
        String type = context.getStringAttribute("type", "PERPETUAL");
        //通过别名去获取自定义缓存
        Class<? extends Cache> typeClass = typeAliasRegistry.resolveAlias(type);
        //缓存策略，默认LRU
        String eviction = context.getStringAttribute("eviction", "LRU");
        //通过别名去获取缓存策略
        Class<? extends Cache> evictionClass = typeAliasRegistry.resolveAlias(eviction);
        //获取缓存刷新节点
        Long flushInterval = context.getLongAttribute("flushInterval");
        //获取缓存引用最大数量
        Integer size = context.getIntAttribute("size");
        //是否只读，默认false，安全
        boolean readWrite = !context.getBooleanAttribute("readOnly", false);
        //是否阻塞
        boolean blocking = context.getBooleanAttribute("blocking", false);
        //获取子属性
        Properties props = context.getChildrenAsProperties();
        //通过builderAssistant来生成缓存
        builderAssistant.useNewCache(typeClass, evictionClass, flushInterval, size, readWrite, blocking, props);
    }
}
```

通过MapperBuilderAssistant类设置缓存类MapperBuilderAssistant#useNewCache

```java
public Cache useNewCache(Class<? extends Cache> typeClass,
    Class<? extends Cache> evictionClass,
    Long flushInterval,
    Integer size,
    boolean readWrite,
    boolean blocking,
    Properties props) {
  //这里cache要设置的东西比较多，而且各个缓存的成员都不确定，所以有看到Builder模式，可见mybatis的代码写的还是很不错的
  Cache cache = new CacheBuilder(currentNamespace)
      //如果type值是空的，就用PerpetualCache
      .implementation(valueOrDefault(typeClass, PerpetualCache.class))
      //加入缓存实现策略（可能存在多个），用一个list来维护cache class的
      .addDecorator(valueOrDefault(evictionClass, LruCache.class))
       //维护刷新时间
      .clearInterval(flushInterval)
      //维护缓存引用数
      .size(size)
      //是否只读
      .readWrite(readWrite)
      //是否只读
      .blocking(blocking)
      //维护属性
      .properties(props)
      //构建
      .build();
  //构建完了加入到configuration对象中
  configuration.addCache(cache);
  //并且将当前的currentCache更新成自己
  currentCache = cache;
  return cache;
}
```

在这里其实build方法是关键，因为他处理了怎么样去构建该缓存的，在这里用了建造者模式，在创建的过程中，这里有用到了装饰器模式，合理的生成了一个从内向外的一个装饰缓存

```java
public Cache build() {
  setDefaultImplementations();
  Cache cache = newBaseCacheInstance(implementation, id);
  setCacheProperties(cache);
  // 只要不是自定义的日志，基本都会走到这里
  if (PerpetualCache.class.equals(cache.getClass())) {
    for (Class<? extends Cache> decorator : decorators) {
      //包装策略
      cache = newCacheDecoratorInstance(decorator, cache);
      setCacheProperties(cache);
    }
    //开始包装各种实现
    cache = setStandardDecorators(cache);
    //如果是自定义的日志，那就值包装一次，包装成日志方式的
  } else if (!LoggingCache.class.isAssignableFrom(cache.getClass())) {
    cache = new LoggingCache(cache);
  }
  return cache;
}

 //心得，从这里可以看出mybatis的代码还是比较严谨的，就算这里在框架层来说不怎么可能为空的情况下还是设置了默认的情况，防止从其他途径来扩展使用mybatis
 private void setDefaultImplementations() {
    //这里实现默认是空的情况下，则设置一些默认实现，从这里看到
    if (implementation == null) {
      implementation = PerpetualCache.class;
      if (decorators.isEmpty()) {
        //如果缓存策略也没配置，这里就默认加入了LruCache，虽然默认就是LruCache
        decorators.add(LruCache.class);
      }
    }
  }

private Cache setStandardDecorators(Cache cache) {
    try {
      //看cache有没有setSize方法，有的话就设置下最大缓存对象个数
      MetaObject metaCache = SystemMetaObject.forObject(cache);
      if (size != null && metaCache.hasSetter("size")) {
        metaCache.setValue("size", size);
      }
       //如果存在缓存刷新时间，就继续包装成ScheduledCache
      if (clearInterval != null) {
        cache = new ScheduledCache(cache);
        ((ScheduledCache) cache).setClearInterval(clearInterval);
      }
      //如果是读写的，就包装成SerializedCache对象
      if (readWrite) {
        cache = new SerializedCache(cache);
      }
      //继续包装LoggingCache对象
      cache = new LoggingCache(cache);
      //包装成线程安全的SynchronizedCache
      cache = new SynchronizedCache(cache);
      //如果是阻塞的就包装成BlockingCache
      if (blocking) {
        cache = new BlockingCache(cache);
      }
      //最后返回被包装的对象
      return cache;
    } catch (Exception e) {
      throw new CacheException("Error building standard cache decorators.  Cause: " + e, e);
    }
  }
```

经过这样包装，最后包装的对象就是这样的

![](mybatisimg\5.png)

> *需要注意的：
>
>  在CacheBuilder#setCacheProperties(cache);的过程中，调用了((InitializingObject) cache).initialize();方法
>
> 从3.4.2版本开始，MyBatis已经支持在所有属性设置完毕以后可以调用一个初始化方法了（主要用在cache中）
>
> ```java
> /**
>  * Interface that indicate to provide a initialization method.
>  *
>  * @since 3.4.2
>  */
> public interface InitializingObject {
> 
>   /**
>    * Initialize a instance.This method will be invoked after it has set all         properties.
>    */
>   void initialize() throws Exception;
> 
> }
> ```

cahce的总结：

从整个二级缓存来看，mybatis对二级缓存的实现，用了建造者模式和装饰器模式，并且将包装的缓存设置到了

MapperBuilderAssistant#currentCache成员中方便以后应用，然后将cache加入到了configuration对象中的一个hashmap中

key - value  ===========>   cache.getId - cache

#### ~~3.3 parameterMap节点的解析（这里就不展开讨论了）~~

![](mybatisimg\6.png)

在mybatis官网上说的很清楚

已被废弃！老式风格的参数映射。更好的办法是使用内联参数，此元素可能在将来被移除

#### 3.4 resultMap节点的解析

resultMap可以说是mybatis的灵魂节点了，用好它，那就等于用好了mybatis的一些特殊而且很有用的功能

> [在官网文档上](http://www.mybatis.org/mybatis-3/zh/sqlmap-xml.html)有这么一段话
>
> 外部 resultMap 的命名引用。结果集的映射是 MyBatis 最强大的特性，如果你对其理解透彻，许多复杂映射的情形都能迎刃而解。可以使用 resultMap 或 resultType，但不能同时使用。

可以看到resultMap的重要性，所以我决定深入源码来理解resultMap，对resultMap做一个透彻的分析

```java
resultMapElements(context.evalNodes("/mapper/resultMap"));
```

```java
private void resultMapElements(List<XNode> list) throws Exception {
  //拿到所有节点，进行一个遍历 
  for (XNode resultMapNode : list) {
    try {
      resultMapElement(resultMapNode);
    } catch (IncompleteElementException e) {
      //这里是否似曾相识，就是在cache-ref解析过程中也抛出来这个异常，等cache加载完毕再去处理一次，那这里呢？
      // ignore, it will be retried
    }
  }
}
```

XMLMapperBuilder#resultMapElement(resultMapNode);

```java
private ResultMap resultMapElement(XNode resultMapNode) throws Exception {
  //这里我没看懂为啥EmptyList对象，又不能add操作，而且在接下里的操作中emptyList对象也没做啥操作，是不是写这块代码的人写的有问题呢？这参数写的很没水平
  return resultMapElement(resultMapNode, Collections.<ResultMapping> emptyList());
}
```

```java
private ResultMap resultMapElement(XNode resultMapNode, List<ResultMapping> additionalResultMappings) throws Exception {
  //单存的日志记录，后续专门找一张来讲解下mybaits的异常日志
  ErrorContext.instance().activity("processing " + resultMapNode.getValueBasedIdentifier());
  String id = resultMapNode.getStringAttribute("id",
      resultMapNode.getValueBasedIdentifier());
  String type = resultMapNode.getStringAttribute("type",
      resultMapNode.getStringAttribute("ofType",
          resultMapNode.getStringAttribute("resultType",
              resultMapNode.getStringAttribute("javaType"))));
  String extend = resultMapNode.getStringAttribute("extends");
  Boolean autoMapping = resultMapNode.getBooleanAttribute("autoMapping");
  Class<?> typeClass = resolveClass(type);
  Discriminator discriminator = null;
  List<ResultMapping> resultMappings = new ArrayList<ResultMapping>();
  resultMappings.addAll(additionalResultMappings);
  List<XNode> resultChildren = resultMapNode.getChildren();
  for (XNode resultChild : resultChildren) {
    if ("constructor".equals(resultChild.getName())) {
      processConstructorElement(resultChild, typeClass, resultMappings);
    } else if ("discriminator".equals(resultChild.getName())) {
      discriminator = processDiscriminatorElement(resultChild, typeClass, resultMappings);
    } else {
      List<ResultFlag> flags = new ArrayList<ResultFlag>();
      if ("id".equals(resultChild.getName())) {
        flags.add(ResultFlag.ID);
      }
      resultMappings.add(buildResultMappingFromContext(resultChild, typeClass, flags));
    }
  }
  ResultMapResolver resultMapResolver = new ResultMapResolver(builderAssistant, id, typeClass, extend, discriminator, resultMappings, autoMapping);
  try {
    return resultMapResolver.resolve();
  } catch (IncompleteElementException  e) {
    configuration.addIncompleteResultMap(resultMapResolver);
    throw e;
  }
}
```

