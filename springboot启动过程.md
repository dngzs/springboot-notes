# springboot启动过程

## 一、常用启动方式

#### 1.通过普通api进行启动

```java
SpringApplication app = new SpringApplication(MySpringConfiguration.class);
	app.setBannerMode(Banner.Mode.OFF);
	app.run(args);
```

2.通过Fluent Api

spring提供的SpringApplicationBuilder进行绑定

如果需要构建ApplicationContext层次结构（具有父/子关系的多个上下文），或者如果你更喜欢spring提供的==流畅==API，那么你就可以使用Fluent  api

```java
new SpringApplicationBuilder()
		.sources(Parent.class)
		.child(Application.class)
		.bannerMode(Banner.Mode.OFF)
		.run(args);
```

## 二、springboot的启动

springboot的启动阶段，自认为可以分为三个阶段

#### 1.准备阶段

![](bootimg\1.png)



new springApplication()干了些什么事情呢

![](bootimg\3.png)

- **设置primarySources**

​      primarySources是main方法传入的，通过==@SpringBootApplication==标注的类，也可以是任何==@Configuration==配置类

- **类型推断**

​      主要是推断该程序是web( server )服务呢？还是webflux( reactive )服务？还是纯的java( none ) 服务



#@see   WebApplicationType 

```java
/**
 * The application should not run as a web application and should not start an
 * embedded web server.（应用程序应不能基于servlet的Web应用程序运行，更不能启动嵌入式servlet Web服务器）
 */
NONE,  

/**
 * The application should run as a servlet-based web application and should start an
 * embedded servlet web server.（应用程序应作为基于servlet的Web应用程序运行，并应启动嵌入式servlet Web服务器）
 */
SERVLET,

/**
 * The application should run as a reactive web application and should start an
 * embedded reactive web server.（应用程序应作为响应式Web应用程序运行，并应启动嵌入式响应式Web服务器。）
 */
REACTIVE;
```

 ![](bootimg\2.png)



- **加载应用上下文初始器 （ ApplicationContextInitializer ）**

​           运用spring工厂类，加载ApplicationContextInitializer 集合

​             具体是通过 `org.springframework.core.io.support.SpringFactoriesLoader`来加载的

​    SpringFactoriesLoader通过在META-INF/spring.factories去加载对应类名为key的类并实例化，然后通过

`AnnotationAwareOrderComparator.sort(instances)`去排序；目前ApplicationContextInitializer默认加载的是这两个实现（*举例，当然还有很多，这个只是autoconfiguer下的带上spring-boot-starter包下的，初始化估计有6个*）

```java
org.springframework.context.ApplicationContextInitializer=\
org.springframework.boot.autoconfigure.SharedMetadataReaderFactoryContextInitializer,\
org.springframework.boot.autoconfigure.logging.ConditionEvaluationReportLoggingListener
```

SharedMetadataReaderFactoryContextInitialize向容器注册了一个CachingMetadataReaderFactoryPostProcessor 这个bean的作用主要是在容器启动的时候调用的==<u>invokeBeanFactoryPostProcessors</u>==来缓存每个类的元数据信息（ <u>包括类的父类，接口，是否抽象，类名等等的信息</u> ）的

ConditionEvaluationReportLoggingListener则会想容器注册一个事件监听器（`ConditionEvaluationReportListener`），这个事件监听器的作用是用来当容器刷新或者启动失败的时候来打印状况报告的

- **加载应用事件监听器（ ApplicationListener )**

​            运用spring工厂类，加载ApplicationListener 集合

​           目前ApplicationListener 默认加载的是这一个实现（*举例，当然还有很多，这个只是autoconfiguer下的带上spring-boot-starter包下的，初始化估计有10个*）

```java
org.springframework.boot.autoconfigure.BackgroundPreinitializer
```

这个ApplicationListener 很简单，去另起一个线程在boot启动的时候来跑一个耗时的操作，例如jsr303，设置字符编码，验证器、消息转换器等的操作

![](bootimg\5.png)



当spring容器加载完成后，再去判断是不是一个ApplicationReadyEvent或者ApplicationFailedEvent事件，利用CountDownLatch来达到这类耗时操作的初始化完毕

![](bootimg\6.png)

==<u>不得不说spring的设计者把事件和并发同步容器运用的很精妙啊，我们也得学习</u>==

- 推断引导类（Main Class）

  ![](bootimg\7.png)

