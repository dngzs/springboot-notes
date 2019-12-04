# apache shiro

## 一、简单介绍

> 来源 http://shiro.apache.org/reference.html
>
> https://www.iteye.com/blog/user/jinnianshilongnian

```java
Apache Shiro是一个功能强大且灵活的开源安全框架，可以干净地处理身份验证，授权，企业会话管理和加密。
```

Apache Shiro的首要目标是易于使用和理解。安全有时可能非常复杂，甚至会很痛苦，但这不是必须的。框架应尽可能掩盖复杂性，并公开简洁直观的API，以简化开发人员确保其应用程序安全的工作。

以下是Apache Shiro可以做的一些事情：

- 验证用户以验证其身份(Authentication认证)
- 对用户执行访问控制，例如

​         确定是否为用户分配了某个安全角色

​         确定是否允许用户执行某些操作

- 在任何环境中使用Session API，即使没有Web容器或EJB容器也是如此。
- 在身份验证，访问控制或会话的生存期内对事件做出反应
- 启用单点登录（SSO）功能
- 无需登录即可为用户关联启用“记住我”服务

… 

在开发使用中,认证(用户登陆)和授权(访问某资源的权限)是shiro最核心的两大功能模块.

## 二、shiro的功能介绍

Apache Shiro是具有许多功能的全面的应用程序安全框架

![](shiroimg\1.png)

shiro的主要功能是四个

- **身份认证**： 也就是我们说的登录
- **授权**： 访问控制的过程，即确定“谁”可以访问“什么”。
- **会话管理**：即使在非Web或EJB应用程序中，也可以管理特定于用户的会话
- **密码**：使用加密算法保持数据安全，同时API很简单，方便使用

在不同的应用程序环境中，还具有其他功能来支持和加强自身

-  Shiro的Web API可帮助轻松保护Web应用程序的安全。
- 缓存是Apache Shiro API中的第一层公民，可确保安全操作保持快速高效
- shiro支持多线程应用的并发验证，即如在一个线程中开启另一个线程，能把权限自动传播过去；
- 提供单元测试
- 允许一个用户假装为另一个用户（如果他们允许）的身份进行访问；
- 记住我，这个是非常常见的功能，即一次登录后，下次再来的话不用登录了

## 三、shiro的简单架构介绍

在shiro的最高概念层面，Shiro的架构有3个主要概念：和Subject，SecurityManager和Realms，下图是这些组件如何交互的高级概述，我们将在下面介绍每个概念

![](shiroimg\2.png)

- **Subject(主体)**：代表着“当前用户”，与当前应用交互的任何东西都是Subject，如网络爬虫，第三方服务等；即一个抽象概念；所有Subject都绑定到SecurityManager，与Subject的所有交互都会委托给SecurityManager；可以把Subject认为是一个门面；SecurityManager才是实际的执行者；
- **SecurityManager（安全管理器）**：即所有与安全有关的操作都会与SecurityManager交互，它就是shiro的核心，并且它管理着所有Subject；它负责与后边介绍的其他组件进行交互，如果学习过SpringMVC，你可以把它看成DispatcherServlet前端控制器
- **Realm(域)**：Shiro从从Realm获取安全数据（如用户、角色、权限）；也就是说SecurityManager要验证用户身份，那么它需要从Realm获取相应的用户进行比较以确定用户身份是否合法；也需要从Realm得到用户相应的角色/权限进行验证用户是否能进行操作；可以把Realm看成DataSource，即安全数据源。

```
总结：shiro的简单运行流程
1、应用代码通过Subject来进行认证和授权，而Subject又委托给SecurityManager；
2、我们需要给Shiro的SecurityManager注入Realm，从而让SecurityManager能得到合法的用户及其权限进行判断。
```

## 四、详细的架构介绍

下图显示了Shiro的核心体系结构概念

![](shiroimg\3.png)

