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
>        要记住你缓存的对象数目和你运行环境的可用内存资源数目。默认值是1024
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
  //获取id字段
  String id = resultMapNode.getStringAttribute("id",
      resultMapNode.getValueBasedIdentifier());
  //获取类型
  String type = resultMapNode.getStringAttribute("type",
      resultMapNode.getStringAttribute("ofType",
          resultMapNode.getStringAttribute("resultType",
              resultMapNode.getStringAttribute("javaType"))));
  //获取继承的父resultMap
  String extend = resultMapNode.getStringAttribute("extends");
  //是否自动映射
  Boolean autoMapping = resultMapNode.getBooleanAttribute("autoMapping");
  //获取别名对应的class对象
  Class<?> typeClass = resolveClass(type);
  Discriminator discriminator = null;
  List<ResultMapping> resultMappings = new ArrayList<ResultMapping>();
  resultMappings.addAll(additionalResultMappings);
  List<XNode> resultChildren = resultMapNode.getChildren();
  for (XNode resultChild : resultChildren) {
     //结果映射处理构造函数
    if ("constructor".equals(resultChild.getName())) {
      processConstructorElement(resultChild, typeClass, resultMappings);
      //鉴别器的处理
    } else if ("discriminator".equals(resultChild.getName())) {
      discriminator = processDiscriminatorElement(resultChild, typeClass, resultMappings);
    } else {
      //处理<id> <reult>节点
      List<ResultFlag> flags = new ArrayList<ResultFlag>();
      if ("id".equals(resultChild.getName())) {
        //如果存在id节点，就标识下
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

##### 3.4.1 constructor

在<resultMap>中存在<constructor>节点，在官网中是这么描述的

> constructor
>
>  \- 用于在实例化类时，注入结果到构造方法中
>
> - `idArg` - ID 参数；标记出作为 ID 的结果可以帮助提高整体性能
> - `arg` - 将被注入到构造方法的一个普通结果

他是用来处理构造函数的，具体怎么来处理的，看如下的例子

```xml
<resultMap id="user_map" type="CtUser" extends="user_map_id">
        <constructor>
            <arg column="id" javaType="Long" name="id"></arg>
            <arg column="age" javaType="integer" name="age"></arg>
            <arg column="username" javaType="String" name="username">               </arg>
        </constructor>
        <result property="username" column="username"></result>
</resultMap>
```

```java
public CtUser(@Param("age")Integer age, @Param("id")Long id, @Param("username")String username) {
    this.age = age;
    this.id = id;
    this.username = username;
}
```

为了将结果注入构造方法，MyBatis 需要通过某种方式定位相应的构造方法。上面的例子中，MyBatis 一共声明了有三个形参的的构造方法，参数类型以 `java.lang.Integer`, `java.lang.Long 和    ` `java.lang.String`的顺序给出。

但是这样的体验很不要，参数的顺序要和constructor  arg的索引顺序一模一样才可以，为了解决这个问题，mybatis在从版本 3.4.3 开始，可以在==构造方法的参数上添加 `@Param` 注解==或者==使用 '-parameters' 编译选项并启用 `useActualParamName==` 选项（默认开启）来编译项目

跟入构造器的解析源码

```java
processConstructorElement(resultChild, typeClass, resultMappings);

 private void processConstructorElement(XNode resultChild, Class<?> resultType, List<ResultMapping> resultMappings) throws Exception {
    List<XNode> argChildren = resultChild.getChildren();
    //获取子属性
    for (XNode argChild : argChildren) {
      List<ResultFlag> flags = new ArrayList<ResultFlag>();
      //将所有的子节点标识为构造器
      flags.add(ResultFlag.CONSTRUCTOR);
      //如果存在idarg节点，则标识该节点是id节点
      if ("idArg".equals(argChild.getName())) {
        flags.add(ResultFlag.ID);
      }
      //将解析到的数据加入到resultMappings中去
      resultMappings.add(buildResultMappingFromContext(argChild, resultType, flags));
    }
  }
```

在下面例子中

```xml
<resultMap id="user_map" type="CtUser" extends="user_map_id" autoMapping="true">
    <constructor>
        <arg column="id" javaType="Long" name="id"></arg>
        <arg column="age" javaType="integer" name="age"></arg>
        <arg column="username" javaType="String" name="username"></arg>
    </constructor>
    <result property="username" column="username" jdbcType="VARCHAR" typeHandler="org.apache.ibatis.type.StringTypeHandler"></result>
</resultMap>
```

经过该方法处理resultMappings就已经存在三个元素了，是constructor的节点，他们被当做resultMapping来处理了，只是做了标识，标识他们为CONSTRUCTOR类型的resultMapping

#####  3.4.2  鉴别器discriminator

鉴别器其实也算是一种映射了，这里复制下官网的介绍，因为用的不多，但是有些复杂的业务可能还会用到

```xml
discriminator javaType="int" column="draft">
  <case value="1" resultType="DraftPost"/>
</discriminator>
```

有时候，一个数据库查询可能会返回多个不同的结果集（但总体上还是有一定的联系的）。 鉴别器（discriminator）元素就是被设计来应对这种情况的，另外也能处理其它情况，例如类的继承层次结构。 鉴别器的概念很好理解——它很像 Java 语言中的 switch 语句。



==鉴别器的处理比constructor复杂的多了==

```java
private Discriminator processDiscriminatorElement(XNode context, Class<?> resultType, List<ResultMapping> resultMappings) throws Exception {
  //获取鉴别器上的节点信息
  String column = context.getStringAttribute("column");
  String javaType = context.getStringAttribute("javaType");
  String jdbcType = context.getStringAttribute("jdbcType");
  String typeHandler = context.getStringAttribute("typeHandler");
  Class<?> javaTypeClass = resolveClass(javaType);
  @SuppressWarnings("unchecked")
  Class<? extends TypeHandler<?>> typeHandlerClass = (Class<? extends TypeHandler<?>>) resolveClass(typeHandler);
  JdbcType jdbcTypeEnum = resolveJdbcType(jdbcType);
  Map<String, String> discriminatorMap = new HashMap<String, String>();
  //####2标识
  for (XNode caseChild : context.getChildren()) {
    String value = caseChild.getStringAttribute("value");
    //####3标识，这里resultMap是空的，就自动生成一个标识
    //在下面的例子中：com.best.dao.UserMapper.mapper_resultMap[user_map]_discriminator_case[1] com.best.dao.UserMapper.mapper_resultMap[user_map]_discriminator_case[0]
    String resultMap = caseChild.getStringAttribute("resultMap", processNestedResultMappings(caseChild, resultMappings));
    discriminatorMap.put(value, resultMap);
  }
  //最后生成Discriminator返回（此处的生成比较复杂，会设计到递归），因为鉴别器用的不是很多，这里就不一一看源码了
  return builderAssistant.buildDiscriminator(resultType, column, javaTypeClass, jdbcTypeEnum, typeHandlerClass, discriminatorMap);
}
```

```java
public class Discriminator {

  private ResultMapping resultMapping;
  private Map<String, String> discriminatorMap;
  //省略部分代码
}
```



我们根据这个例子来分析下上面的源码

```xml
<resultMap id="user_map" type="CtUser" extends="user_map_id" autoMapping="true">
        <constructor>
            <arg column="id" javaType="Long" name="id"></arg>
            <arg column="age" javaType="integer" name="age"></arg>
            <arg column="username" javaType="String" name="username"></arg>
        </constructor>
        <result property="username" column="username" jdbcType="VARCHAR" typeHandler="org.apache.ibatis.type.StringTypeHandler"></result>
        <discriminator javaType="int" column="sex">
            <case value="1" resultType="female"></case>
            <case value="0" resultType="male"></case>
        </discriminator>
    </resultMap>
```

在上面代码中的==####2==标识处获取到两个子节点

```java
<case value="1" resultType="female"></case>
<case value="0" resultType="male"></case>
```

因为在case标签上也可以存在，所以在==####3==标识处

经过上面的处理，最后生成的鉴别器是这样的

![](mybatisimg\7.png)

##### 3.4.3 继续源码分析

经过上面构造器和鉴别器的生成，这里最终解析到了四个节点

![](mybatisimg\9.png)

其中有两个username，上面的一个是构造器，刚才说了，构造器也当做一个resultMapp节点来处理，而鉴别器当Discriminator对象存在

在这里一个resultMap节点的信息算是解析完了，下来就是组装解析到的数据

```java
ResultMapResolver resultMapResolver = new ResultMapResolver(builderAssistant, id, typeClass, extend, discriminator, resultMappings, autoMapping);
try {
  return resultMapResolver.resolve();
} catch (IncompleteElementException  e) {
  configuration.addIncompleteResultMap(resultMapResolver);
  throw e;
}
```

这里用ResultMapResolver去解析

```java
public class ResultMapResolver {
  //MapperBuilderAssistant对象我们分析过，存着当前namespace的缓存，id以及mapper文件的存在的路径
  private final MapperBuilderAssistant assistant;
  //ResultMap的id属性
  private final String id;
  //ResultMap的type类型
  private final Class<?> type;
  ////ResultMap的继承情况
  private final String extend;
  //鉴别器
  private final Discriminator discriminator;
  //子节点的ResultMapping对象
  private final List<ResultMapping> resultMappings;
  //是否自动映射
  private final Boolean autoMapping;

  public ResultMap resolve() {
    //通过mapper的助理对象去解析ResultMap
    return assistant.addResultMap(this.id, this.type, this.extend, this.discriminator, this.resultMappings, this.autoMapping);
  }

}
```

resultMapResolver#resolve()去解析

```java
public ResultMap addResultMap(
    String id,
    Class<?> type,
    String extend,
    Discriminator discriminator,
    List<ResultMapping> resultMappings,
    Boolean autoMapping) {
  //获取ResultMapId（这里拿到的都是namespace.id）
  id = applyCurrentNamespace(id, false);
  //获取继承（这里拿到的都是namespace.extend）
  extend = applyCurrentNamespace(extend, true);
  //如果继承不是空，去处理继承
  if (extend != null) {
     //一般第一次进来都是空的，和cache-ref的处理方式一样，直接抛异常
    if (!configuration.hasResultMap(extend)) {
      throw new IncompleteElementException("Could not find a parent resultmap with id '" + extend + "'");
    }
    ResultMap resultMap = configuration.getResultMap(extend);
    List<ResultMapping> extendedResultMappings = new ArrayList<ResultMapping>(resultMap.getResultMappings());
    extendedResultMappings.removeAll(resultMappings);
    // Remove parent constructor if this resultMap declares a constructor.
    boolean declaresConstructor = false;
    for (ResultMapping resultMapping : resultMappings) {
      if (resultMapping.getFlags().contains(ResultFlag.CONSTRUCTOR)) {
        declaresConstructor = true;
        break;
      }
    }
    if (declaresConstructor) {
      Iterator<ResultMapping> extendedResultMappingsIter = extendedResultMappings.iterator();
      while (extendedResultMappingsIter.hasNext()) {
        if (extendedResultMappingsIter.next().getFlags().contains(ResultFlag.CONSTRUCTOR)) {
          extendedResultMappingsIter.remove();
        }
      }
    }
    resultMappings.addAll(extendedResultMappings);
  }
  ResultMap resultMap = new ResultMap.Builder(configuration, id, type, resultMappings, autoMapping)
      .discriminator(discriminator)
      .build();
  configuration.addResultMap(resultMap);
  return resultMap;
}
```

```java

try {
      return resultMapResolver.resolve();
    } catch (IncompleteElementException  e) {
      //然后将没有加载完成的resultMap放入到configuration的incompleteResultMaps中去，该变量是个LinkedList类型的
      configuration.addIncompleteResultMap(resultMapResolver);
      throw e;
    }
```

```xml
<resultMap id="user_map" type="CtUser" extends="user_map_id" autoMapping="true">
    <constructor>
        <arg column="id" javaType="Long" name="id"></arg>
        <arg column="age" javaType="integer" name="age"></arg>
        <arg column="username" javaType="String" name="username"></arg>
    </constructor>
    <result property="username" column="username" jdbcType="VARCHAR" typeHandler="org.apache.ibatis.type.StringTypeHandler"></result>
    <discriminator javaType="int" column="sex">
        <case value="1" resultType="female"></case>
        <case value="0" resultType="male"></case>
    </discriminator>
</resultMap>

<resultMap id="user_map_id" type="CtUser">
    <id property="id" column="id" javaType="long" jdbcType="NUMERIC"></id>
</resultMap>
```

解析到这里，user_map因为在最上面，所以先解析的user_map，而他继承的user_map_id在下面，所以会抛出异常，将user_map加入到configuration的incompleteResultMaps变量中去，代表着是个半成品的resultMap

接下来就去加载user_map_id去了，和上面的代码同一个思路，这里就不多做分析了

那么到底在什么时候才能加载并处理完半成品呢

```java
public void parse() {
  if (!configuration.isResourceLoaded(resource)) {
    configurationElement(parser.evalNode("/mapper"));
    configuration.addLoadedResource(resource);
    bindMapperForNamespace();
  }
  //是的在这里处理了半成品ResultMap
  parsePendingResultMaps();
  //还记得在这里处理了半成品CacheRef吗
  parsePendingCacheRefs();
  parsePendingStatements();
}
```

```java
private void parsePendingResultMaps() {
  //拿到半成品，遍历
  Collection<ResultMapResolver> incompleteResultMaps = configuration.getIncompleteResultMaps();
  synchronized (incompleteResultMaps) {
    Iterator<ResultMapResolver> iter = incompleteResultMaps.iterator();
    while (iter.hasNext()) {
      try {
        //还是调用ResultMapResolver的resolve方法去处理的，和上面的CacheRefResolver处理cache-ref一个套路，经过MapperBuilderAssistant去处理，这样就又回到上面的代码
        iter.next().resolve();
        iter.remove();
      } catch (IncompleteElementException e) {
        // ResultMap is still missing a resource...
      }
    }
  }
}
```

不妨在把上面的代码复制下来

```java
public ResultMap addResultMap(
    String id,
    Class<?> type,
    String extend,
    Discriminator discriminator,
    List<ResultMapping> resultMappings,
    Boolean autoMapping) {
  //获取ResultMapId（这里拿到的都是namespace.id）
  id = applyCurrentNamespace(id, false);
  //获取继承（这里拿到的都是namespace.extend）
  extend = applyCurrentNamespace(extend, true);
  //如果继承不是空，去处理继承
  if (extend != null) {
     //在这里，经过上面哪一个步骤，已经这里存在了hasResultMap了
    if (!configuration.hasResultMap(extend)) {
      throw new IncompleteElementException("Could not find a parent resultmap with id '" + extend + "'");
    }
    //获取到继承的ResultMap
    ResultMap resultMap = configuration.getResultMap(extend);
    //这里会做一个去重，将子类的resultMap和父类的resultMap的子节点相同的元素一出掉
    List<ResultMapping> extendedResultMappings = new ArrayList<ResultMapping>(resultMap.getResultMappings());
    extendedResultMappings.removeAll(resultMappings);
    //这里会去除掉父类的CONSTRUCTOR节点
    boolean declaresConstructor = false;
    for (ResultMapping resultMapping : resultMappings) {
      if (resultMapping.getFlags().contains(ResultFlag.CONSTRUCTOR)) {
        declaresConstructor = true;
        break;
      }
    }
    if (declaresConstructor) {
      Iterator<ResultMapping> extendedResultMappingsIter = extendedResultMappings.iterator();
      while (extendedResultMappingsIter.hasNext()) {
        if (extendedResultMappingsIter.next().getFlags().contains(ResultFlag.CONSTRUCTOR)) {
          extendedResultMappingsIter.remove();
        }
      }
    }
    //然后加入到resultMappings
    resultMappings.addAll(extendedResultMappings);
  }
  ResultMap resultMap = new ResultMap.Builder(configuration, id, type, resultMappings, autoMapping)
      .discriminator(discriminator)
      //在这里用了build模式
      .build();
  //加入configuration中
  configuration.addResultMap(resultMap);
  return resultMap;
}
```

ResultMap对象

```java
public class ResultMap {
  private Configuration configuration;
  //ResultMap id
  private String id;
  //类型
  private Class<?> type;
  //子节点id ruslt,包括构造器
  private List<ResultMapping> resultMappings;
  //id  ResultMappings
  private List<ResultMapping> idResultMappings;
  //构造器ResultMappings
  private List<ResultMapping> constructorResultMappings;
  //除了constructor剩下的都是propertyResult，包括id  ResultMapping
  private List<ResultMapping> propertyResultMappings;
  //所有子节点的column属性字段值
  private Set<String> mappedColumns;
  //所有子节点的property属性字段值 
  private Set<String> mappedProperties;
  //鉴别器
  private Discriminator discriminator;
  //是否嵌套的ResultMaps
  private boolean hasNestedResultMaps;
  //是否是检讨查询
  private boolean hasNestedQueries;
  //是否自动映射
  private Boolean autoMapping;
  //省略部分源码
}
```

ResultMap.Builder#build

```java
public ResultMap build() {
  if (resultMap.id == null) {
    throw new IllegalArgumentException("ResultMaps must have an id");
  }
  resultMap.mappedColumns = new HashSet<String>();
  resultMap.mappedProperties = new HashSet<String>();
  resultMap.idResultMappings = new ArrayList<ResultMapping>();
  resultMap.constructorResultMappings = new ArrayList<ResultMapping>();
  resultMap.propertyResultMappings = new ArrayList<ResultMapping>();
  final List<String> constructorArgNames = new ArrayList<String>();
  for (ResultMapping resultMapping : resultMap.resultMappings) {
    resultMap.hasNestedQueries = resultMap.hasNestedQueries || resultMapping.getNestedQueryId() != null;
    resultMap.hasNestedResultMaps = resultMap.hasNestedResultMaps || (resultMapping.getNestedResultMapId() != null && resultMapping.getResultSet() == null);
    final String column = resultMapping.getColumn();
    if (column != null) {
      resultMap.mappedColumns.add(column.toUpperCase(Locale.ENGLISH));
    } else if (resultMapping.isCompositeResult()) {
      for (ResultMapping compositeResultMapping : resultMapping.getComposites()) {
        final String compositeColumn = compositeResultMapping.getColumn();
        if (compositeColumn != null) {
          resultMap.mappedColumns.add(compositeColumn.toUpperCase(Locale.ENGLISH));
        }
      }
    }
    final String property = resultMapping.getProperty();
    if(property != null) {
      resultMap.mappedProperties.add(property);
    }
    if (resultMapping.getFlags().contains(ResultFlag.CONSTRUCTOR)) {
      resultMap.constructorResultMappings.add(resultMapping);
      if (resultMapping.getProperty() != null) {
        constructorArgNames.add(resultMapping.getProperty());
      }
    } else {
      resultMap.propertyResultMappings.add(resultMapping);
    }
    if (resultMapping.getFlags().contains(ResultFlag.ID)) {
      resultMap.idResultMappings.add(resultMapping);
    }
  }
  if (resultMap.idResultMappings.isEmpty()) {
    resultMap.idResultMappings.addAll(resultMap.resultMappings);
  }
  //---------------------------------------------------------------
  //这里会判断参数类型，参数名称等（actualParamNames，就是<arg name="username"></arg>的name字段,如果不加name字段，就默认按照字段的顺序来）
  if (!constructorArgNames.isEmpty()) {
    final List<String> actualArgNames = argNamesOfMatchingConstructor(constructorArgNames);
    if (actualArgNames == null) {
      throw new BuilderException("Error in result map '" + resultMap.id
          + "'. Failed to find a constructor in '"
          + resultMap.getType().getName() + "' by arg names " + constructorArgNames
          + ". There might be more info in debug log.");
    }
    Collections.sort(resultMap.constructorResultMappings, new Comparator<ResultMapping>() {
      @Override
      public int compare(ResultMapping o1, ResultMapping o2) {
        int paramIdx1 = actualArgNames.indexOf(o1.getProperty());
        int paramIdx2 = actualArgNames.indexOf(o2.getProperty());
        return paramIdx1 - paramIdx2;
      }
    });
  }
  // 为了防止用户修改生成的resultMap中的数据，mybaits返回一个不可以修改的resultMap给用户
  resultMap.resultMappings = Collections.unmodifiableList(resultMap.resultMappings);
  resultMap.idResultMappings = Collections.unmodifiableList(resultMap.idResultMappings);
  resultMap.constructorResultMappings = Collections.unmodifiableList(resultMap.constructorResultMappings);
  resultMap.propertyResultMappings = Collections.unmodifiableList(resultMap.propertyResultMappings);
  resultMap.mappedColumns = Collections.unmodifiableSet(resultMap.mappedColumns);
  return resultMap;
}
```

在上述代码中，----上面的代码就不看了，就是给ResultMap赋值的，主要来看下构造器的参数映射部分的代码

```java
final List<String> actualArgNames = argNamesOfMatchingConstructor(constructorArgNames);
```

ResultMap#argNamesOfMatchingConstructor

```java
private List<String> argNamesOfMatchingConstructor(List<String> constructorArgNames) {
  //获取ResultMap类型的所有public的构造方法
  Constructor<?>[] constructors = resultMap.type.getDeclaredConstructors();
  for (Constructor<?> constructor : constructors) {
    Class<?>[] paramTypes = constructor.getParameterTypes();
    //如果参数个数相同，继续分析
    if (constructorArgNames.size() == paramTypes.length) {
      List<String> paramNames = getArgNames(constructor);
       //判断constructorArgNames是否都在参数名称里并且
        
      if (constructorArgNames.containsAll(paramNames)
          && argTypesMatch(constructorArgNames, paramTypes, paramNames)) {
        return paramNames;
      }
    }
  }
  return null;
}
```

完成上面的步骤后，就基本完成了rsultMap的创建了，==最后将resultMap加入configuration中去==

configuration#addResultMap(resultMap);

```java
public void addResultMap(ResultMap rm) {
  //完成了添加之后
  resultMaps.put(rm.getId(), rm);
  // 检查本resultMap内的鉴别器有没有嵌套resultMap
  checkLocallyForDiscriminatedNestedResultMaps(rm);
  //检查所有resultMap的鉴别器有没有嵌套resultMap
  checkGloballyForDiscriminatedNestedResultMaps(rm);
}
```

应该来说，设置resultMap的鉴别器有没有嵌套的resultMap在解析resultMap子元素的时候就可以设置，当然放在最后统一处理也未尝不可，也不见得放在这里就一定更加清晰，只能说实现的方式有多种。

最后看下生成后的截图

![](C:\Users\bg317957\Desktop\springboot-notes\mybatisimg\8.png)

==可以看到，基本都是一个全类名，一个简单命名，这是因为mybatis复写了hashmap,在放入的同时，放入了一个简单的key==

```java
protected static class StrictMap<V> extends HashMap<String, V>
```

到此为止，一个根resultMap的解析就完整的结束了。不得不说resutMap的实现确实是很复杂。至于用法，resultMap的用法可以说可以非常的复杂，但是用好却是不容易啊

最后我们附上官网对于[resultMap的介绍](http://www.mybatis.org/mybatis-3/zh/sqlmap-xml.html)，可以看看怎么使用的

##### 3.4.4  sql节点的解析

sql节点的作用：用来定义可重用的 SQL 代码段

```xml
<sql id="user">age,usernmae,id</sql>
```

解析sql的源码也比较简单，来看看

```java
private void sqlElement(List<XNode> list) throws Exception {
  if (configuration.getDatabaseId() != null) {
    sqlElement(list, configuration.getDatabaseId());
  }
  sqlElement(list, null);
}
```

```java
private void sqlElement(List<XNode> list) throws Exception {
  if (configuration.getDatabaseId() != null) {
    sqlElement(list, configuration.getDatabaseId());
  }
  sqlElement(list, null);
}
```

```java
private void sqlElement(List<XNode> list, String requiredDatabaseId) throws Exception {
  for (XNode context : list) {
    String databaseId = context.getStringAttribute("databaseId");
    String id = context.getStringAttribute("id");
    id = builderAssistant.applyCurrentNamespace(id, false);
    if (databaseIdMatchesCurrent(id, databaseId, requiredDatabaseId)) {
      //将解析到的xml加入到sqlFragments变量中
      sqlFragments.put(id, context);
    }
  }
}
```

值得注意的是存入了

XMLMapperBuilder#  Map<String, XNode> sqlFragments，这个sqlFragments其实在创建的时候就是拿的configuration的sqlFragments引用，于是这里存入就是存入了configuration的sqlFragments中去了

##### 3.4.5  解析sql crud语句

mybatis作为一个orm框架，那么最重要的就是增删改查了，这也是mybaits最有价值的部分，mybatis前面这些组件铺垫了这么久，就是为了这一刻。为了研究清楚mybaits对于crud的实现，我决定深入crud的源码进行详细了解不理解这一块，那么真正的使用起来只能证明你不是一个老油条，更谈不上对mybatis有深刻的理解了。

这一部分，我决定用源码+图示的方法来理解最重要的一刻

mybaits的crud是从这里开始的，那我就从这里开始继续解析源码

```java
buildStatementFromContext(context.evalNodes("select|insert|update|delete"));
```

```java
private void buildStatementFromContext(List<XNode> list) {
  if (configuration.getDatabaseId() != null) {
    buildStatementFromContext(list, configuration.getDatabaseId());
  }
  buildStatementFromContext(list, null);
}

private void buildStatementFromContext(List<XNode> list, String requiredDatabaseId) {
  for (XNode context : list) {
    //在这里生成XMLStatementBuilder开始解析sql
    final XMLStatementBuilder statementParser = new XMLStatementBuilder(configuration, builderAssistant, context, requiredDatabaseId);
    try {
      //开始解析
      statementParser.parseStatementNode();
    } catch (IncompleteElementException e) {
      configuration.addIncompleteStatement(statementParser);
    }
  }
}
```

接下来statementParser.parseStatementNode();就是我们看的重点，这里面包含了sql的解析过程

```java
public void parseStatementNode() {
  //获取crud的id
  String id = context.getStringAttribute("id");
  String databaseId = context.getStringAttribute("databaseId");

  if (!databaseIdMatchesCurrent(id, databaseId, this.requiredDatabaseId)) {
    return;
  }
    
  //获取mybaits最基本的一些信息，这些信息是我们经常写的，所以就不一一过了
  Integer fetchSize = context.getIntAttribute("fetchSize");
  Integer timeout = context.getIntAttribute("timeout");
  String parameterMap = context.getStringAttribute("parameterMap");
  String parameterType = context.getStringAttribute("parameterType");
  Class<?> parameterTypeClass = resolveClass(parameterType);
  String resultMap = context.getStringAttribute("resultMap");
  String resultType = context.getStringAttribute("resultType");
    
 //MyBatis从3.2开始支持可插拔的脚本语言，因此你可以在插入一种语言的驱动（language driver）之后来写基于这种语言的动态 SQL 查询
  String lang = context.getStringAttribute("lang");
  
  //这里如果你没有定义，那么lang值就是空的，mybaits在Configuration初始化的时候默认注册了XMLLanguageDriver.class，除了他，还注册了languageRegistry.register(RawLanguageDriver.class);
  LanguageDriver langDriver = getLanguageDriver(lang);

 
  Class<?> resultTypeClass = resolveClass(resultType);
  String resultSetType = context.getStringAttribute("resultSetType");
  //解析crud语句的类型,STATEMENT，PREPARED 或 CALLABLE 的一个。这会让 MyBatis 分别使用 Statement，PreparedStatement 或 CallableStatement，默认值：PREPARED。
  StatementType statementType = StatementType.valueOf(context.getStringAttribute("statementType", StatementType.PREPARED.toString()));
  ResultSetType resultSetTypeEnum = resolveResultSetType(resultSetType);
  //解析SQL是什么类型的，目前主要有UNKNOWN, INSERT, UPDATE, DELETE, SELECT, FLUSH
  String nodeName = context.getNode().getNodeName();
  SqlCommandType sqlCommandType = SqlCommandType.valueOf(nodeName.toUpperCase(Locale.ENGLISH));
  //判断是不是select节点
  boolean isSelect = sqlCommandType == SqlCommandType.SELECT;
   
  //如果不是select节点，flushCache默认设置成true
  boolean flushCache = context.getBooleanAttribute("flushCache", !isSelect);
  
  //select是否使用缓存
  boolean useCache = context.getBooleanAttribute("useCache", isSelect);
    
  //对于嵌套查询才有用，后面在分析sql执行源码的时候，再去分析它的作用
  boolean resultOrdered = context.getBooleanAttribute("resultOrdered", false);

  // 先把sql片段中的<inclued处理了>
  //<include refid="${user_table}">
  XMLIncludeTransformer includeParser = new XMLIncludeTransformer(configuration, builderAssistant);
  includeParser.applyIncludes(context.getNode());

  //在处理完include后，处理SelectKey节点（）
  processSelectKeyNodes(id, parameterTypeClass, langDriver);
  
  // Parse the SQL (pre: <selectKey> and <include> were parsed and removed)
  SqlSource sqlSource = langDriver.createSqlSource(configuration, context, parameterTypeClass);
  String resultSets = context.getStringAttribute("resultSets");
  String keyProperty = context.getStringAttribute("keyProperty");
  String keyColumn = context.getStringAttribute("keyColumn");
  KeyGenerator keyGenerator;
  String keyStatementId = id + SelectKeyGenerator.SELECT_KEY_SUFFIX;
  keyStatementId = builderAssistant.applyCurrentNamespace(keyStatementId, true);
  if (configuration.hasKeyGenerator(keyStatementId)) {
    keyGenerator = configuration.getKeyGenerator(keyStatementId);
  } else {
    keyGenerator = context.getBooleanAttribute("useGeneratedKeys",
        configuration.isUseGeneratedKeys() && SqlCommandType.INSERT.equals(sqlCommandType))
        ? Jdbc3KeyGenerator.INSTANCE : NoKeyGenerator.INSTANCE;
  }

  builderAssistant.addMappedStatement(id, sqlSource, statementType, sqlCommandType,
      fetchSize, timeout, parameterMap, parameterTypeClass, resultMap, resultTypeClass,
      resultSetTypeEnum, flushCache, useCache, resultOrdered, 
      keyGenerator, keyProperty, keyColumn, databaseId, langDriver, resultSets);
}
```



这里我们知道下面这几个对象需要注意下，我们一一来看

###### 3.4.5.1 LanguageDriver

1. LanguageDriver：处理sql的语言驱动器

​      我们看下继承结构

![](mybatisimg\10.png)

语言驱动器非常重要，在mybait中，默认情况下使用的XMLLanguageDriver驱动器，如果你觉得mybaits的语言驱动器不好用，那么可以自定义，这就是mybaits留给你的扩展

而RawLanguageDriver驱动器是继承自XMLLanguageDriver，作用就是检查如果sql语句存在动态语句，直接抛异常，除此之外，mybaits还提供了一些驱动，有兴趣可以去看看，官方地址[戳这里](https://github.com/mybatis)

![](mybatisimg\11.png)

话不多说，既然默认的是XMLLanguageDriver，那么我们就看看mybaits是怎么解析sql语句的，这里就是重点了

XMLLanguageDriver#createSqlSource

```java
@Override
public SqlSource createSqlSource(Configuration configuration, XNode script, Class<?> parameterType) {
  XMLScriptBuilder builder = new XMLScriptBuilder(configuration, script, parameterType);
  return builder.parseScriptNode();
}
```

mybaits通过XMLScriptBuilder来解析sql语句，在XMLScriptBuilder的初始化阶段，就调用了==initNodeHandlerMap==方法，初始化了解析的一些动态的标签的NodeHandler，并且每个NodeHandler都有自己对应的SqlNode，比如 IfHandler---对应IfSqlNode，为了防止外部使用，mybaits将所有的NodeHandler全部设置成了私有的，并且是在XMLScriptBuilder的私有内部类

```java
public class XMLScriptBuilder extends BaseBuilder {

  private final XNode context;
  private boolean isDynamic;
  private final Class<?> parameterType;
  private final Map<String, NodeHandler> nodeHandlerMap = new HashMap<String, NodeHandler>();

  public XMLScriptBuilder(Configuration configuration, XNode context, Class<?> parameterType) {
    super(configuration);
    this.context = context;
    this.parameterType = parameterType;
    initNodeHandlerMap();
  }
  //将各个NodeHandler初始化，放入map中
  private void initNodeHandlerMap() {
    nodeHandlerMap.put("trim", new TrimHandler());
    nodeHandlerMap.put("where", new WhereHandler());
    nodeHandlerMap.put("set", new SetHandler());
    nodeHandlerMap.put("foreach", new ForEachHandler());
    nodeHandlerMap.put("if", new IfHandler());
    nodeHandlerMap.put("choose", new ChooseHandler());
    nodeHandlerMap.put("when", new IfHandler());
    nodeHandlerMap.put("otherwise", new OtherwiseHandler());
    nodeHandlerMap.put("bind", new BindHandler());
  }



  private class IfHandler implements NodeHandler {
    public IfHandler() {
      // Prevent Synthetic Access
    }

    @Override
    public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
      MixedSqlNode mixedSqlNode = parseDynamicTags(nodeToHandle);
      String test = nodeToHandle.getStringAttribute("test");
      IfSqlNode ifSqlNode = new IfSqlNode(mixedSqlNode, test);
      targetContents.add(ifSqlNode);
    }
  }
//省略大部分代码

}
```

准备工作做好了，开始解析XMLScriptBuilder#parseScriptNode()

```java
public SqlSource parseScriptNode() {
  //可以看到parseDynamicTags之后会生成一个MixedSqlNode，而MixedSqlNode存放了一系列的SqlNode
  MixedSqlNode rootSqlNode = parseDynamicTags(context);
  SqlSource sqlSource = null;
  if (isDynamic) {
    sqlSource = new DynamicSqlSource(configuration, rootSqlNode);
  } else {
    sqlSource = new RawSqlSource(configuration, rootSqlNode, parameterType);
  }
  return sqlSource;
}
```

来看下mixedSqlNode怎么解析出来的

XMLScriptBuilder#parseDynamicTags()

```java
protected MixedSqlNode parseDynamicTags(XNode node) {
  //初始化一个sqlnode的list，这是生成MixedSqlNode的根本
  List<SqlNode> contents = new ArrayList<SqlNode>();
  //拿到select节点下的所有节点，开始遍历
  NodeList children = node.getNode().getChildNodes();
  for (int i = 0; i < children.getLength(); i++) {
    XNode child = node.newXNode(children.item(i));
     //判断子节点是不是cdate或者是text类型的
    if (child.getNode().getNodeType() == Node.CDATA_SECTION_NODE || child.getNode().getNodeType() == Node.TEXT_NODE) {
      //拿到sql语句
      String data = child.getStringBody("");
      //将解析到的sql放入TextSqlNode中（TextSqlNode算是sqlNode中实现比较复杂的一个了）
      TextSqlNode textSqlNode = new TextSqlNode(data);
      //解析是不是动态sql（判断是不是动态sql的标准是sql中存在${}否）
      if (textSqlNode.isDynamic()) {
        //如果是动态的，就将isDynamic标识，一条是动态sql，那么这个select下的sql就是动态的了
        contents.add(textSqlNode);
        isDynamic = true;
      } else {
        //如果不是就包装成一个静态sql(这里的data是解析到的sql语句)添加到contents中去
        contents.add(new StaticTextSqlNode(data));
      }
      //如果sql中存在<if><where><trim>等等的标签，那么走的下面
    } else if (child.getNode().getNodeType() == Node.ELEMENT_NODE) { // issue #628
       //获取是什么类型的标签（假设这里获取到的是if标签）
      String nodeName = child.getNode().getNodeName();
      //还记得我们在初始化XMLScriptBuilder的时候，往nodeHandlerMap丢入的各种NodeHandler吗，根据名字拿出来，那么这里拿到的就是IfHandler
      NodeHandler handler = nodeHandlerMap.get(nodeName);
      if (handler == null) {
        throw new BuilderException("Unknown element <" + nodeName + "> in SQL statement.");
      }
      //处理,跳到2.3.5.3节，看是怎么处理的
      handler.handleNode(child, contents);
      //如果sql中存在一些标签，我们也认为这个sql是动动态的，标注起来
      isDynamic = true;
    }
  }
  //最后再封装成一个MixedSqlNode sqlNode返回获取
  return new MixedSqlNode(contents);
}
```

这里我们总结下，其实select下的sql我们在写的时候是比较复杂的，可能会存在多种多样的形式，比如加了<if><where><choose>等等的标签，所以解析的过程要不断的递归，最后得到一个MixedSqlNode返回回去，这里用到了sqlNode和NodeHandler两个接口，NodeHandler处理怎么生成sqlNode，并将其加入到MixedSqlNode后返回，而MixedSqlNode我们知道，成员是一个List<SqlNode>，来存放整个select节点遍历完的所有节点经过NodeHandler处理返回的sqlNode的一个集合，拿到这个集合。在这里有个知识点：

判断动态sql的标准：

1. 有没有${}
2. sql中有没有<if><where>..等等的标签

二者其中有一个是就是动态sql

在接着看，搬过来上面的代码

```java
public SqlSource parseScriptNode() {
  //可以看到parseDynamicTags之后会生成一个MixedSqlNode，而MixedSqlNode存放了一系列的SqlNode
  MixedSqlNode rootSqlNode = parseDynamicTags(context);
  SqlSource sqlSource = null;
  //如果是动态的sql就生成一个DynamicSqlSource
  if (isDynamic) {
    sqlSource = new DynamicSqlSource(configuration, rootSqlNode);
  } else {
    //如果是动态的就生成一个RawSqlSource
    sqlSource = new RawSqlSource(configuration, rootSqlNode, parameterType);
  }
  return sqlSource;
}
```



###### 3.4.5.2  sqlNode（sql节点的接口）

这里又一个很重要的接口sqlNode,下面这是sqlNode的实现

> StaticTextSqlNode (org.apache.ibatis.scripting.xmltags)
> MixedSqlNode (org.apache.ibatis.scripting.xmltags)
> TextSqlNode (org.apache.ibatis.scripting.xmltags)
> ForEachSqlNode (org.apache.ibatis.scripting.xmltags)
> IfSqlNode (org.apache.ibatis.scripting.xmltags)
> VarDeclSqlNode (org.apache.ibatis.scripting.xmltags)
> TrimSqlNode (org.apache.ibatis.scripting.xmltags)
>     WhereSqlNode (org.apache.ibatis.scripting.xmltags)
>     SetSqlNode (org.apache.ibatis.scripting.xmltags)
> ChooseSqlNode (org.apache.ibatis.scripting.xmltags)



**MixedSqlNode**

```java
public class MixedSqlNode implements SqlNode {
 
  private final List<SqlNode> contents;

  public MixedSqlNode(List<SqlNode> contents) {
    this.contents = contents;
  }

  @Override
  public boolean apply(DynamicContext context) {
    for (SqlNode sqlNode : contents) {
      sqlNode.apply(context);
    }
    return true;
  }
}
```

**TextSqlNode**

```java
public class TextSqlNode implements SqlNode {
  private String text;

  public TextSqlNode(String text) {
    this.text = text;
  }
  //判断是不是动态sql,用的GenericTokenParser来解析的，后面会有这些工具的介绍和详解
  public boolean isDynamic() {
    DynamicCheckerTokenParser checker = new DynamicCheckerTokenParser();
    GenericTokenParser parser = createParser(checker);
    parser.parse(text);
    return checker.isDynamic();
  }

  public boolean apply(DynamicContext context) {
    
    GenericTokenParser parser = createParser(new BindingTokenParser(context));
    context.appendSql(parser.parse(text));
    return true;
  }
  
  private GenericTokenParser createParser(TokenHandler handler) {
    //判断是不是动态的标准是有没有${}
    return new GenericTokenParser("${", "}", handler);
  }

  private static class BindingTokenParser implements TokenHandler {

    private DynamicContext context;

    public BindingTokenParser(DynamicContext context) {
      this.context = context;
    }

    public String handleToken(String content) {
      Object parameter = context.getBindings().get("_parameter");
      if (parameter == null) {
        context.getBindings().put("value", null);
      } else if (SimpleTypeRegistry.isSimpleType(parameter.getClass())) {
        context.getBindings().put("value", parameter);
      }
      Object value = OgnlCache.getValue(content, context.getBindings());
      return (value == null ? "" : String.valueOf(value)); // issue #274 return "" instead of "null"
    }
  }

  private static class DynamicCheckerTokenParser implements TokenHandler {
    //就存了一份是不是动态的标识
    private boolean isDynamic;

    public boolean isDynamic() {
      return isDynamic;
    }

    public String handleToken(String content) {
      this.isDynamic = true;
      return null;
    }
  }
  
}
```

**StaticTextSqlNode**

```java
public class StaticTextSqlNode implements SqlNode {
  //sql语句
  private final String text;

  public StaticTextSqlNode(String text) {
    this.text = text;
  }

  @Override
  public boolean apply(DynamicContext context) {
    //通过DynamicContext#appendSql将sql拼接起来
    context.appendSql(text);
    return true;
  }

}
```

**IfSqlNode**

```java
public class IfSqlNode implements SqlNode {
 
  private ExpressionEvaluator evaluator;
  //test的值
  private String test;
  //if里面解析到的sql（这里的sqlNode一般是MixedSqlNode类型的，我们知道MixedSqlNode是个list）
  private SqlNode contents;

  public IfSqlNode(SqlNode contents, String test) {
    this.test = test;
    this.contents = contents;
    //初始化的时候并且生成ognl处理类ExpressionEvaluator,可见test的值是用正则处理的，也就是test里可以写正则表达式
    this.evaluator = new ExpressionEvaluator();
  }

  public boolean apply(DynamicContext context) {
    if (evaluator.evaluateBoolean(test, context.getBindings())) {
      contents.apply(context);
      return true;
    }
    return false;
  }

}
```



###### 2.3.5.3 NodeHandler（生成sql节点的接口）

![](mybatisimg\12.png)

**IfHandler**

```java
private class IfHandler implements NodeHandler {
  public IfHandler() {
    // Prevent Synthetic Access
  }

  @Override
  public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
    //又去XMLScriptBuilder#parseDynamicTags递归解析一次，直到解析<if>标签解析到干净的sql
    MixedSqlNode mixedSqlNode = parseDynamicTags(nodeToHandle);
    //获取test属性值
    String test = nodeToHandle.getStringAttribute("test");
    //将test属性值和生成sql语句包装成IfSqlNode
    IfSqlNode ifSqlNode = new IfSqlNode(mixedSqlNode, test);
    //加入到targetContents去中
    targetContents.add(ifSqlNode);
  }
}
```

###### 2.3.5.4 SqlSource接口（sql的表述）

> ProviderSqlSource (org.apache.ibatis.builder.annotation)
> StaticSqlSource (org.apache.ibatis.builder)
> DynamicSqlSource (org.apache.ibatis.scripting.xmltags)
> BoundSqlSqlSource in PaginationInterceptor (com.oasis.captain.dao.core.plugin.pagination)
> RawSqlSource (org.apache.ibatis.scripting.defaults)

**DynamicSqlSource**

```java
public class DynamicSqlSource implements SqlSource {

 
  private final Configuration configuration;
  //生成的SqlNode集合（一般是MixedSqlNode对象）
  private final SqlNode rootSqlNode;

  //很简单，就保存到成员里
  public DynamicSqlSource(Configuration configuration, SqlNode rootSqlNode) {
    this.configuration = configuration;
    this.rootSqlNode = rootSqlNode;
  }

  @Override
  public BoundSql getBoundSql(Object parameterObject) {
    DynamicContext context = new DynamicContext(configuration, parameterObject);
    rootSqlNode.apply(context);
    SqlSourceBuilder sqlSourceParser = new SqlSourceBuilder(configuration);
    Class<?> parameterType = parameterObject == null ? Object.class : parameterObject.getClass();
    SqlSource sqlSource = sqlSourceParser.parse(context.getSql(), parameterType, context.getBindings());
    BoundSql boundSql = sqlSource.getBoundSql(parameterObject);
    for (Map.Entry<String, Object> entry : context.getBindings().entrySet()) {
      boundSql.setAdditionalParameter(entry.getKey(), entry.getValue());
    }
    return boundSql;
  }

}
```

**RawSqlSource**

```java
/**
 * 静态sql. 他比 {@link DynamicSqlSource}要快，因为在项目启动的时候他已经拿到sql语句了
 * 
 * @since 3.2.0（从这个版本，看的出来绝对是优化的，因为静态sql根本不需要去计算，启动的时候就可以拿到，加快sql效率）
 */
public class RawSqlSource implements SqlSource {

  //经过SqlSourceBuilder的解析这里最终拿到的是一个经过处理的（带问号的，并且有参数映射的）StaticSqlSource
  private final SqlSource sqlSource;

  public RawSqlSource(Configuration configuration, SqlNode rootSqlNode, Class<?> parameterType) {
    //通过getSql拿到sql
    this(configuration, getSql(configuration, rootSqlNode), parameterType);
  }

  public RawSqlSource(Configuration configuration, String sql, Class<?> parameterType) {
    //生成SqlSourceBuilder
    SqlSourceBuilder sqlSourceParser = new SqlSourceBuilder(configuration);
    //获取参数类型，如果是空的，就放入object.class
    Class<?> clazz = parameterType == null ? Object.class : parameterType;
    sqlSource = sqlSourceParser.parse(sql, clazz, new HashMap<String, Object>());
  }

  private static String getSql(Configuration configuration, SqlNode rootSqlNode) {
    //通过DynamicContext来获取sql
    DynamicContext context = new DynamicContext(configuration, null);
    //SqlNode来拼接sql到DynamicContext中
    rootSqlNode.apply(context);
    //拿到sql
    return context.getSql();
  }

  @Override
  public BoundSql getBoundSql(Object parameterObject) {
    return sqlSource.getBoundSql(parameterObject);
  }

}
```

静态sql，在mybaits启动的时候就已经获取到sql了



**StaticSqlSource**

处理完的sql都会生成StaticSqlSource，不管静态的sql还是动态的sql

```java
public class StaticSqlSource implements SqlSource {
  //最终的sql语句
  private final String sql;
  //参数映射（此处是参数的描述信息，比如字段名、javatype jdbcType等等）
  private final List<ParameterMapping> parameterMappings;
  //Configuration对象
  private final Configuration configuration;

  public StaticSqlSource(Configuration configuration, String sql) {
    this(configuration, sql, null);
  }

  public StaticSqlSource(Configuration configuration, String sql, List<ParameterMapping> parameterMappings) {
    this.sql = sql;
    this.parameterMappings = parameterMappings;
    this.configuration = configuration;
  }

  @Override
  public BoundSql getBoundSql(Object parameterObject) {
    return new BoundSql(configuration, sql, parameterMappings, parameterObject);
  }

}
```



###### 2.3.5.5 DynamicContext（mybaits上下文）

```java
public class DynamicContext {

  public static final String PARAMETER_OBJECT_KEY = "_parameter";
  public static final String DATABASE_ID_KEY = "_databaseId";

  static {
    OgnlRuntime.setPropertyAccessor(ContextMap.class, new ContextAccessor());
  }

  private final ContextMap bindings;
  //通过sqlBuilder来拼接获取最终的sql
  private final StringBuilder sqlBuilder = new StringBuilder();
  private int uniqueNumber = 0;

  //
  public DynamicContext(Configuration configuration, Object parameterObject) {
    if (parameterObject != null && !(parameterObject instanceof Map)) {
      MetaObject metaObject = configuration.newMetaObject(parameterObject);
      bindings = new ContextMap(metaObject);
    } else {
      //ContextMap，又是一个对弈hashmap的封装
      bindings = new ContextMap(null);
    }
    //ContextMap bindings加入_parameter和_databaseId，后续参数设置要用到（静态sql这里设置的是空）
    bindings.put(PARAMETER_OBJECT_KEY, parameterObject);
    bindings.put(DATABASE_ID_KEY, configuration.getDatabaseId());
  }

  public Map<String, Object> getBindings() {
    return bindings;
  }

  public void bind(String name, Object value) {
    bindings.put(name, value);
  }

  //拼接sql
  public void appendSql(String sql) {
    sqlBuilder.append(sql);
    sqlBuilder.append(" ");
  }

  public String getSql() {
    return sqlBuilder.toString().trim();
  }

  public int getUniqueNumber() {
    return uniqueNumber++;
  }

  static class ContextMap extends HashMap<String, Object> {
    private static final long serialVersionUID = 2977601501966151582L;

    private MetaObject parameterMetaObject;
    public ContextMap(MetaObject parameterMetaObject) {
      this.parameterMetaObject = parameterMetaObject;
    }

    @Override
    public Object get(Object key) {
      String strKey = (String) key;
      if (super.containsKey(strKey)) {
        return super.get(strKey);
      }

      if (parameterMetaObject != null) {
        // issue #61 do not modify the context when reading
        return parameterMetaObject.getValue(strKey);
      }

      return null;
    }
  }

  static class ContextAccessor implements PropertyAccessor {

    @Override
    public Object getProperty(Map context, Object target, Object name)
        throws OgnlException {
      Map map = (Map) target;

      Object result = map.get(name);
      if (map.containsKey(name) || result != null) {
        return result;
      }

      Object parameterObject = map.get(PARAMETER_OBJECT_KEY);
      if (parameterObject instanceof Map) {
        return ((Map)parameterObject).get(name);
      }

      return null;
    }

    @Override
    public void setProperty(Map context, Object target, Object name, Object value)
        throws OgnlException {
      Map<Object, Object> map = (Map<Object, Object>) target;
      map.put(name, value);
    }

    @Override
    public String getSourceAccessor(OgnlContext arg0, Object arg1, Object arg2) {
      return null;
    }

    @Override
    public String getSourceSetter(OgnlContext arg0, Object arg1, Object arg2) {
      return null;
    }
  }
```

###### 2.3.5.6  SqlSourceBuilder （生成最终的sql，并且生成一个StaticSqlSource）

```java
public class SqlSourceBuilder extends BaseBuilder {

  private static final String parameterProperties = "javaType,jdbcType,mode,numericScale,resultMap,typeHandler,jdbcTypeName";

  public SqlSourceBuilder(Configuration configuration) {
    super(configuration);
  }

  public SqlSource parse(String originalSql, Class<?> parameterType, Map<String, Object> additionalParameters) {
    //生成参数映射处理器
    ParameterMappingTokenHandler handler = new ParameterMappingTokenHandler(configuration, parameterType, additionalParameters);
    //看样子是要处理#{}
    GenericTokenParser parser = new GenericTokenParser("#{", "}", handler);
    //吃力完后，这边就返回一个select * from ct_user where id = ?
    String sql = parser.parse(originalSql);
    //然后生成一个静态sqlSource
    return new StaticSqlSource(configuration, sql, handler.getParameterMappings());
  }

  private static class ParameterMappingTokenHandler extends BaseBuilder implements TokenHandler {

    private List<ParameterMapping> parameterMappings = new ArrayList<ParameterMapping>();
    private Class<?> parameterType;
    private MetaObject metaParameters;

    public ParameterMappingTokenHandler(Configuration configuration, Class<?> parameterType, Map<String, Object> additionalParameters) {
      super(configuration);
      this.parameterType = parameterType;
      this.metaParameters = configuration.newMetaObject(additionalParameters);
    }

    public List<ParameterMapping> getParameterMappings() {
      return parameterMappings;
    }

    @Override
    public String handleToken(String content) {
      //解析参数映射，将解析的参数放入到 List<ParameterMapping> parameterMappings中去，此处包括类型，字段名、jdbctype javatype等
      parameterMappings.add(buildParameterMapping(content));
      //并返回一个问号
      return "?";
    }

    //处理参数映射
    private ParameterMapping buildParameterMapping(String content) {
      Map<String, String> propertiesMap = parseParameterMapping(content);
      String property = propertiesMap.get("property");
      Class<?> propertyType;
      if (metaParameters.hasGetter(property)) { // issue #448 get type from additional params
        propertyType = metaParameters.getGetterType(property);
      } else if (typeHandlerRegistry.hasTypeHandler(parameterType)) {
        propertyType = parameterType;
      } else if (JdbcType.CURSOR.name().equals(propertiesMap.get("jdbcType"))) {
        propertyType = java.sql.ResultSet.class;
      } else if (property == null || Map.class.isAssignableFrom(parameterType)) {
        propertyType = Object.class;
      } else {
        MetaClass metaClass = MetaClass.forClass(parameterType, configuration.getReflectorFactory());
        if (metaClass.hasGetter(property)) {
          propertyType = metaClass.getGetterType(property);
        } else {
          propertyType = Object.class;
        }
      }
      ParameterMapping.Builder builder = new ParameterMapping.Builder(configuration, property, propertyType);
      Class<?> javaType = propertyType;
      String typeHandlerAlias = null;
      for (Map.Entry<String, String> entry : propertiesMap.entrySet()) {
        String name = entry.getKey();
        String value = entry.getValue();
        if ("javaType".equals(name)) {
          javaType = resolveClass(value);
          builder.javaType(javaType);
        } else if ("jdbcType".equals(name)) {
          builder.jdbcType(resolveJdbcType(value));
        } else if ("mode".equals(name)) {
          builder.mode(resolveParameterMode(value));
        } else if ("numericScale".equals(name)) {
          builder.numericScale(Integer.valueOf(value));
        } else if ("resultMap".equals(name)) {
          builder.resultMapId(value);
        } else if ("typeHandler".equals(name)) {
          typeHandlerAlias = value;
        } else if ("jdbcTypeName".equals(name)) {
          builder.jdbcTypeName(value);
        } else if ("property".equals(name)) {
          // Do Nothing
        } else if ("expression".equals(name)) {
          throw new BuilderException("Expression based parameters are not supported yet");
        } else {
          throw new BuilderException("An invalid property '" + name + "' was found in mapping #{" + content + "}.  Valid properties are " + parameterProperties);
        }
      }
      if (typeHandlerAlias != null) {
        builder.typeHandler(resolveTypeHandler(javaType, typeHandlerAlias));
      }
      return builder.build();
    }

    private Map<String, String> parseParameterMapping(String content) {
      try {
        return new ParameterExpression(content);
      } catch (BuilderException ex) {
        throw ex;
      } catch (Exception ex) {
        throw new BuilderException("Parsing error was found in mapping #{" + content + "}.  Check syntax #{property|(expression), var1=value1, var2=value2, ...} ", ex);
      }
    }
  }

}
```

从这里也可以清楚的看到，SqlSourceBuilder的作用就是将sql加工，分别生成最终的sql(带问号的)和参数映射的一个工具类































































1. XMLIncludeTransformer：解析<include>节点的





```
GenericTokenParser   ---  解析带${}和#{}的字符串的
```