main方法推断通过堆栈，然后判断方法名是不是main来推断的<!--为啥要推断这个main方法所在的类呢？-->

#### 2.SpringApplication 运行阶段

- **加载 SpringApplication 运行监听器（ SpringApplicationRunListeners ）**

看下接口

@see 此处注意下代码中接口中方法的起始版本

```java
public interface SpringApplicationRunListener {

   /**
    * Called immediately when the run method has first started. Can be used for very
    * early initialization.
    */
   void starting();

   /**
    * Called once the environment has been prepared, but before the
    * {@link ApplicationContext} has been created.
    * @param environment the environment
    */
   void environmentPrepared(ConfigurableEnvironment environment);

   /**
    * Called once the {@link ApplicationContext} has been created and prepared, but
    * before sources have been loaded.
    * @param context the application context
    */
   void contextPrepared(ConfigurableApplicationContext context);

   /**
    * Called once the application context has been loaded but before it has been
    * refreshed.
    * @param context the application context
    */
   void contextLoaded(ConfigurableApplicationContext context);

   /**
    * The context has been refreshed and the application has started but
    * {@link CommandLineRunner CommandLineRunners} and {@link ApplicationRunner
    * ApplicationRunners} have not been called.
    * @param context the application context.
    * @since 2.0.0
    */
   void started(ConfigurableApplicationContext context);

   /**
    * Called immediately before the run method finishes, when the application context has
    * been refreshed and all {@link CommandLineRunner CommandLineRunners} and
    * {@link ApplicationRunner ApplicationRunners} have been called.
    * @param context the application context.
    * @since 2.0.0
    */
   void running(ConfigurableApplicationContext context);

   /**
    * Called when a failure occurs when running the application.
    * @param context the application context or {@code null} if a failure occurred before
    * the context was created
    * @param exception the failure
    * @since 2.0.0
    */
   void failed(ConfigurableApplicationContext context, Throwable exception);

}
```

|      监听方法       |                           阶段说明                           | springboot起始版本 |
| :-----------------: | :----------------------------------------------------------: | :----------------: |
|      starting       |                       spring应用刚启动                       |       1.0.0        |
| environmentPrepared |        ConfigurableEnvironment 准备妥当，允许将其调整        |       1.0.0        |
|   contextPrepared   |    ConfigurableApplicationContext 准备妥当，允许将其调整     |       1.0.0        |
|    contextLoaded    |      ConfigurableApplicationContext 已装载，但仍未启动       |       1.0.0        |
|       started       | ConfigurableApplicationContext 已启动，此时 Spring Bean 已初始化完成 |       2.0.0        |
|       running       |                     Spring 应用正在运行                      |       2.0.0        |
|       failed        |                     Spring 应用运行失败                      |       2.0.0        |

各个阶段请注意看==`SpringApplication`==源码，基本都在其中

```java
对于事件广播

Spring Boot 通过 SpringApplicationRunListener 的实现类 EventPublishingRunListener 利用 Spring Framework 事件 API ，广播 Spring Boot 事件。
```

具体请查看EventPublishingRunListener源码，发布的时间在上面已经设置进去了（==请看 1标题中的加载应用事件监听器（ ApplicationListener )==）



- **准备环境（ConfigurableEnvironment）**

  ![](bootimg\8.png)

（1）根据之前的类型（是server,reactive,none）推断要创建什么样的环境

```
server   --  org.springframework.web.context.support.StandardServletEnvironment
reactive --  org.springframework.boot.web.reactive.context.StandardReactiveWebEnvironment
none     --  org.springframework.core.env.StandardEnvironment
```

 （2） 配置环境

```
 如果设置了一些启动参数args，添加基于args的SimpleCommandLinePropertySource
 还会配置profile信息，比如设置了spring.profiles.active启动参数，设置到环境信息中
```

（3）执行监听器的environmentPrepared 事件 ， 通知环境已经准备好了（在这里其实就会加载spring的配置文件的，可以去看ConfigFileApplicationListener的源码）

（4）绑定Environment 到 当前的 SpringApplication中（<!--此处绑定spring.main干嘛不是特别理解-->）

（5）判断是否是用户自定义的Environment（==isCustomEnvironment在用户自定义设置时会变为true,默认是false==）



- **配置是否忽略beanInfo类**

  是否跳过beanInfo类，通过

  ```
  spring.beaninfo.ignore
  ```

  来控制，先从system.getproperty获取该属性，如果获取不到，就从environment中拿，此时肯定能拿到，如果没拿到就是没配置，那么默认给true



