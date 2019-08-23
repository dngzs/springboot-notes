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