- **Subject（主体）**：，可以看到主体可以是任何可以与应用交互的“用户
- **SecurityManager**（[org.apache.shiro.mgt.SecurityManager](https://shiro.apache.org/static/current/apidocs/org/apache/shiro/mgt/SecurityManager.html)）：相当于SpringMVC中的DispatcherServlet或者Struts2中的FilterDispatcher；是Shiro的心脏；所有具体的交互都通过SecurityManager进行控制；它管理着所有Subject、且负责进行认证和授权、及会话、缓存的管理。
- **Authenticator（认证器）**：，负责主体认证的，这是一个扩展点，如果用户觉得Shiro默认的不好，可以自定义实现；其需要认证策略（Authentication Strategy），即什么情况下算用户认证通过了；

​       **认证策略**（[org.apache.shiro.authc.pam.AuthenticationStrategy](https://shiro.apache.org/static/current/apidocs/org/apache/shiro/authc/pam/AuthenticationStrategy.html)）
​        如果`Realm`配置了多个**认证策略**，则`AuthenticationStrategy`  它将协调Realms，来确保是否认证成功了，比如一个成功就成功了，还是所有的都要成功，才算真正的成功了）。 

- **Authrizer（授权器）**：，或者访问控制器，用来决定主体是否有权限进行相应的操作；即控制着用户能访问应用中的哪些功能；
- **Realm**：可以有1个或多个Realm，可以认为是安全实体数据源，即用于获取安全实体的；可以是JDBC实现，也可以是LDAP实现，或者内存实现等等；由用户提供；注意：Shiro不知道你的用户/权限存储在哪及以何种格式存储；所以我们一般在应用中都需要实现自己的Realm；
- **SessionManager**：如果写过Servlet就应该知道Session的概念，Session呢需要有人去管理它的生命周期，这个组件就是SessionManager；而Shiro并不仅仅可以用在Web环境，也可以用在如普通的JavaSE环境、EJB等环境；所有呢，Shiro就抽象了一个自己的Session来管理主体与应用之间交互的数据；这样的话，比如我们在Web环境用，刚开始是一台Web服务器；接着又上了台EJB服务器；这时想把两台服务器的会话数据放到一个地方，这个时候就可以实现分布式会话（如把数据放到redis服务器）；
- **SessionDAO**：DAO大家都用过，数据访问对象，用于会话的CRUD，比如我们想把Session保存到数据库，那么可以实现自己的SessionDAO，通过如JDBC写到数据库；比如想把Session放到redis中，可以实现自己的redis SessionDAO；另外SessionDAO中可以使用Cache进行缓存，以提高性能；
- **CacheManager**：缓存控制器，来管理如用户、角色、权限等的缓存的；因为这些数据基本上很少去改变，放到缓存中后可以提高访问的性能
- **Cryptography**：密码模块，Shiro提供了一些常见的加密组件用于如密码加密/解密的。

## 五、Shiro源码之 ---- 入门demo

maven引入相关jar包

```xml
<dependency>
        <groupId>org.apache.shiro</groupId>
        <artifactId>shiro-core</artifactId>
        <version>1.4.2</version>
    </dependency>
    <dependency>
        <groupId>junit</groupId>
        <artifactId>junit</artifactId>
        <version>4.12</version>
    </dependency>
</dependencies>
```

hello world代码

```java
@Test
public void test(){
    //创建SecurityManager
    DefaultSecurityManager defaultSecurityManager = new DefaultSecurityManager();
    
    //绑定SecurityManager给SecurityUtils
    SecurityUtils.setSecurityManager(defaultSecurityManager);
    
    //new一个简单的realm
    SimpleAccountRealm simpleAccountRealm = new SimpleAccountRealm();
    simpleAccountRealm.addAccount("zhangbo","123");
   
    //将Realm注册到SecurityManager
    defaultSecurityManager.setRealm(simpleAccountRealm);
   
    //获取主体Subject
    Subject subject = SecurityUtils.getSubject();
    UsernamePasswordToken token = new UsernamePasswordToken("zhangbo", "123");
   
    //登录，认证身份
    subject.login(token);
    
    //用户是否已登录
    Assert.assertTrue(subject.isAuthenticated());
    
    //退出登录
    subject.logout();
}
```

## 六、深入理解shiro源码

### 1. shiro web入口EnvironmentLoaderListener 

`EnvironmentLoaderListener` 作为 web的入口，那当然是实现了`ServletContextListener`接口，`javax.servlet.ServletContextListener`是servlet规范里面的接口。它能够监听 `ServletContext` 对象的生命周期，实际上就是监听 Web 应用的生命周期。当Servlet 容器启动或终止Web 应用时，会触发`ServletContextEvent` 事件，该事件由`ServletContextListener` 来处理的

​       Shiro 的 `EnvironmentLoaderListener` 就是一个典型的 `ServletContextListener`，它也是整个 Shiro Web 应用的入口，来看下继承结构

![](shiroimg\4.png)

```java
public class EnvironmentLoaderListener extends EnvironmentLoader implements ServletContextListener {

    /**
     * 容器启动时调用
     */
    public void contextInitialized(ServletContextEvent sce) {
        initEnvironment(sce.getServletContext());
    }

    /**
     * 容器死亡时调用
     */
    public void contextDestroyed(ServletContextEvent sce) {
        destroyEnvironment(sce.getServletContext());
    }
}
```

从上面我们可以看出`EnvironmentLoaderListener`

(1)  继承自`EnvironmentLoader`

(2)  实现了servlet规范的`ServletContextListener`接口，在web容器启动和死亡的时候会调用上述两个方法

(3)  `EnvironmentLoaderListener`并没有实现任何的代码，真正干活的是`EnvironmentLoader`类

```java
public class EnvironmentLoader {

    /**
     *
     */
    public static final String ENVIRONMENT_CLASS_PARAM = "shiroEnvironmentClass";

    /**
     *
     */
    public static final String CONFIG_LOCATIONS_PARAM = "shiroConfigLocations";

    public static final String ENVIRONMENT_ATTRIBUTE_KEY = EnvironmentLoader.class.getName() + ".ENVIRONMENT_ATTRIBUTE_KEY";

    private static final Logger log = LoggerFactory.getLogger(EnvironmentLoader.class);

    /**
     * 从 ServletContext 中获取相关信息，并创建 WebEnvironment 实例
     */
    public WebEnvironment initEnvironment(ServletContext servletContext) throws IllegalStateException {
        //确保WebEnvironment只创建一次
        if (servletContext.getAttribute(ENVIRONMENT_ATTRIBUTE_KEY) != null) {
            String msg = "There is already a Shiro environment associated with the current ServletContext.  " +
                    "Check if you have multiple EnvironmentLoader* definitions in your web.xml!";
            throw new IllegalStateException(msg);
        }

        servletContext.log("Initializing Shiro environment");
        log.info("Starting Shiro environment initialization.");

        long startTime = System.currentTimeMillis();

        try {
            //尝试从web.xml（context-param  shiroEnvironmentClass）配置中和java SPI尝试获取WebEnvironment，如果获取不到，就返回默认的IniWebEnvironment，并调用WebEnvironment的init方法
            WebEnvironment environment = createEnvironment(servletContext);
            //将WebEnvironment设置到servletContext中去
            servletContext.setAttribute(ENVIRONMENT_ATTRIBUTE_KEY,environment);
   
            log.debug("Published WebEnvironment as ServletContext attribute with name [{}]",
                    ENVIRONMENT_ATTRIBUTE_KEY);

            if (log.isInfoEnabled()) {
                long elapsed = System.currentTimeMillis() - startTime;
                log.info("Shiro environment initialized in {} ms.", elapsed);
            }

            return environment;
        } catch (RuntimeException ex) {
            log.error("Shiro environment initialization failed", ex);
            servletContext.setAttribute(ENVIRONMENT_ATTRIBUTE_KEY, ex);
            throw ex;
        } catch (Error err) {
            log.error("Shiro environment initialization failed", err);
            servletContext.setAttribute(ENVIRONMENT_ATTRIBUTE_KEY, err);
            throw err;
        }
    }

    /**
     * 
     */
    @Deprecated
    protected Class<?> determineWebEnvironmentClass(ServletContext servletContext) {
        //从web.xml（context-param）中获取 WebEnvironment 接口的实现类
        Class<? extends WebEnvironment> webEnvironmentClass = webEnvironmentClassFromServletContext(servletContext);
        if( webEnvironmentClass != null) {
            return webEnvironmentClass;
        } else {
            //如果没有配置，则默认返回IniWebEnvironment.class
            return getDefaultWebEnvironmentClass();
        }
    }

    
    private Class<? extends WebEnvironment> webEnvironmentClassFromServletContext(ServletContext servletContext) {

        Class<? extends WebEnvironment> webEnvironmentClass = null;
        //从web.xml（context-param）中获取 WebEnvironment 接口的实现类
        String className = servletContext.getInitParameter(ENVIRONMENT_CLASS_PARAM);
        if (className != null) {
            try {
                webEnvironmentClass = ClassUtils.forName(className);
            } catch (UnknownClassException ex) {
                throw new ConfigurationException(
                        "Failed to load custom WebEnvironment class [" + className + "]", ex);
            }
        }
        return webEnvironmentClass;
    }

    private WebEnvironment webEnvironmentFromServiceLoader() {

        WebEnvironment webEnvironment = null;
        // try to load WebEnvironment as a service
        ServiceLoader<WebEnvironment> serviceLoader = ServiceLoader.load(WebEnvironment.class);
        Iterator<WebEnvironment> iterator = serviceLoader.iterator();

        // Use the first one
        if (iterator.hasNext()) {
            webEnvironment = iterator.next();
        }
        // if there are others, throw an error
        if (iterator.hasNext()) {
            List<String> allWebEnvironments = new ArrayList<String>();
            allWebEnvironments.add(webEnvironment.getClass().getName());
            while (iterator.hasNext()) {
                allWebEnvironments.add(iterator.next().getClass().getName());
            }
            throw new ConfigurationException("ServiceLoader for class [" + WebEnvironment.class + "] returned more then one " +
                    "result.  ServiceLoader must return zero or exactly one result for this class. Select one using the " +
                    "servlet init parameter '"+ ENVIRONMENT_CLASS_PARAM +"'. Found: " + allWebEnvironments);
        }
        return webEnvironment;
    }

   
    protected Class<? extends WebEnvironment> getDefaultWebEnvironmentClass() {
        return IniWebEnvironment.class;
    }

    /**
     * 从web.xml（context-param  shiroEnvironmentClass）配置中和java SPI尝试获取WebEnvironment，如果获取不到，就返回默认的IniWebEnvironment
     */
    protected WebEnvironment determineWebEnvironment(ServletContext servletContext) {
         //确定 WebEnvironment 接口的实现类，先从web.xml（context-param）中获取 WebEnvironment 接口的实现类,如果没有配置，则默认返回
        Class<? extends WebEnvironment> webEnvironmentClass = webEnvironmentClassFromServletContext(servletContext);
        WebEnvironment webEnvironment = null;

        //尝试java SPI加载
        if (webEnvironmentClass == null) {
            webEnvironment = webEnvironmentFromServiceLoader();
        }

        // 如果此时两个都是空，则默认是 IniWebEnvironment
        if (webEnvironmentClass == null && webEnvironment == null) {
            webEnvironmentClass = getDefaultWebEnvironmentClass();
        }

        //至此，我们将webEnvironmentClass作为class，并初始化它
        if (webEnvironmentClass != null) {
            webEnvironment = (WebEnvironment) ClassUtils.newInstance(webEnvironmentClass);
        }

        return webEnvironment;
    }

    /**
     * 创建web环境
     */
    protected WebEnvironment createEnvironment(ServletContext sc) {
        //创建WebEnvironment
        WebEnvironment webEnvironment = determineWebEnvironment(sc);
        //类型检查
        if (!MutableWebEnvironment.class.isInstance(webEnvironment)) {
            throw new ConfigurationException("Custom WebEnvironment class [" + webEnvironment.getClass().getName() +
                    "] is not of required type [" + MutableWebEnvironment.class.getName() + "]");
        }

        //从 ServletContext 中获取 Shiro 配置文件的位置参数，并判断该参数是否在web.xml配置了
        String configLocations = sc.getInitParameter(CONFIG_LOCATIONS_PARAM);
        boolean configSpecified = StringUtils.hasText(configLocations);
        //若配置文件位置参数已定义，则需确保该实现类实现了 ResourceConfigurable 接口
        if (configSpecified && !(ResourceConfigurable.class.isInstance(webEnvironment))) {
            String msg = "WebEnvironment class [" + webEnvironment.getClass().getName() + "] does not implement the " +
                    ResourceConfigurable.class.getName() + "interface.  This is required to accept any " +
                    "configured " + CONFIG_LOCATIONS_PARAM + "value(s).";
            throw new ConfigurationException(msg);
        }
        
        MutableWebEnvironment environment = (MutableWebEnvironment) webEnvironment;

        //将 ServletContext 放入WebEnvironment实例中
        environment.setServletContext(sc);

        //若配置文件位置参数已定义，且该实例是 ResourceConfigurable 接口的实例（实现了该接口），则将此参数放入该实例中
        if (configSpecified && (environment instanceof ResourceConfigurable)) {
            ((ResourceConfigurable) environment).setConfigLocations(configLocations);
        }

        //可进一步定制 WebEnvironment 实例（在子类中扩展）
        customizeEnvironment(environment);

        //调用 WebEnvironment 实例的 init 方法
        LifecycleUtils.init(environment);

        //返回
        return environment;
    }

    /**
     * Any additional customization of the Environment can be by overriding this method. For example setup shared
     * resources, etc. By default this method does nothing.
     * @param environment
     */
    protected void customizeEnvironment(WebEnvironment environment) {
    }

    /**
     * 销毁 WebEnvironment 
     */
    public void destroyEnvironment(ServletContext servletContext) {
        servletContext.log("Cleaning up Shiro Environment");
        try {
            Object environment = servletContext.getAttribute(ENVIRONMENT_ATTRIBUTE_KEY);
            if (environment instanceof WebEnvironment) {
                finalizeEnvironment((WebEnvironment) environment);
            }
            //// 调用 WebEnvironment 实例的 destroy 方法
            LifecycleUtils.destroy(environment);
        } finally {
            //移除 ServletContext 中存放的 WebEnvironment 实例
            servletContext.removeAttribute(ENVIRONMENT_ATTRIBUTE_KEY);
        }
    }

    /**
     * Any additional cleanup of the Environment can be done by overriding this method.  For example clean up shared
     * resources, etc. By default this method does nothing.
     * @param environment
     * @since 1.3
     */
    protected void finalizeEnvironment(WebEnvironment environment) {
    }
}
```

看了EnvironmentLoader的源码，我们来总结下

（1）  当容器启动时，EnvironmentLoader读取web.xml配置，主要配置有下面两个

```xml
<context-param>
    <param-name>shiroEnvironmentClass</param-name>
    <param-value>WebEnvironment 接口的实现类</param-value>
</context-param>
<context-param>
    <param-name>shiroConfigLocations</param-name>
    <param-value>shiro.ini 配置文件的位置</param-value>
</context-param>
```

尝试读取web.xml  context-param shiroEnvironmentClass配置的`WebEnvironment`，如果获取不到，尝试从JAVA SPI获取`WebEnvironment`的实现，如果还获取不到，则返回一个默认的`IniWebEnvironment`实现，并且调用`IniWebEnvironment`的init初始化方法。获取到`WebEnvironment`class对象后，将Environment实例化，并且返回，加载到 ServletContext 中。

shiroConfigLocations再来分析，无疑就是找到shiro.ini文件，将其解析

（2） 容器关闭时，销毁 `WebEnvironment` 实例，并从 ServletContext 将其移除。

简单讲：`EnvironmentLoader` 中仅用于创建 WebEnvironment 接口的实现类，随后将由这个实现类来加载并解析 shiro.ini 配置文件

### 2.  WebEnvironment

先来了解下他的继承结构

![](shiroimg\5.png)

（1）WebEnvironment 的结构非常的复杂

（2） 最底层的 IniWebEnvironment 是 WebEnvironment 接口的默认实现类，它将读取 ini 配置文件，并创建 WebEnvironment 实例。

（3）如果需要将 Shiro 配置定义在 XML 或 Properties 配置文件中，那就需要自定义一些 WebEnvironment 实现类。

（4）WebEnvironment 的实现类不仅需要实现最顶层的 Environment 接口，还需要实现具有生命周期功能的 Initializable 与 Destroyable 接口。



那么 IniWebEnvironment 这个默认的实现类到底做了写什么呢？来看看它的代码

```java
public class IniWebEnvironment extends ResourceBasedWebEnvironment implements Initializable, Destroyable {

    // 默认 shiro.ini 路径
    public static final String DEFAULT_WEB_INI_RESOURCE_PATH = "/WEB-INF/shiro.ini";
    public static final String FILTER_CHAIN_RESOLVER_NAME = "filterChainResolver";

    private static final Logger log = LoggerFactory.getLogger(IniWebEnvironment.class);

    /**
     *  定义一个 Ini 对象，用于封装 ini 配置项
     */
    private Ini ini;

    private WebIniSecurityManagerFactory factory;

    public IniWebEnvironment() {
        //初始化SecurityManagerFactory对象，用来生成
        factory = new WebIniSecurityManagerFactory();
    }

    /**
     * Initializes this instance by resolving any potential (explicit or resource-configured) {@link Ini}
     * configuration and calling {@link #configure() configure} for actual instance configuration.
     */
    public void init() {

        setIni(parseConfig());

        configure();
    }

    /**
     * 
     */
    protected Ini parseConfig() {
        // 从成员变量中获取 Ini 对象
        Ini ini = getIni();

        // 从 web.xml 中获取配置文件位置（在 EnvironmentLoader 中已设置）
        String[] configLocations = getConfigLocations();

        if (log.isWarnEnabled() && !CollectionUtils.isEmpty(ini) &&
                configLocations != null && configLocations.length > 0) {
            log.warn("Explicit INI instance has been provided, but configuration locations have also been " +
                    "specified.  The {} implementation does not currently support multiple Ini config, but this may " +
                    "be supported in the future. Only the INI instance will be used for configuration.",
                    IniWebEnvironment.class.getName());
        }
        // 若成员变量中不存在，则从已定义的配置文件位置获取
        if (CollectionUtils.isEmpty(ini)) {
            log.debug("Checking any specified config locations.");
            ini = getSpecifiedIni(configLocations);
        }

        // 若已定义的配置文件中仍然不存在，则从默认的位置获取（/WEB-INF/shiro.ini，classpath:shiro.ini）
        if (CollectionUtils.isEmpty(ini)) {
            log.debug("No INI instance or config locations specified.  Trying default config locations.");
            ini = getDefaultIni();
        }

        // 留给子类扩展，并且可以两个ini合并
        ini = mergeIni(getFrameworkIni(), ini);

        if (CollectionUtils.isEmpty(ini)) {
            String msg = "Shiro INI configuration was either not found or discovered to be empty/unconfigured.";
            throw new ConfigurationException(msg);
        }
        return ini;
    }

    protected void configure() {
        //清空这个 Bean 容器（一个 Map<String, Object> 对象，在 DefaultEnvironment 中定义）
        this.objects.clear();

        WebSecurityManager securityManager = createWebSecurityManager();
        setWebSecurityManager(securityManager);
        
        FilterChainResolver resolver = createFilterChainResolver();
        if (resolver != null) {
            setFilterChainResolver(resolver);
        }
    }

    /**
     *扩展点，留给子类扩展
     * @since 1.4
     */
    protected Ini getFrameworkIni() {
        return null;
    }

    protected Ini getSpecifiedIni(String[] configLocations) throws ConfigurationException {

        Ini ini = null;

        if (configLocations != null && configLocations.length > 0) {

            if (configLocations.length > 1) {
                log.warn("More than one Shiro .ini config location has been specified.  Only the first will be " +
                        "used for configuration as the {} implementation does not currently support multiple " +
                        "files.  This may be supported in the future however.", IniWebEnvironment.class.getName());
            }

            //通过第一个配置文件的位置来创建 Ini 对象，且必须有一个配置文件，否则就抛出错误
            ini = createIni(configLocations[0], true);
        }

        return ini;
    }

    protected Ini mergeIni(Ini ini1, Ini ini2) {

        if (ini1 == null) {
            return ini2;
        }

        if (ini2 == null) {
            return ini1;
        }

        // at this point we have two valid ini objects, create a new one and merge the contents of 2 into 1
        Ini iniResult = new Ini(ini1);
        iniResult.merge(ini2);

        return iniResult;
    }

    protected Ini getDefaultIni() {

        Ini ini = null;

        String[] configLocations = getDefaultConfigLocations();
        if (configLocations != null) {
            for (String location : configLocations) {
                ini = createIni(location, false);
                if (!CollectionUtils.isEmpty(ini)) {
                    log.debug("Discovered non-empty INI configuration at location '{}'.  Using for configuration.",
                            location);
                    break;
                }
            }
        }

        return ini;
    }

    /**
     * required：是否必须加载，如果为true，仍加载不到，则抛出异常
     */
    protected Ini createIni(String configLocation, boolean required) throws ConfigurationException {

        Ini ini = null;

        if (configLocation != null) {
            // 从指定路径下读取配置文件
            ini = convertPathToIni(configLocation, required);
        }
        if (required && CollectionUtils.isEmpty(ini)) {
            String msg = "Required configuration location '" + configLocation + "' does not exist or did not " +
                    "contain any INI configuration.";
            throw new ConfigurationException(msg);
        }

        return ini;
    }

    protected FilterChainResolver createFilterChainResolver() {

        FilterChainResolver resolver = null;

        Ini ini = getIni();

        if (!CollectionUtils.isEmpty(ini)) {
            // Filter 可以从 [urls] 或 [filters] 片段中读取
            Ini.Section urls = ini.getSection(IniFilterChainResolverFactory.URLS);
            Ini.Section filters = ini.getSection(IniFilterChainResolverFactory.FILTERS);
            if (!CollectionUtils.isEmpty(urls) || !CollectionUtils.isEmpty(filters)) {
                //通过工厂对象创建 FilterChain解析器 实例
                Factory<FilterChainResolver> factory = (Factory<FilterChainResolver>) this.objects.get(FILTER_CHAIN_RESOLVER_NAME);
                if (factory instanceof IniFactorySupport) {
                    IniFactorySupport iniFactory = (IniFactorySupport) factory;
                    iniFactory.setIni(ini);
                    iniFactory.setDefaults(this.objects);
                }
                resolver = factory.getInstance();
            }
        }

        return resolver;
    }

    protected WebSecurityManager createWebSecurityManager() {
        //获取到上面设置的Ini对象
        Ini ini = getIni();
        if (!CollectionUtils.isEmpty(ini)) {
            //设置ini对象
            factory.setIni(ini);
        }
        //生成一个key-value map ,响应的“filterChainResolver”-IniFilterChainResolverFactory
        Map<String, Object> defaults = getDefaults();
        if (!CollectionUtils.isEmpty(defaults)) {
            //设置filterChain的解析器工厂
            factory.setDefaults(defaults);
        }

        WebSecurityManager wsm = (WebSecurityManager)factory.getInstance();

        //从工厂中获取 Bean Map 并将其放入 Bean 容器中
        Map<String, ?> beans = factory.getBeans();
        if (!CollectionUtils.isEmpty(beans)) {
            this.objects.putAll(beans);
        }

        return wsm;
    }

    /**
     * Returns an array with two elements, {@code /WEB-INF/shiro.ini} and {@code classpath:shiro.ini}.
     *
     * @return an array with two elements, {@code /WEB-INF/shiro.ini} and {@code classpath:shiro.ini}.
     */
    protected String[] getDefaultConfigLocations() {
        return new String[]{
                DEFAULT_WEB_INI_RESOURCE_PATH,
                IniFactorySupport.DEFAULT_INI_RESOURCE_PATH
        };
    }

    /**
     * 尝试从文件，ServletContext等地方加载ini文件
     */
    private Ini convertPathToIni(String path, boolean required) {

        Ini ini = null;

        if (StringUtils.hasText(path)) {
            InputStream is = null;

            // 若路径不包括资源前缀（classpath:、url:、file:），则从 ServletContext 中读取，否则从这些资源路径下读取
            if (!ResourceUtils.hasResourcePrefix(path)) {
                is = getServletContextResourceStream(path);
            } else {
                try {
                    is = ResourceUtils.getInputStreamForPath(path);
                } catch (IOException e) {
                    if (required) {
                        throw new ConfigurationException(e);
                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug("Unable to load optional path '" + path + "'.", e);
                        }
                    }
                }
            }
            if (is != null) {
                //将流中的数据加载到 Ini 对象中
                ini = new Ini();
                ini.load(is);
            } else {
                if (required) {
                    throw new ConfigurationException("Unable to load resource path '" + path + "'");
                }
            }
        }

        return ini;
    }

    //TODO - this logic is ugly - it'd be ideal if we had a Resource API to polymorphically encaspulate this behavior
    private InputStream getServletContextResourceStream(String path) {
        InputStream is = null;
        // 需要将路径进行标准化
        path = WebUtils.normalize(path);
        ServletContext sc = getServletContext();
        if (sc != null) {
            is = sc.getResourceAsStream(path);
        }

        return is;
    }

 
    public Ini getIni() {
        return this.ini;
    }


    public void setIni(Ini ini) {
        this.ini = ini;
    }

    
    protected Map<String, Object> getDefaults() {
        Map<String, Object> defaults = new HashMap<String, Object>();
        defaults.put(FILTER_CHAIN_RESOLVER_NAME, new IniFilterChainResolverFactory());
        return defaults;
    }


    @SuppressWarnings("unused")
    protected WebIniSecurityManagerFactory getSecurityManagerFactory() {
        return factory;
    }

    protected void setSecurityManagerFactory(WebIniSecurityManagerFactory factory) {
        this.factory = factory;
    }
}
```

基本看了下`IniWebEnvironment` 的逻辑，我们来总结下

（1） 查找并加载 shiro.ini 配置文件，首先从自身成员变量里查找，然后从 web.xml 中查找，然后从 /WEB-INF 下查找，然后从 classpath 下查找，若均未找到，则直接报错。

（2） 当找到了 ini 配置文件后就开始解析，此时构造了一个 Bean 容器（相当于一个轻量级的 IOC 容器），最终的目标是为了创建 WebSecurityManager 对象与 FilterChainResolver 对象，创建过程使用了 Abstract Factory 模式

![](shiroimg\6.png)

可以看到，两三个已经过期了，过期的原因是

```
use Shiro's {@code Environment} mechanisms instead.
请改用Shiro的Environment机制。
```

其中有两个 Factory 需要关注：

- `WebIniSecurityManagerFactory` 用于创建 `WebSecurityManager`。
- `IniFilterChainResolverFactory` 用于创建 `FilterChainResolver`。



通过以上分析，相信 `EnvironmentLoaderListener` 已经不再神秘了，无非就是在容器启动时创建 `WebEnvironment` 对象，并由该对象来读取 Shiro 配置文件，创建`WebSecurityManager` 与 `FilterChainResolver` 对象，它们都在后面将要出现的 `ShiroFilter` 中起到了重要作用。

从 web.xml 中同样可以得知，`ShiroFilter` 是整个 Shiro 框架的门面，因为它拦截了所有的请求，后面是需要 `Authentication`（认证）还是需要 `Authorization`（授权）都由它说了算。

> 参考： https://my.oschina.net/huangyong/blog/209339