- **打印Banner**

默认拿banner位置

  

```
  spring.banner.image.location
  banner.("gif", "jpg", "png") 
  spring.banner.location
  banner.txt
```

​    如果还没拿到，那么从SpringBootBanner出拿



-   **==创建 Spring 应用上下文（ ConfigurableApplicationContext ）==**

根据之前的类型推断来创建不同的应用上下文

![](bootimg\9.png)



```
Web Reactive： AnnotationConfigReactiveWebServerApplicationContext
Web Servlet： AnnotationConfigServletWebServerApplicationContext
非 Web： AnnotationConfigApplicationContext
```

- **加载异常报告集合**

默认情况下就这一个，也可通过工厂自己扩展异常报告

```java
org.springframework.boot.SpringBootExceptionReporter=\
org.springframework.boot.diagnostics.FailureAnalyzers
```

- **prepareContext准备容器阶段**

```java
private void prepareContext(ConfigurableApplicationContext context,
      ConfigurableEnvironment environment, SpringApplicationRunListeners listeners,
      ApplicationArguments applicationArguments, Banner printedBanner) {
    //将之前创建的环境设置到容器中
   context.setEnvironment(environment);
   //见下分析
   postProcessApplicationContext(context);
   //还记得我们在springboot准备阶段配置的上下文初始器吗？实在这里执行的，对容器做最后的调整
   applyInitializers(context);
   //这里就会回调容器准备好了的事件，准备回调
   listeners.contextPrepared(context);
   if (this.logStartupInfo) {
      logStartupInfo(context.getParent() == null);
      logStartupProfileInfo(context);
   }
   // Add boot specific singleton beans
   ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
   //将args参数配置信息加入到容器中
   beanFactory.registerSingleton("springApplicationArguments", applicationArguments);
   if (printedBanner != null) {
      beanFactory.registerSingleton("springBootBanner", printedBanner);
   }
   //将bean是否覆盖的操作加入到容器中去
   if (beanFactory instanceof DefaultListableBeanFactory) {
      ((DefaultListableBeanFactory) beanFactory)
            .setAllowBeanDefinitionOverriding(this.allowBeanDefinitionOverriding);
   }
   // Load the sources
   /// 根据构造时传入的资源进行资源加载,可以是类名、包名、或者XML的bean定义资源路径
   Set<Object> sources = getAllSources();
   Assert.notEmpty(sources, "Sources must not be empty");
   load(context, sources.toArray(new Object[0]));
   //回调容器装载完毕，但是还没有启动呢
   listeners.contextLoaded(context);
}
```

```java
//填充一些设置属性
protected void postProcessApplicationContext(ConfigurableApplicationContext context) {
    //设置beanname的生成策略（如果配置了，SpringApplication可以设置）
    if (this.beanNameGenerator != null) {
      context.getBeanFactory().registerSingleton(
            AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR,
            this.beanNameGenerator);
   }
    //用于加载资源的策略接口
   if (this.resourceLoader != null) {
      if (context instanceof GenericApplicationContext) {
         ((GenericApplicationContext) context)
               .setResourceLoader(this.resourceLoader);
      }
      if (context instanceof DefaultResourceLoader) {
         ((DefaultResourceLoader) context)
               .setClassLoader(this.resourceLoader.getClassLoader());
      }
   }
   //springboot默认情况下会设置一些ConversionService
   if (this.addConversionService) {
      context.getBeanFactory().setConversionService(
            ApplicationConversionService.getSharedInstance());
   }
}
```

`beanNameGenerator`也是一个非常重要的组件，[BeanNameGenerator](https://my.oschina.net/u/1261452/blog/1801885)是beans体系非常重要的一个组件，主要功能是从一定的条件中计算出bean的name.如果出现问题，是可以规避的。同样可以重写解决。所以深入了解BeanNameGenerator体系是十分重要的

[ConversionService](https://www.cnblogs.com/jyyzzjl/p/5478620.html)也是一个非常重要的接口，他可以将不同的数据类型转换到另外一种类型上去，springboot这里给我们配置了一些默认的【==`FormattingConversionService`==】，它认为足够了，其实确实是足够了

```java
protected void load(ApplicationContext context, Object[] sources) {
   if (logger.isDebugEnabled()) {
      logger.debug(
            "Loading source " + StringUtils.arrayToCommaDelimitedString(sources));
   }
   //在这里就会生成BeanDefinitionLoader
   BeanDefinitionLoader loader = createBeanDefinitionLoader(
         getBeanDefinitionRegistry(context), sources);
   //同时也可以将beanname的生成策略加入到loader中
   if (this.beanNameGenerator != null) {
      loader.setBeanNameGenerator(this.beanNameGenerator);
   }
   if (this.resourceLoader != null) {
      loader.setResourceLoader(this.resourceLoader);
   }
   if (this.environment != null) {
      loader.setEnvironment(this.environment);
   }
    //然后注入到容器中去
   loader.load();
}
```

```java
BeanDefinitionLoader(BeanDefinitionRegistry registry, Object... sources) {
   Assert.notNull(registry, "Registry must not be null");
   Assert.notEmpty(sources, "Sources must not be empty");
   this.sources = sources;
    //注解配置
   this.annotatedReader = new AnnotatedBeanDefinitionReader(registry);
    //可以使xml的资源
   this.xmlReader = new XmlBeanDefinitionReader(registry);
   //也可以是Groovy的配置
   if (isGroovyPresent()) {
      this.groovyReader = new GroovyBeanDefinitionReader(registry);
   }
   //包扫描的配置
   this.scanner = new ClassPathBeanDefinitionScanner(registry);
   this.scanner.addExcludeFilter(new ClassExcludeFilter(sources));
}
```

- **刷新容器**

下来就是最重要的spring springframework的刷新容器了

- **刷新容器完后刷新**

![](bootimg\10.png)

该回调目前是空实现，还没有一个子类去实现它

- **容器启动事件回调**

  ```java
  listeners.started(context);
  ```

  此时容器已将启动完成了

- 回调

  If you need to run some specific code once the `SpringApplication` has started, you can implement the `ApplicationRunner` or `CommandLineRunner` interfaces. Both interfaces work in the same way and offer a single `run` method, which is called just before `SpringApplication.run(…)` completes.

  ==如果你需要在`SpringApplication`启动后运行一些特定的代码，你可以实现`ApplicationRunner`或`CommandLineRunner`接口。 两个接口以相同的方式工作，并提供单个`run`方法，该方法在SpringApplication.run（...）`完成之前调用。==

  The `CommandLineRunner` interfaces provides access to application arguments as a simple string array, whereas the `ApplicationRunner` uses the `ApplicationArguments` interface discussed earlier. The following example shows a `CommandLineRunner` with a `run` method:

  `CommandLineRunner`接口提供对应用程序参数的访问，作为一个简单的字符串数组，而`ApplicationRunner`使用前面讨论的`ApplicationArguments`接口 ==<u>这就是两个的区别</u>==

  ```
  import org.springframework.boot.*;
  import org.springframework.stereotype.*;
  
  @Component
  public class MyBean implements CommandLineRunner {
  
  	public void run(String... args) {
  		// Do something...
  	}
  
  }
  ```

  If several `CommandLineRunner` or `ApplicationRunner` beans are defined that must be called in a specific order, you can additionally implement the`org.springframework.core.Ordered` interface or use the `org.springframework.core.annotation.Order` annotation.

告诉我们，如果你要规定执行顺序，你可以另外实现`org.springframework.core.Ordered`接口或使用`org.springframework.core.annotation.Order` 注解。，这里会调用该代码排序（是不是很眼熟）

```java
AnnotationAwareOrderComparator.sort(runners);
```

- **异常处理以及后续回调工作**

![](bootimg\11.png)

如果出现异常怎么办

```java
private void handleRunFailure(ConfigurableApplicationContext context,
      Throwable exception,
      Collection<SpringBootExceptionReporter> exceptionReporters,
      SpringApplicationRunListeners listeners) {
   try {
      try {
         //获取错误码
         handleExitCode(context, exception);
          //发送容器启动失败回调
         if (listeners != null) {
            listeners.failed(context, exception);
         }
      }
      finally {
         //打印异常报告
         reportFailure(exceptionReporters, exception);
          //关闭容器
         if (context != null) {
            context.close();
         }
      }
   }
   catch (Exception ex) {
      logger.warn("Unable to close ApplicationContext", ex);
   }
   ReflectionUtils.rethrowRuntimeException(exception);
}
```

否则就是正常运行

```java

listeners.running(context);
```

回调spring正在运行的事件



上述所有完成后，返回IOC容器