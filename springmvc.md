# spring源码分析

## 一、servlet的分析

### 1.DispatcherServlet的整体分析

结构图

![](mvcimg\1.png)

- DispatcherServlet中，和spring相关的就只有三个servlet,一个是FrameworkServlet （骨架），HtttpServletBean（处理httpservlet的简单bean）；另外三个是jsr315规范的东西，HttpServlet，GenericServlet（通用servlet）和 servlet接口

- EnvironmentCapable接口是用来获取Environment环境的

- EnvironmentAware则是用来设置Environment环境的

- ApplicationContextAware则是用来设置spring容器的

  ```java
  如果单纯的在web.xml中配置配置该servlet，那么xxxAware根本不会被调用到的；
  
  但是如果你用了springboot，那么就会被调用到了，因为这里的DispatcherServlet会被注入进容器的，那么在spring实例化的时候就会调用到xxxAware接口
  
  spring在3.2.0RELEASE，才在HttpservletBean上实现了EnvironmentCapable和EnvironmentAware接口，那么至少在3.2.0以后，spring就在下一步大棋了
  ```

  

Environment环境这个东西是非常的重要，在spring中，他是在HttpServletBean  init()初始化的时候就被创建了，随后在spring刷新容器中的==prepareBeanFactory()==的时候被装载进容器的，mvc环境下默认创建了==`org.springframework.web.context.support.StandardServletEnvironment`==

```
需要注意的是，springmvc启动时在创建spring容器的同时就启动了容器，将容器进行刷新
```

#### 1.1  **ServletConfig**

ServletConfig：代表当前Servlet在web.xml中的配置信息

常用的方法：

getServletName（）                                     ----   获取当前servlet在web.xml的名字

getInitParameter（String name）              ----   获取当前servlet在web.xml配置的**==init-param==**参数

Enumeration getInitParameterNames()    ----   获取当前servlet在web.xml配置的所有**==init-param==**参数

ServletContext getServletContext()            ----   获取代表当前web应用的ServletContext对象

#### 1.2 属性访问器工厂（PropertyAccessorFactory）

spring作为比较给力的工具，当然避免不了提供一些工具类

```java
public abstract class PropertyAccessorFactory {

   /**
    * 获取beanWrapper
    */
   public static BeanWrapper forBeanPropertyAccess(Object target) {
      return new BeanWrapperImpl(target);
   }

   /**
    * 获取直接属性访问器
    */
   public static ConfigurablePropertyAccessor forDirectFieldAccess(Object target) {
      return new DirectFieldAccessor(target);
   }

}
```

Spring内部大量使用BeanWrapper进行属性取值赋值（setter/getter），在spring4.1之前，是没有一个工具能够直接获取对象字段值的，但是Spring 4.1提供了一个DirectFieldAccessor可以直接来获取到属性值和设置属性值

![](mvcimg\3.png)

#### 1.3 源码分析

拥有了这些知识就去看下springmvc的初始化过程

知道servlet的你一定知道sverlet在初始化的时候会调用init方法，那么继承自HttpServlet的HttpServletBean也覆盖了init方法

```java
@Override
public final void init() throws ServletException {
   if (logger.isDebugEnabled()) {
      logger.debug("Initializing servlet '" + getServletName() + "'");
   }

  
   try {
      //将ServletConfig（init-param）获得的参数封装到PropertyValues中去
      PropertyValues pvs = new ServletConfigPropertyValues(getServletConfig(), this.requiredProperties);
      //通过属性访问工厂获得一个beanWrapper
      BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(this);
      //得到以ServletContex为资源路径的ResourceLoader对象，用来为下面的属性解析器做铺垫
      ResourceLoader resourceLoader = new ServletContextResourceLoader(getServletContext());
      //向beanWrapper中注册ResourceEditor属性解析器
      bw.registerCustomEditor(Resource.class, new ResourceEditor(resourceLoader, getEnvironment()));
      //目前该模板方法还没有被用到
      initBeanWrapper(bw);
      //设置属性，一般这里已经解析到了DispatcherServlet的<init-param>中的<init-param>属性，并设置到FrameworkServlet的contextConfigLocation属性中了，为spring mvc容器初始化拿到了资源配置文件
      bw.setPropertyValues(pvs, true);
   }
   catch (BeansException ex) {
      logger.error("Failed to set bean properties on servlet '" + getServletName() + "'", ex);
      throw ex;
   }

   // 模板方法FrameworkServlet中有实现
   initServletBean();

   if (logger.isDebugEnabled()) {
      logger.debug("Servlet '" + getServletName() + "' configured successfully");
   }
}
```

 initServletBean()是比较重要的，来看看子类的 initServletBean干了些什么事

```java
protected final void initServletBean() throws ServletException {
   getServletContext().log("Initializing Spring FrameworkServlet '" + getServletName() + "'");
   if (this.logger.isInfoEnabled()) {
      this.logger.info("FrameworkServlet '" + getServletName() + "': initialization started");
   }
    //记录开始时间
   long startTime = System.currentTimeMillis();

   try {
      //获得springmvc容器
      this.webApplicationContext = initWebApplicationContext();
      //初始化FrameworkServlet，同样该模板方法还没有子类实现
      initFrameworkServlet();
   }
   catch (ServletException ex) {
      this.logger.error("Context initialization failed", ex);
      throw ex;
   }
   catch (RuntimeException ex) {
      this.logger.error("Context initialization failed", ex);
      throw ex;
   }

   //打印结束时间
   if (this.logger.isInfoEnabled()) {
      long elapsedTime = System.currentTimeMillis() - startTime;
      this.logger.info("FrameworkServlet '" + getServletName() + "': initialization completed in " +
            elapsedTime + " ms");
   }
}
```

该模板方法总体来说就干了一件事，实例化spring容器，这个方法非常重要

```java
protected WebApplicationContext initWebApplicationContext() {
   //从ServletContext拿取Root容器
   WebApplicationContext rootContext =
         WebApplicationContextUtils.getWebApplicationContext(getServletContext());
   WebApplicationContext wac = null;
   //如果不是空，但是一般这里都是空
   if (this.webApplicationContext != null) {
      //设置root,并刷新容器
      wac = this.webApplicationContext;
      if (wac instanceof ConfigurableWebApplicationContext) {
         ConfigurableWebApplicationContext cwac = (ConfigurableWebApplicationContext) wac;
         if (!cwac.isActive()) {
            // The context has not yet been refreshed -> provide services such as
            // setting the parent context, setting the application context id, etc
            if (cwac.getParent() == null) {
               // The context instance was injected without an explicit parent -> set
               // the root application context (if any; may be null) as the parent
               cwac.setParent(rootContext);
            }
            configureAndRefreshWebApplicationContext(cwac);
         }
      }
   }
   if (wac == null) {
      //在构造时没有注入上下文实例，试着从ServletContext 的contextAttribute属性中找一个
      wac = findWebApplicationContext();
   }
   if (wac == null) {
      //如果还是没有，就生成一个
      wac = createWebApplicationContext(rootContext);
   }

   if (!this.refreshEventReceived) {
      // Either the context is not a ConfigurableApplicationContext with refresh
      // support or the context injected at construction time had already been
      // refreshed -> trigger initial onRefresh manually here.
      onRefresh(wac);
   }

   if (this.publishContext) {
      // Publish the context as a servlet context attribute.
      String attrName = getServletContextAttributeName();
      getServletContext().setAttribute(attrName, wac);
      if (this.logger.isDebugEnabled()) {
         this.logger.debug("Published WebApplicationContext of servlet '" + getServletName() +
               "' as ServletContext attribute with name [" + attrName + "]");
      }
   }

   return wac;
}
```

对于spring mvc来说createWebApplicationContext（）这个方法是比较重要的，下面看这个方法干了什么事情

```java
protected WebApplicationContext createWebApplicationContext(ApplicationContext parent) {
   //从静态属性中得到一个 XmlWebApplicationContext.class实例
   Class<?> contextClass = getContextClass();
   if (this.logger.isDebugEnabled()) {
      this.logger.debug("Servlet with name '" + getServletName() +
            "' will try to create custom WebApplicationContext context of class '" +
            contextClass.getName() + "'" + ", using parent context [" + parent + "]");
   }
   //判断是不是ConfigurableWebApplicationContext类型
   if (!ConfigurableWebApplicationContext.class.isAssignableFrom(contextClass)) {
      throw new ApplicationContextException(
            "Fatal initialization error in servlet with name '" + getServletName() +
            "': custom WebApplicationContext class [" + contextClass.getName() +
            "] is not of type ConfigurableWebApplicationContext");
   }
   //实例化一个XmlWebApplicationContext
   ConfigurableWebApplicationContext wac =
         (ConfigurableWebApplicationContext) BeanUtils.instantiateClass(contextClass);
   //设置环境
   wac.setEnvironment(getEnvironment());
   //设置父容器
   wac.setParent(parent);
   //设置配置文件路径
   wac.setConfigLocation(getContextConfigLocation());

   //配置并刷新容器
   configureAndRefreshWebApplicationContext(wac);

   return wac;
}
```

说到这里，基本上servlet的源码分析暂时先告一段落，来简单分析下各个组件的源码，再统一看下springmvc的请求处理过程，不然，也看不懂组件之间是怎么串联和使用的，也就看不懂后续源码了

## 二、springmvc中部分组件的使用以及原理

## 2.1 @sessionattributes

### 2.1.1 @sessionattributes注解

接口

```java
/**
 * ElementType.TYPE：该注解只作用于类上
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface SessionAttributes {
    
   String[] value() default {};

   Class<?>[] types() default {};

}
```

@sessionattributes作用在处理器类上，用于在多个参数之间传递参数，==类似于session的Attribute==，但并不完全一样。一般来说@sessionattributes传递的参数只用于暂时的传递，而不是长期的保存，像身份验证信息这种需要长期保存的参数还是应该使用 session#setAttribute设置到session中

### 2.1.2 源码分析

操作sesssion的接口

```java

/**
 * 用于在后端会话中存储模型属性的策略接口
 */
public interface SessionAttributeStore {

   /**
    * 存储或者修改
    */
   void storeAttribute(WebRequest request, String attributeName, Object attributeValue);

   /**
    * 从后端会话中检索指定的属性
    */
   Object retrieveAttribute(WebRequest request, String attributeName);

   /**
    * 清除后端会话中的指定属性
    */
   void cleanupAttribute(WebRequest request, String attributeName);

}
```

```java
//操作session的接口实现,如果自己的seesion是集群，就要考虑实现SessionAttributeStore接口了
public class DefaultSessionAttributeStore implements SessionAttributeStore {

   //属性名称前缀
   private String attributeNamePrefix = "";


   /**
    * 指定用于后端会话中的属性名称的前缀。
    * 默认是不使用前缀，存储会话属性与模型中的名称相同。
    *
    */
   public void setAttributeNamePrefix(String attributeNamePrefix) {
      this.attributeNamePrefix = (attributeNamePrefix != null ? attributeNamePrefix : "");
   }


   @Override
   public void storeAttribute(WebRequest request, String attributeName, Object attributeValue) {
      Assert.notNull(request, "WebRequest must not be null");
      Assert.notNull(attributeName, "Attribute name must not be null");
      Assert.notNull(attributeValue, "Attribute value must not be null");
      String storeAttributeName = getAttributeNameInSession(request, attributeName);
      //将attributeName的值设置进session中
      request.setAttribute(storeAttributeName, attributeValue, WebRequest.SCOPE_SESSION);
   }

   @Override
   public Object retrieveAttribute(WebRequest request, String attributeName) {
      Assert.notNull(request, "WebRequest must not be null");
      Assert.notNull(attributeName, "Attribute name must not be null");
      String storeAttributeName = getAttributeNameInSession(request, attributeName);
      //检错attributeName的值
      return request.getAttribute(storeAttributeName, WebRequest.SCOPE_SESSION);
   }

   @Override
   public void cleanupAttribute(WebRequest request, String attributeName) {
      Assert.notNull(request, "WebRequest must not be null");
      Assert.notNull(attributeName, "Attribute name must not be null");
      String storeAttributeName = getAttributeNameInSession(request, attributeName);
      //清空attributeName的值
      request.removeAttribute(storeAttributeName, WebRequest.SCOPE_SESSION);
   }


   /**
    * 获取前缀+attributeName的值
    * 
    */
   protected String getAttributeNameInSession(WebRequest request, String attributeName) {
      return this.attributeNamePrefix + attributeName;
   }
```

SessionAttributes的处理器，也就是处理实现

```java
/**
 * 通过声明的@SessionAttributes注解来声明，实际是委托给SessionAttributeStore接口来实现的
 *
 * 当一个controller被@SessionAttributes直接标注后，通过@SessionAttributes标记的属性名称和
 * 类型的数据会被保存在http的会话中，直到controller调用SessionStatus#setComplete()后，这些数据将会 
 * 被清理
 * @since 3.1 从3.1开始实现的
 */
public class SessionAttributesHandler {

   //存储@SessionAttributes  value属性对应的值，也就是参数名
   private final Set<String> attributeNames = new HashSet<String>();

   //存储@SessionAttributes  type属性对应的值，也就是参数类型
   private final Set<Class<?>> attributeTypes = new HashSet<Class<?>>();
    /**
     * 
     * 这个作用就是保存了attributeNames和attributeTypes中的所有值，清除的话只需要遍历knownAttributeNames就可以了，不需要遍历上面两个
     */
   private final Set<String> knownAttributeNames =
         Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>(4));

    
   //具体执行保存到session的操作接口实现
   private final SessionAttributeStore sessionAttributeStore;


   public SessionAttributesHandler(Class<?> handlerType, SessionAttributeStore sessionAttributeStore) {
      Assert.notNull(sessionAttributeStore, "SessionAttributeStore may not be null.");
      this.sessionAttributeStore = sessionAttributeStore;
      //查看是否标注了@SessionAttributes注解，如果注解了，就将数据保存起来
      SessionAttributes annotation = AnnotationUtils.findAnnotation(handlerType, SessionAttributes.class);
      if (annotation != null) {
         this.attributeNames.addAll(Arrays.asList(annotation.value()));
         this.attributeTypes.addAll(Arrays.<Class<?>>asList(annotation.types()));
      }
      //这里没有加attributeTypes是因为这里我也不知道要设置什么名字的AttributeNames，等到用到的时候才
      //能知道是哪个名字的属性，才加进来
      for (String attributeName : this.attributeNames) {
         this.knownAttributeNames.add(attributeName);
      }
   }

   /**
    * 判断是否标记了@SessionAttributes注解
    */
   public boolean hasSessionAttributes() {
      return ((this.attributeNames.size() > 0) || (this.attributeTypes.size() > 0));
   }

   /**
    * 属性名称或类型是否与指定的名称和类型匹配
    */
   public boolean isHandlerSessionAttribute(String attributeName, Class<?> attributeType) {
      Assert.notNull(attributeName, "Attribute name must not be null");
      if (this.attributeNames.contains(attributeName) || this.attributeTypes.contains(attributeType)) {
         this.knownAttributeNames.add(attributeName);
         return true;
      }
      else {
         return false;
      }
   }

   /**
    * 在会话中存储给定属性的子集，如果不是，则直接忽略掉
    */
   public void storeAttributes(WebRequest request, Map<String, ?> attributes) {
      for (String name : attributes.keySet()) {
         Object value = attributes.get(name);
         Class<?> attrType = (value != null) ? value.getClass() : null;

         if (isHandlerSessionAttribute(name, attrType)) {
            this.sessionAttributeStore.storeAttribute(request, name, value);
         }
      }
   }

   /**
    *从会话中检索“已知”属性，即列出的属性
    */
   public Map<String, Object> retrieveAttributes(WebRequest request) {
      Map<String, Object> attributes = new HashMap<String, Object>();
      for (String name : this.knownAttributeNames) {
         Object value = this.sessionAttributeStore.retrieveAttribute(request, name);
         if (value != null) {
            attributes.put(name, value);
         }
      }
      return attributes;
   }

   /**
    * 从会话中删除“已知”属性，即列出的属性
    */
   public void cleanupAttributes(WebRequest request) {
      for (String attributeName : this.knownAttributeNames) {
         this.sessionAttributeStore.cleanupAttribute(request, attributeName);
      }
   }

   /**
    * 对底层SessionAttributeStore的传递调用
    */
   Object retrieveAttribute(WebRequest request, String attributeName) {
      return this.sessionAttributeStore.retrieveAttribute(request, attributeName);
   }

}
```

这里值得注意的是，knownAttributeNames是通过**==SetFromMap==**通过ConcurrentHashMap来实现并发容器的，是不是又学到点东西 ，具体关于setFromMap的简单介绍可以看下这个[链接](https://www.jianshu.com/p/f25d9d7fdaf3)

## 2.2  WebDataBinder注解

### 2.2.1 WebDataBinderFactory

```java
/**
 * 用于创建WebDataBinder的工厂方法
 * @since 3.1
 */
public interface WebDataBinderFactory {

   /**
    * 为给定对象创建WebDataBinder
    */
   WebDataBinder createBinder(NativeWebRequest webRequest, Object target, String objectName) throws Exception;

}
```

![](mvcimg\4.png)

可以看到WebDataBinderFactory的实现有下列三个，分别是： 



- DefaultDataBinderFactory的实现，基本上就一个很基础的实现。其实没有干太多的事情

```java
/**
 *创建一个WebRequestDataBinder，通过web绑定初始化器（在springmvc的实现中，如果加入了
 <mvc:annotation-driven>的配置 springmvc会自动向容器中注册一ConfigurableWebBindingInitializer
 springboot会在WebMvcAutoConfiguration也会注册一个的）
 * {@link WebBindingInitializer}.
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class DefaultDataBinderFactory implements WebDataBinderFactory {

   private final WebBindingInitializer initializer;

  
   public DefaultDataBinderFactory(WebBindingInitializer initializer) {
      this.initializer = initializer;
   }

   /**
    * 创建一个WebDataBinder并用WebBindingInitializer初始化它
    * @throws Exception in case of invalid state or arguments
    */
   @Override
   public final WebDataBinder createBinder(NativeWebRequest webRequest, Object target, String objectName)
         throws Exception {
      WebDataBinder dataBinder = createBinderInstance(target, objectName, webRequest);
      if (this.initializer != null) {
         this.initializer.initBinder(dataBinder, webRequest);
      }
      //初始化的工作交由子类去实现
      initBinder(dataBinder, webRequest);
      return dataBinder;
   }

  
   protected WebDataBinder createBinderInstance(Object target, String objectName, NativeWebRequest webRequest)
         throws Exception {
      return new WebRequestDataBinder(target, objectName);
   }

   /**
    * 进一步初始化创建的数据绑定器实例
     *（例如使用{@code @InitBinder}方法）在“全局”初始化之后
    */
   protected void initBinder(WebDataBinder dataBinder, NativeWebRequest webRequest) throws Exception {
   }

}
```

- InitBinderDataBinderFactor是通过 @InitBinder方法向WebDataBinder添加初始化的

  ```java
  public class InitBinderDataBinderFactory extends DefaultDataBinderFactory {
  
     private final List<InvocableHandlerMethod> binderMethods;
  
     /**
      * 创建一个实例，将@InitBinder标注的方法传递进来
      */
     public InitBinderDataBinderFactory(List<InvocableHandlerMethod> binderMethods, WebBindingInitializer initializer) {
        super(initializer);
        this.binderMethods = (binderMethods != null) ? binderMethods : new ArrayList<InvocableHandlerMethod>();
     }
  
     /**
      * 初始化WebDataBinder
      */
     @Override
     public void initBinder(WebDataBinder binder, NativeWebRequest request) throws Exception {
        for (InvocableHandlerMethod binderMethod : this.binderMethods) {
           if (isBinderMethodApplicable(binderMethod, binder)) {
              //判断这个binderMethod是否有返回值，如果有返回值的话直接抛错
              Object returnValue = binderMethod.invokeForRequest(request, null, binder);
              if (returnValue != null) {
                 throw new IllegalStateException("@InitBinder methods should return void: " + binderMethod);
              }
           }
        }
     }
  
     /**
      * 返回是否包含InitBinder的方法
      */
     protected boolean isBinderMethodApplicable(HandlerMethod initBinderMethod, WebDataBinder binder) {
        InitBinder annot = initBinderMethod.getMethodAnnotation(InitBinder的方法.class);
        Collection<String> names = Arrays.asList(annot.value());
        return (names.size() == 0 || names.contains(binder.getObjectName()));
     }
  
  }
  ```

- ServletRequestDataBinderFactory：

  ```java
  /**
   * 创建一个ServletRequestDataBinder的子类ExtendedServletRequestDataBinder
   *
   * @since 3.1
   */
  public class ServletRequestDataBinderFactory extends InitBinderDataBinderFactory {
  
     public ServletRequestDataBinderFactory(List<InvocableHandlerMethod> binderMethods, WebBindingInitializer initializer) {
        super(binderMethods, initializer);
     }
  
     /**
      * 返回一个ExtendedServletRequestDataBinder
      */
     @Override
     protected ServletRequestDataBinder createBinderInstance(Object target, String objectName, NativeWebRequest request) {
        return new ExtendedServletRequestDataBinder(target, objectName);
     }
  
  }
  ```

## 2.3  HandlerMethod（处理方法）

先分析下继承体系

![](mvcimg\5.png)

可以看到HandlerMethod下面有三个继承的类，他们分别处理了不同的东西

1. HandlerMethod 封装方法定义相关的信息,如类,方法,参数等.

　　 使用场景:HandlerMapping时会使用

2. InvocableHandlerMethod 添加参数准备,方法调用功能

   　　使用场景:执行使用@ModelAttribute注解会使用

3. ServletInvocableHandlerMethod 添加返回值处理职责,ResponseStatus处理

   　　使用场景:执行http相关方法会使用,比如调用处理执行

### 2.3.1 HandlerMethod

```java
public class HandlerMethod {

   /** 给子类提供log日志 */
   protected final Log logger = LogFactory.getLog(HandlerMethod.class);

   //方法所在的类,如果是String类型,可以去容器中获取
   private final Object bean;
   //类管理的容器
   private final BeanFactory beanFactory;
   //方法
   private final Method method;
   //如果方法是bridged方法,则对应原始方法（桥接方法后续可能会分析）
   private final Method bridgedMethod;
   //方法参数
   private final MethodParameter[] parameters;

  
   /**
    * 如果bean是string类型，则从IOC容器中找
    */
   public HandlerMethod createWithResolvedBean() {
      Object handler = this.bean;
      if (this.bean instanceof String) {
         String beanName = (String) this.bean;
         handler = this.beanFactory.getBean(beanName);
      }
      return new HandlerMethod(this, handler);
   }


}
```

### 2.3.2  InvocableHandlerMethod（可调用的HandlerMethod）

在父类HandlerMethod的基础上添加了调用的功能，也就是InvocableHandlerMethod可以直接调用内部的Method对应的方法

```java
/**
 * 提供了一种HandlerMethodArgumentResolver来解析的HandlerMethod方法
 * @since 3.1
 */
public class InvocableHandlerMethod extends HandlerMethod {

   //web数据绑定工厂，用于参数解析器中ArgumentResolver中
   private WebDataBinderFactory dataBinderFactory;

   //参数解析器
   private HandlerMethodArgumentResolverComposite argumentResolvers = new HandlerMethodArgumentResolverComposite();

   //参数名查找器，用于MethodParameter中
   private ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();




   /**
    * 在给定请求的上下文中解析其参数值后调用该方法。
    * 参数值通常通过HandlerMethodArgumentResolver来解决
    * {@code provideArgs}参数可以提供要直接使用的参数值，
    *  没有参数解析。 提供的参数值的示例包括WebDataBinder， SessionStatus或抛出的异常实例。
    *在参数解析器之前检查提供的参数值。
    */
   public Object invokeForRequest(NativeWebRequest request, ModelAndViewContainer mavContainer,
         Object... providedArgs) throws Exception {
      //解析参数
      Object[] args = getMethodArgumentValues(request, mavContainer, providedArgs);
      if (logger.isTraceEnabled()) {
         StringBuilder sb = new StringBuilder("Invoking [");
         sb.append(getBeanType().getSimpleName()).append(".");
         sb.append(getMethod().getName()).append("] method with arguments ");
         sb.append(Arrays.asList(args));
         logger.trace(sb.toString());
      }
      //调用方法
      Object returnValue = doInvoke(args);
      if (logger.isTraceEnabled()) {
         logger.trace("Method [" + getMethod().getName() + "] returned [" + returnValue + "]");
      }
      return returnValue;
   }

   /**
    * 获取当前请求的方法参数值。
    */
   private Object[] getMethodArgumentValues(NativeWebRequest request, ModelAndViewContainer mavContainer,
         Object... providedArgs) throws Exception {
      //拿到请求的方法参数
      MethodParameter[] parameters = getMethodParameters();
      Object[] args = new Object[parameters.length];
      for (int i = 0; i < parameters.length; i++) {
         MethodParameter parameter = parameters[i];
         parameter.initParameterNameDiscovery(this.parameterNameDiscoverer);
         //获取参数的类型和所属的类
         GenericTypeResolver.resolveParameterType(parameter, getBean().getClass());
         // 如果提供了参数的值(providedArgs)的话，直接返回
         args[i] = resolveProvidedArgument(parameter, providedArgs);
         if (args[i] != null) {
            continue;
         }
          // 使用参数解析器解析
         if (this.argumentResolvers.supportsParameter(parameter)) {
            try {
               //解析
               args[i] = this.argumentResolvers.resolveArgument(
                     parameter, mavContainer, request, this.dataBinderFactory);
               continue;
            }
            catch (Exception ex) {
               if (logger.isTraceEnabled()) {
                  logger.trace(getArgumentResolutionErrorMessage("Error resolving argument", i), ex);
               }
               throw ex;
            }
         }
         if (args[i] == null) {
            String msg = getArgumentResolutionErrorMessage("No suitable resolver for argument", i);
            throw new IllegalStateException(msg);
         }
      }
      return args;
   }

    
    /**
    * 参数解析错误
    */
   private String getArgumentResolutionErrorMessage(String message, int index) {
      MethodParameter param = getMethodParameters()[index];
      message += " [" + index + "] [type=" + param.getParameterType().getName() + "]";
      return getDetailedErrorMessage(message);
   }

   /**
    * 将HandlerMethod详细信息（例如控制器类型和方法签名）添加到给定的错误消息中。.
    */
   protected String getDetailedErrorMessage(String message) {
      StringBuilder sb = new StringBuilder(message).append("\n");
      sb.append("HandlerMethod details: \n");
      sb.append("Controller [").append(getBeanType().getName()).append("]\n");
      sb.append("Method [").append(getBridgedMethod().toGenericString()).append("]\n");
      return sb.toString();
   }

   /**
    * 尝试从给定的参数值列表中解析方法参数
    */
   private Object resolveProvidedArgument(MethodParameter parameter, Object... providedArgs) {
      if (providedArgs == null) {
         return null;
      }
      for (Object providedArg : providedArgs) {
         if (parameter.getParameterType().isInstance(providedArg)) {
            return providedArg;
         }
      }
      return null;
   }


   /**
    * 使用给定的参数值调用方法
    */
   protected Object doInvoke(Object... args) throws Exception {
      //调用之前先将方法设置为可调用，也就是private也可以调用的
      ReflectionUtils.makeAccessible(getBridgedMethod());
      try {
         return getBridgedMethod().invoke(getBean(), args);
      }
      catch (IllegalArgumentException ex) {
         assertTargetBean(getBridgedMethod(), getBean(), args);
         throw new IllegalStateException(getInvocationErrorMessage(ex.getMessage(), args), ex);
      }
      catch (InvocationTargetException ex) {
         //展开HandlerExceptionResolvers
         Throwable targetException = ex.getTargetException();
         if (targetException instanceof RuntimeException) {
            throw (RuntimeException) targetException;
         }
         else if (targetException instanceof Error) {
            throw (Error) targetException;
         }
         else if (targetException instanceof Exception) {
            throw (Exception) targetException;
         }
         else {
            String msg = getInvocationErrorMessage("Failed to invoke controller method", args);
            throw new IllegalStateException(msg, targetException);
         }
      }
   }

   /**
    * Assert that the target bean class is an instance of the class where the given
    * method is declared. In some cases the actual controller instance at request-
    * processing time may be a JDK dynamic proxy (lazy initialization, prototype
    * beans, and others). {@code @Controller}'s that require proxying should prefer
    * class-based proxy mechanisms.
    */
   private void assertTargetBean(Method method, Object targetBean, Object[] args) {
      Class<?> methodDeclaringClass = method.getDeclaringClass();
      Class<?> targetBeanClass = targetBean.getClass();
      if (!methodDeclaringClass.isAssignableFrom(targetBeanClass)) {
         String msg = "The mapped controller method class '" + methodDeclaringClass.getName() +
               "' is not an instance of the actual controller bean instance '" +
               targetBeanClass.getName() + "'. If the controller requires proxying " +
               "(e.g. due to @Transactional), please use class-based proxying.";
         throw new IllegalStateException(getInvocationErrorMessage(msg, args));
      }
   }

   private String getInvocationErrorMessage(String message, Object[] resolvedArgs) {
      StringBuilder sb = new StringBuilder(getDetailedErrorMessage(message));
      sb.append("Resolved arguments: \n");
      for (int i=0; i < resolvedArgs.length; i++) {
         sb.append("[").append(i).append("] ");
         if (resolvedArgs[i] == null) {
            sb.append("[null] \n");
         }
         else {
            sb.append("[type=").append(resolvedArgs[i].getClass().getName()).append("] ");
            sb.append("[value=").append(resolvedArgs[i]).append("]\n");
         }
      }
      return sb.toString();
   }

}
```

### 2.3.3 ServletInvocableHandlerMethod（基于Servlet）

在父类InvocableHandlerMethod的基础上增加了三个功能

1. 对@ResponseStatus注解的支持

   ```java
   /**
    * @since 3.0
    */
   @Target({ElementType.TYPE, ElementType.METHOD})
   @Retention(RetentionPolicy.RUNTIME)
   @Documented
   public @interface ResponseStatus {
       
      HttpStatus value();
   
      String reason() default "";
   
   }
   ```

2. 对返回值的处理

3. 对异步结果的处理

```java
/**
 *扩展{@link InvocableHandlerMethod}，能够通过已注册的{@link HandlerMethodReturnValueHandler}处理返回值，还支持根据方法级{@code @ResponseStatus}注释设置响应状态。
 *
 * @since 3.1
 */
public class ServletInvocableHandlerMethod extends InvocableHandlerMethod {

   private static final Method CALLABLE_METHOD = ClassUtils.getMethod(Callable.class, "call");

   //@ResponseStatus注解对应的value值
   private HttpStatus responseStatus;
   //@ResponseStatus注解对应的reason
   private String responseReason;
   //对返回值的处理
   private HandlerMethodReturnValueHandlerComposite returnValueHandlers;


   public ServletInvocableHandlerMethod(Object handler, Method method) {
      super(handler, method);
      //
      initResponseStatus();
   }
   //查询方法是否注释了@ResponseStatus，并初始化内部成员
   private void initResponseStatus() {
      ResponseStatus annotation = getMethodAnnotation(ResponseStatus.class);
      if (annotation != null) {
         this.responseStatus = annotation.value();
         this.responseReason = annotation.reason();
      }
   }

  

   /**
    * 注册HandlerMethodReturnValueHandler，用于返回值的解析
    */
   public void setHandlerMethodReturnValueHandlers(HandlerMethodReturnValueHandlerComposite returnValueHandlers) {
      this.returnValueHandlers = returnValueHandlers;
   }

   /**
    * 对于返回值的处理是通过HandlerMethodReturnValueHandler来处理的
    * 处理请求就是用的invokeAndHandle
    */
   public void invokeAndHandle(ServletWebRequest webRequest,
         ModelAndViewContainer mavContainer, Object... providedArgs) throws Exception {
      //先调用父类invokeForRequest来调用方法，拿到返回值
      Object returnValue = invokeForRequest(webRequest, mavContainer, providedArgs);
      //然后对@ResponseStatus注解做处理
      setResponseStatus(webRequest);
      //如果返回值时空的
      if (returnValue == null) {
         //如果NotModified为true,或者ResponseStatus不为空，或者RequestHandled为true，
         //这标识该请求已经处理，并且返回
         if (isRequestNotModified(webRequest) || hasResponseStatus() || mavContainer.isRequestHandled()) {
            mavContainer.setRequestHandled(true);
            return;
         }
      }
      //如果ResponseStatus有理由，也设置不需要处理返回这
      else if (StringUtils.hasText(this.responseReason)) {
         mavContainer.setRequestHandled(true);
         return;
      }
      //如果都不是，那就设置false
      mavContainer.setRequestHandled(false);
      try {
         //处理返回值
         this.returnValueHandlers.handleReturnValue(
               returnValue, getReturnValueType(returnValue), mavContainer, webRequest);
      }
      catch (Exception ex) {
         if (logger.isTraceEnabled()) {
            logger.trace(getReturnValueHandlingErrorMessage("Error handling return value", returnValue), ex);
         }
         throw ex;
      }
   }

   /**
    * Set the response status according to the {@link ResponseStatus} annotation.
    */
   private void setResponseStatus(ServletWebRequest webRequest) throws IOException {
      if (this.responseStatus == null) {
         return;
      }
      if (StringUtils.hasText(this.responseReason)) {
         webRequest.getResponse().sendError(this.responseStatus.value(), this.responseReason);
      }
      else {
         webRequest.getResponse().setStatus(this.responseStatus.value());
      }
      // 设置到request预，在redirect中使用
      webRequest.getRequest().setAttribute(View.RESPONSE_STATUS_ATTRIBUTE, this.responseStatus);
   }

}
```

## 2.4 ModelFactory

ModelFactory是用来维护model的，看名字是个工厂方法，那么当然是model的工厂

```java
/**
 * Model的工厂
 * @since 3.1
 */
public final class ModelFactory {

   private final List<ModelMethod> modelMethods = new ArrayList<ModelMethod>();
   //web数据绑定器工厂
   private final WebDataBinderFactory dataBinderFactory;
   //SessionAttributes的处理器
   private final SessionAttributesHandler sessionAttributesHandler;


   /**
    * 初始化ModelFactory，并且设置被@ModelAttrabute修饰的方法以及方法参数的封装
    */
   public ModelFactory(List<InvocableHandlerMethod> invocableMethods, WebDataBinderFactory dataBinderFactory,
         SessionAttributesHandler sessionAttributesHandler) {

      if (invocableMethods != null) {
         for (InvocableHandlerMethod method : invocableMethods) {
            this.modelMethods.add(new ModelMethod(method));
         }
      }
      this.dataBinderFactory = dataBinderFactory;
      this.sessionAttributesHandler = sessionAttributesHandler;
   }

   /**
    * 初始化model
    * 
    */
   public void initModel(NativeWebRequest request, ModelAndViewContainer mavContainer, HandlerMethod handlerMethod)
         throws Exception {
      //从sessionAttributes中检索出所有设置的Attributename的键值
      Map<String, ?> sessionAttributes = this.sessionAttributesHandler.retrieveAttributes(request);
      //将sessionAttributes检索的属性加入到mavContainer的model中
      mavContainer.mergeAttributes(sessionAttributes);
      //执行注释了ModelAttribute方法，并且将结果设置到model中
      //如果mavContainer中已经有了名字一样的，那就不设置了，直接丢了
      invokeModelAttributeMethods(request, mavContainer);
      //如果在@ModelAttribute中有，并且在其他controller的@sessionAttributes也在的话
      for (String name : findSessionAttributeArguments(handlerMethod)) {        
         if (!mavContainer.containsAttribute(name)) {
             //如果mavContainer中不存在，则从@sessionAttributes注解中拿到，则加入进去
            Object value = this.sessionAttributesHandler.retrieveAttribute(request, name);
            if (value == null) {
               throw new HttpSessionRequiredException("Expected session attribute '" + name + "'");
            }
            mavContainer.addAttribute(name, value);
         }
      }
   }

   /**
    * 调用@ModelAttributeMethod注释的方法，并且将结果设置到model中
    */
   private void invokeModelAttributeMethods(NativeWebRequest request, ModelAndViewContainer mavContainer)
         throws Exception {
      //如果@ModelAttributeMethod注释的方法不是空
      while (!this.modelMethods.isEmpty()) {
         //拿到一个，并且将其remove掉
         InvocableHandlerMethod attrMethod = getNextModelMethod(mavContainer).getHandlerMethod();
         //获取方法上的@ModelAttribute注解的value值
         String modelName = attrMethod.getMethodAnnotation(ModelAttribute.class).value();
         //如果在mavContainer中已经存在了，那么就直接跳过
         if (mavContainer.containsAttribute(modelName)) {
            continue;
         }
         //否则就执行该方法
         Object returnValue = attrMethod.invokeForRequest(request, mavContainer);
        
         if (!attrMethod.isVoid()){
             //如果这个方法不是Void的，则获取返回值类型的名字
            //（如果方法@ModelAttribute有value值，名字就是这个value值），
            //否则就是返回值类型的简单名称
            String returnValueName = getNameForReturnValue(returnValue, attrMethod.getReturnType());
             //如果mavContainer中没有，将返回值加入到model中
            if (!mavContainer.containsAttribute(returnValueName)) {
               mavContainer.addAttribute(returnValueName, returnValue);
            }
         }
      }
   }

   private ModelMethod getNextModelMethod(ModelAndViewContainer mavContainer) {
      //循环遍历
      for (ModelMethod modelMethod : this.modelMethods) {
          //mavContainer中是有已经有这个属性
         if (modelMethod.checkDependencies(mavContainer)) {
            if (logger.isTraceEnabled()) {
               logger.trace("Selected @ModelAttribute method " + modelMethod);
            }
            //如果有，则删除掉，并且返回回去
            this.modelMethods.remove(modelMethod);
            return modelMethod;
         }
      }
      //否则就拿到第一个，并且删除掉自己，返回回去
      ModelMethod modelMethod = this.modelMethods.get(0);
      if (logger.isTraceEnabled()) {
         logger.trace("Selected @ModelAttribute method (not present: " +
               modelMethod.getUnresolvedDependencies(mavContainer)+ ") " + modelMethod);
      }
      this.modelMethods.remove(modelMethod);
      return modelMethod;
   }

   /**
    * Find {@code @ModelAttribute} arguments also listed as {@code @SessionAttributes}.
    */
   private List<String> findSessionAttributeArguments(HandlerMethod handlerMethod) {
      List<String> result = new ArrayList<String>();
      for (MethodParameter parameter : handlerMethod.getMethodParameters()) {
         if (parameter.hasParameterAnnotation(ModelAttribute.class)) {
            String name = getNameForParameter(parameter);
            if (this.sessionAttributesHandler.isHandlerSessionAttribute(name, parameter.getParameterType())) {
               result.add(name);
            }
         }
      }
      return result;
   }

   /**
    * Derives the model attribute name for a method parameter based on:
    */
   public static String getNameForParameter(MethodParameter parameter) {
      ModelAttribute annot = parameter.getParameterAnnotation(ModelAttribute.class);
      String attrName = (annot != null) ? annot.value() : null;
      return StringUtils.hasText(attrName) ? attrName :  Conventions.getVariableNameForParameter(parameter);
   }

   /**
    *如果方法@ModelAttribute有value值，名字就是这个value值），否则就是返回值类型的简单名称
      String       ->  string
    * ClassUtils   ->  classUtils
      List<Double> ->  doubleList
      Set<Double>  ->  doubleList
      Double[]     ->  doubleList
    */
   public static String getNameForReturnValue(Object returnValue, MethodParameter returnType) {
      ModelAttribute annotation = returnType.getMethodAnnotation(ModelAttribute.class);
      if (annotation != null && StringUtils.hasText(annotation.value())) {
         return annotation.value();
      }
      else {
         Method method = returnType.getMethod();
         Class<?> resolvedType = GenericTypeResolver.resolveReturnType(method, returnType.getContainingClass());
         return Conventions.getVariableNameForReturnType(method, resolvedType, returnValue);
      }
   }

  /**
   *
   */
   public void updateModel(NativeWebRequest request, ModelAndViewContainer mavContainer) throws Exception {
      ModelMap defaultModel = mavContainer.getDefaultModel();
       //如果SessionStatus.isComplete()方法被调用，则清理sessionAttributes
      if (mavContainer.getSessionStatus().isComplete()){
         this.sessionAttributesHandler.cleanupAttributes(request);
      }
      //检查所有的ModelMap中是否在sessionAttributes中存在，如果存在就更新掉或者存储掉
      else {
         this.sessionAttributesHandler.storeAttributes(request, defaultModel);
      }
      //其实也就是判断需不需要渲染页面，如果需要渲染则给则给model中相应参数设置BindingResult
      if (!mavContainer.isRequestHandled() && mavContainer.getModel() == defaultModel) {
         updateBindingResult(request, defaultModel);
      }
   }

   /**
    * Add {@link BindingResult} attributes to the model for attributes that require it.
    */
   private void updateBindingResult(NativeWebRequest request, ModelMap model) throws Exception {
      List<String> keyNames = new ArrayList<String>(model.keySet());
      for (String name : keyNames) {
         Object value = model.get(name);

         if (isBindingCandidate(name, value)) {
            String bindingResultKey = BindingResult.MODEL_KEY_PREFIX + name;
  
            if (!model.containsAttribute(bindingResultKey)) {
               WebDataBinder dataBinder = dataBinderFactory.createBinder(request, value, name);
               model.put(bindingResultKey, dataBinder.getBindingResult());
            }
         }
      }
   }

   /**
    * Whether the given attribute requires a {@link BindingResult} in the model.
    */
   private boolean isBindingCandidate(String attributeName, Object value) {
      if (attributeName.startsWith(BindingResult.MODEL_KEY_PREFIX)) {
         return false;
      }

      Class<?> attrType = (value != null) ? value.getClass() : null;
      if (this.sessionAttributesHandler.isHandlerSessionAttribute(attributeName, attrType)) {
         return true;
      }

      return (value != null && !value.getClass().isArray() && !(value instanceof Collection) &&
            !(value instanceof Map) && !BeanUtils.isSimpleValueType(value.getClass()));
   }

   /**
    * 用于封装@modeAttribute注解的方法和注释了@modeAttribute参数名称
    */
   private static class ModelMethod {
      //调用方法
      private final InvocableHandlerMethod handlerMethod;
      //依赖
      private final Set<String> dependencies = new HashSet<String>();

      private ModelMethod(InvocableHandlerMethod handlerMethod) {
         this.handlerMethod = handlerMethod;
         for (MethodParameter parameter : handlerMethod.getMethodParameters()) {
             //查看是否参数有@ModelAttribute
            if (parameter.hasParameterAnnotation(ModelAttribute.class)) {              //将ModelAttribute vule属性或者参数名称加入到依赖中
               this.dependencies.add(getNameForParameter(parameter));
            }
         }
      }

      public InvocableHandlerMethod getHandlerMethod() {
         return this.handlerMethod;
      }

      /**
       * 判断mavContainer是否已经有了，如果没有就返回false，其他的返回true
       */
      public boolean checkDependencies(ModelAndViewContainer mavContainer) {
         for (String name : this.dependencies) {
            if (!mavContainer.containsAttribute(name)) {
               return false;
            }
         }
         return true;
      }

      public List<String> getUnresolvedDependencies(ModelAndViewContainer mavContainer) {
         List<String> result = new ArrayList<String>(this.dependencies.size());
         for (String name : this.dependencies) {
            if (!mavContainer.containsAttribute(name)) {
               result.add(name);
            }
         }
         return result;
      }

      @Override
      public String toString() {
         return this.handlerMethod.getMethod().toGenericString();
      }
   }

}
```

说到这里，modelFactory就干了两件事

**在controller执行之前，首先将当前处理器保存的所有sessionAttributes属性合并到mavContainer,然后执行注释了@modelAttribute的方法并将结果合并到mavContainer(先处理ControllerAdvice中的)，最后检查注释了@ModeAttribut而且在@sessionAttributes中也设置了参数是否已经添加到了容器中，如果没有则从整个sessionAttributes中获取出来并设置进去，如果获取不到，就抛出异常**

**从这里可以看出model中参数的优先级是这样的**

**（1）FlashMap中保存的参数优先级最高，他在modelFactory之前执行**

**（2）SessionAttributes中保存的参数的优先级第二，他不可以覆盖FlashMap中设置的参数**

**（3）通过注释了@ModeAttribute的方法设置的参数优先级第三**

**（4）注释了@ModeAttribute而且从别的处理器的SessionAttributes中获取的参数优先级最低**

**而且从创建ModeFactory的过程中可以看出，注释了@ModeAttribute的方法是全局的优先，处理器自己定义的次之**



**而updateModel一共做了两件事情，，第一件是维护SessionAttributes中的数据，第二件事给model中需要的参数设置bindingResult，以备视图需要**



## 2.5 HandlerMethodArgumentResolver（参数解析器）

它主要用来参数解析的，主要是用在InvocableHandlerMethod中，先来看看它的继承体系

![](mvcimg\6.png)

HandlerMethodArgumentResolver一般有两种实现接口的命名，一种叫xxxMethodArgumentResolver，另一种叫xxxMethodProcessor，xxxMethodProcessor一般是既能处理参数解析又能处理返回值的。可以看到，实现太多了，所以这里就不记录太多了，下面列举下这些参数解析器是干嘛的：

- MapMethodProcessor  --------------  用来解析Map的参数值和返回值类型
- PathVariableMapMethodArgumentResolver  -------- 处理@PathVariable注解了map类型的参数

- ErrorsMethodArgumentResolver ------ 解析Errors（spring包下的）或者BindingResult（是Errors的族类）类型的参数

- AbstractNamedValueMethodArgumentResolver ---- 解析namedValue类型的参数（有name的参数，如cookie,requestParam,requestHeader,PathVariable)，主要功能有

  ​      (1)  获取name

  ​      (2)  resolveDefaultValue 解析默认的值、handleMissingValue、handleNullValue；

  ​      (3)  调用模板方法resolveName、handleResolvedValue

- RequestHeaderMethodArgumentResolver  ------ 解析了是@RequestHeader注解，并且不是map的参数类型

- RequestParamMethodArgumentResolver  ------ 可以解析注视了@RequestParam的参数、MultipartFile类型的参数和没有注释的通用类型（如int,long类型）

```java
CharSequence.class.isAssignableFrom(clazz) ||
Number.class.isAssignableFrom(clazz) ||
Date.class.isAssignableFrom(clazz) ||
clazz.equals(URI.class) || clazz.equals(URL.class) ||
clazz.equals(Locale.class) || clazz.equals(Class.class);
//是数组并且数组元素是基本类型
 (clazz.isArray() && isSimpleValueType(clazz.getComponentType())
```

如果是@requestParam注释的map类型，必须要有value值

```java
if (parameter.hasParameterAnnotation(RequestParam.class)) {
   if (Map.class.isAssignableFrom(paramType)) {
      String paramName = parameter.getParameterAnnotation(RequestParam.class).value();
      return StringUtils.hasText(paramName);
   }
   else {
      return true;
   }
}
```

- AbstractCookieValueMethodArgumentResolver   ------ 注释了@CookieValue注解的参数的基类

     ----ServletCookieValueMethodArgumentResolver  -----  具体用来解析CookieValue

- MatrixVariableMethodArgumentResolver  ---  解析如果是map类型，并且注释了@MatrixVariable注解的，并且未指定名称

- ExpressionValueMethodArgumentResolver  ----  指定了@value表达式的参数，主要设置了beanfactory，解析动作在父类中完成

- PathVariableMethodArgumentResolver   ----  是map类型，并且@PathVariable  value值不是空的情况

- RequestHeaderMapMethodArgumentResolver   -----  @RequestHeader注解，并且是map类型的

- ServletResponseMethodArgumentResolver ----

  ```java
  ServletResponse.class.isAssignableFrom(paramType) ||
        OutputStream.class.isAssignableFrom(paramType) ||
        Writer.class.isAssignableFrom(paramType)
  ```

- ModelMethodProcessor  -----   解析model类型的参数，直接返回ModelAndViewContainer中的model

- ModelAttributeMethodProcessor  -----  解析注释了@ModelAttribute注解的参数，如果annotationNotRequired=true的话，也可以注释没有注释的非通用类型的参数(RequestParamMethodArgumentResolver没有注释的通用类型的参数)

- ServletModelAttributeMethodProcessor  -----   对父类（ModelAttributeMethodProcessor  ）添加了servlet的特性，使用ServletRequestDataBinder代替父类的webDataBinder进行参数绑定

- SessionStatusMethodArgumentResolver  ---   解析SessionStatus的参数，直接返回ModelAndViewContainer里面的SessionStatus

- RequestParamMapMethodArgumentResolver  -----  可以解析注视了@RequestParam的参数并且是map类型的，并且注释中有value的参数

- AbstractMessageConverterMethodArgumentResolver ---- 使用httpMessageConverter解析==request Body==类型参数的基类

   ----RequestPartMethodArgumentResolver   ----  注释了@requestPart注解、MultipartFile、javax.servlet.http.Part类型的参数

   ---- AbstractMessageConverterMethodProcessor  ---- 定义相关工具，不直接解析参数

  ​      ----- RequestResponseBodyMethodProcessor --- 解析@RequestBody类型的参数

  ​      ----- HttpEntityMethodProcessor ---解析HttpEntity和RequestEntity类型的参数

- AbstractWebArgumentResolverAdapter --- 用作WebArgumentResolver解析器的参数解析器，,用于向后兼容.适配WebArgumentResolver

  ​    ------ ServletWebArgumentResolverAdapter --- 给父类提供了request

- UriComponentsBuilderMethodArgumentResolver  ---   处理UriComponentsBuilder类型参数

- ServletRequestMethodArgumentResolver --- 处理request相关的参数:WebRequest,ServletRequest,MultipartRequest,HttpSession,Principal,Locale,InputStream,Reader

  ```java
  WebRequest.class.isAssignableFrom(paramType) ||
        ServletRequest.class.isAssignableFrom(paramType) ||
        MultipartRequest.class.isAssignableFrom(paramType) ||
        HttpSession.class.isAssignableFrom(paramType) ||
        Principal.class.isAssignableFrom(paramType) ||
        Locale.class.equals(paramType) ||
        TimeZone.class.equals(paramType) ||
        "java.time.ZoneId".equals(paramType.getName()) ||
        InputStream.class.isAssignableFrom(paramType) ||
        Reader.class.isAssignableFrom(paramType) ||
        HttpMethod.class.equals(paramType);
  ```

- HandlerMethodArgumentResolverComposite --- ArgumentResolver的容器，可以封装多个ArgumentResolver进入list

- RedirectAttributesMethodArgumentResolver  --- 处理RedirectAttributes类型参数

- MatrixVariableMapMethodArgumentResolver --注释了@MatrixVariable注解的，并且是map的参数解析

   

### 2.4.1 接口定义

```java
/**
 * 用于在给定请求的上下文中将方法参数解析为参数值的策略接口。
 *
 * @since 3.1
 */
public interface HandlerMethodArgumentResolver {

   /**
    * 是否支持给定的参数
    */
   boolean supportsParameter(MethodParameter parameter);

   /**
    * 将方法参数解析为来自给定请求的参数值。
    */
   Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
         NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception;

}
```

### 2.4.2 MapMethodProcessor（参数解析部分内容）

```java
@Override
public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {

   return mavContainer.getModel();
}
```

直接从ModelAndViewContainer中拿到Model

### 2.4.3 ModelMethodProcessor  （参数解析部分内容）

```java
@Override
public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {

   return mavContainer.getModel();
}
```

直接从ModelAndViewContainer中拿到Model

### 2.4.4 ErrorsMethodArgumentResolver （bindingResult相关）

```java
@Override
public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
   //从mavContainer中拿出来model
   ModelMap model = mavContainer.getModel();
   if (model.size() > 0) {
      //此处说明肯定是已经解析到有error（bindingResult的父类）的相关参数的，bindingResult相关的组件是到参数解析参数的最后一步加入到bindingResult中的，那么肯定从末尾取，如果存在就拿到返回，但是必须参数绑定错误，跟在被绑定参数的后面，否则，直接抛错
      int lastIndex = model.size()-1;
      String lastKey = new ArrayList<String>(model.keySet()).get(lastIndex);
      if (lastKey.startsWith(BindingResult.MODEL_KEY_PREFIX)) {
         return model.get(lastKey);
      }
   }
 
   throw new IllegalStateException(
         "An Errors/BindingResult argument is expected to be declared immediately after the model attribute, " +
         "the @RequestBody or the @RequestPart arguments to which they apply: " + parameter.getMethod());
}
```

![](mvcimg\7.png)

因为抛错不抛错是根据上面条件决定的，如果不抛异常，那么就将这些加入到mavContainer中

![](mvcimg\8.png)

关键就在这一句，如果参数后面的一个参数是error的话，那么就不抛出异常

## 2.6 HandlerExceptionResolver

处理异常解析器，主要用于解析请求过程中产生的异常，需要注意的是

==异常处理本身所抛出的异常和视图解析过程中抛出的异常它是不能处理的==

```java
public interface HandlerExceptionResolver {

   ModelAndView resolveException(
         HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex);

}
```

继承体系

![](mvcimg\9.png)

可以看到AbstractHandlerExceptionResolver基本一家独大

### 2.6.1  AbstractHandlerExceptionResolver

AbstractHandlerExceptionResolver是其他解析类的父类，他里面定义了一些列解析的流程，spring的套路大家都清楚，那就是模板方法让子类去实现具体的解析方式

看下其最重要的方法

```java
@Override
public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response,
      Object handler, Exception ex) {
   //通过shouldApplyTo方法判断当前异常解析器能不能处理请求过程中拋出来的异常
   if (shouldApplyTo(request, handler)) {
      // Log exception, both at debug log level and at warn level, if desired.
      if (logger.isDebugEnabled()) {
         logger.debug("Resolving exception from handler [" + handler + "]: " + ex);
      }
      //如果可以，则打印日志
      logException(ex, request);
      //设置response，强行刷新缓存
      prepareResponse(ex, response);
      //开始实际的解析异常，留给子类去复写
      return doResolveException(request, response, handler, ex);
   }
   //如果无法解析，直接返回空，就给下一个异常解析器
   else {
      return null;
   }
}
```

再来看看shouldApplyTo是怎么判断当前请求产生的异常能不能处理

在该类中，存在两个成员变量

```java
//表示可以处理那些handler集合
private Set<?> mappedHandlers;
//表示可以处理那些handler的类型的集合
private Class<?>[] mappedHandlerClasses;
```

这两个属性在配置HandlerExceptionResolver的时候可以配置，用于指定可以解析处理器抛出来的哪些异常，

```java
protected boolean shouldApplyTo(HttpServletRequest request, Object handler) {
   if (handler != null) {
       //如果处理器集合不为空，返回true
      if (this.mappedHandlers != null && this.mappedHandlers.contains(handler)) {
         return true;
      }
       //如果处理器集合类型不为空
      if (this.mappedHandlerClasses != null) {
         for (Class<?> handlerClass : this.mappedHandlerClasses) {
            if (handlerClass.isInstance(handler)) {
               return true;
            }
         }
      }
   }
   //如果两个都没配置的话就处理所有的异常
   return (this.mappedHandlers == null && this.mappedHandlerClasses == null);
}
```

### 2.6.2 AbstractHandlerMethodExceptionResolver（用于处理HandlerMethod的类）

AbstractHandlerMethodExceptionResolver继承自AbstractHandlerExceptionResolver，也复写了父类的shouldApplyTo方法

```java
@Override
protected boolean shouldApplyTo(HttpServletRequest request, Object handler) {
    //如果handler等于空，直接调用父类的方法
   if (handler == null) {
      return super.shouldApplyTo(request, handler);
   }
   else if (handler instanceof HandlerMethod) {
      //转换成controller bean
      HandlerMethod handlerMethod = (HandlerMethod) handler;
      handler = handlerMethod.getBean();
      //然后调用父类的方法
      return super.shouldApplyTo(request, handler);
   }
   else {
      return false;
   }
}
```

复写了

```java
@Override
protected final ModelAndView doResolveException(
      HttpServletRequest request, HttpServletResponse response,
      Object handler, Exception ex) {

   return doResolveHandlerMethodException(request, response, (HandlerMethod) handler, ex);
}
```

用模板方法doResolveHandlerMethodException交给子类实现

### 2.6.3 ExceptionHandlerExceptionResolver

可以看出，这个类其实就是一个简化版的RequestMappingHandlerAdapter，他的执行也是使用了ServletInvocableHandlerMethod来调用方法的，来看下处理过程：

ExceptionHandlerExceptionResolver实现了==ApplicationContextAware, InitializingBean==，自然可以获得ApplicationContext对象，并且在初始化的时候完成了如下的工作

```java
@Override
public void afterPropertiesSet() {
   //收集到容器中所有bean中带有ControllerAdvice注解的类，找到里面带有@ExceptionHandler得方法，加入到exceptionHandlerAdviceCach中，
    //将含有ResponseBodyAdvice接口的，加入到responseBodyAdvice中
			new LinkedHashMap<ControllerAdviceBean, ExceptionHandlerMethodResolver>();
   initExceptionHandlerAdviceCache();
   //如果参数解析器等于空，赋予默认的参数解析器
   if (this.argumentResolvers == null) {
      List<HandlerMethodArgumentResolver> resolvers = getDefaultArgumentResolvers();
      this.argumentResolvers = new HandlerMethodArgumentResolverComposite().addResolvers(resolvers);
   }
   //如果返回值处理器等于空，则赋予默认的返回值参数处理器
   if (this.returnValueHandlers == null) {
      List<HandlerMethodReturnValueHandler> handlers = getDefaultReturnValueHandlers();
      this.returnValueHandlers = new HandlerMethodReturnValueHandlerComposite().addHandlers(handlers);
   }
}
```





```java
@Override
protected ModelAndView doResolveHandlerMethodException(HttpServletRequest request,
      HttpServletResponse response, HandlerMethod handlerMethod, Exception exception) {
   //获取处理器类
   ServletInvocableHandlerMethod exceptionHandlerMethod = getExceptionHandlerMethod(handlerMethod, exception);
   if (exceptionHandlerMethod == null) {
      return null;
   }
   //给可调用的HandlerMethod设置参数解析器和返回值解析器
   exceptionHandlerMethod.setHandlerMethodArgumentResolvers(this.argumentResolvers);
   exceptionHandlerMethod.setHandlerMethodReturnValueHandlers(this.returnValueHandlers);
   //设置request
   ServletWebRequest webRequest = new ServletWebRequest(request, response);
   //拿到mavContainer
   ModelAndViewContainer mavContainer = new ModelAndViewContainer();

   try {
      if (logger.isDebugEnabled()) {
         logger.debug("Invoking @ExceptionHandler方法 method: " + exceptionHandlerMethod);
      }
      //调用@ExceptionHandler方法
      exceptionHandlerMethod.invokeAndHandle(webRequest, mavContainer, exception);
   }
   catch (Exception invocationEx) {
      if (logger.isErrorEnabled()) {
         logger.error("Failed to invoke @ExceptionHandler method: " + exceptionHandlerMethod, invocationEx);
      }
      return null;
   }
   //下面的就是处理返回结果了
   if (mavContainer.isRequestHandled()) {
      return new ModelAndView();
   }
   else {
      ModelAndView mav = new ModelAndView().addAllObjects(mavContainer.getModel());
      mav.setViewName(mavContainer.getViewName());
      if (!mavContainer.isViewReference()) {
         mav.setView((View) mavContainer.getView());
      }
      return mav;
   }
}
```

看下是如果拿到可处理类的

```java
protected ServletInvocableHandlerMethod getExceptionHandlerMethod(HandlerMethod handlerMethod, Exception exception) {
    //拿到调用类的类型，也就是controller
   Class<?> handlerType = (handlerMethod != null ? handlerMethod.getBeanType() : null);
   
   if (handlerMethod != null) {
      //先从缓存区拿ExceptionHandlerMethodResolver
      ExceptionHandlerMethodResolver resolver = this.exceptionHandlerCache.get(handlerType);
      if (resolver == null) {
         //如果没有就new一个，并且放入到缓存
         resolver = new ExceptionHandlerMethodResolver(handlerType);
         this.exceptionHandlerCache.put(handlerType, resolver);
      }
      //然后就得到一个处理异常的mothed
      Method method = resolver.resolveMethod(exception);
      if (method != null) {
         return new ServletInvocableHandlerMethod(handlerMethod.getBean(), method);
      }
   }
   //如果还没到得到处理异常的方法，那就从ControllerAdvice中拿
   for (Entry<ControllerAdviceBean, ExceptionHandlerMethodResolver> entry : this.exceptionHandlerAdviceCache.entrySet()) {
      if (entry.getKey().isApplicableToBeanType(handlerType)) {
         ExceptionHandlerMethodResolver resolver = entry.getValue();
         Method method = resolver.resolveMethod(exception);
         if (method != null) {
            return new ServletInvocableHandlerMethod(entry.getKey().resolveBean(), method);
         }
      }
   }

   return null;
}
```

### 2.6.4 DefaultHandlerExceptionResolver

这个类的实现就很简单了

```java
@Override
protected ModelAndView doResolveException(HttpServletRequest request, HttpServletResponse response,
      Object handler, Exception ex) {

   try {
      if (ex instanceof NoSuchRequestHandlingMethodException) {
         return handleNoSuchRequestHandlingMethod((NoSuchRequestHandlingMethodException) ex, request, response,
               handler);
      }
      else if (ex instanceof HttpRequestMethodNotSupportedException) {
         return handleHttpRequestMethodNotSupported((HttpRequestMethodNotSupportedException) ex, request,
               response, handler);
      }
      else if (ex instanceof HttpMediaTypeNotSupportedException) {
         return handleHttpMediaTypeNotSupported((HttpMediaTypeNotSupportedException) ex, request, response,
               handler);
      }
      else if (ex instanceof HttpMediaTypeNotAcceptableException) {
         return handleHttpMediaTypeNotAcceptable((HttpMediaTypeNotAcceptableException) ex, request, response,
               handler);
      }
      else if (ex instanceof MissingServletRequestParameterException) {
         return handleMissingServletRequestParameter((MissingServletRequestParameterException) ex, request,
               response, handler);
      }
      else if (ex instanceof ServletRequestBindingException) {
         return handleServletRequestBindingException((ServletRequestBindingException) ex, request, response,
               handler);
      }
      else if (ex instanceof ConversionNotSupportedException) {
         return handleConversionNotSupported((ConversionNotSupportedException) ex, request, response, handler);
      }
      else if (ex instanceof TypeMismatchException) {
         return handleTypeMismatch((TypeMismatchException) ex, request, response, handler);
      }
      else if (ex instanceof HttpMessageNotReadableException) {
         return handleHttpMessageNotReadable((HttpMessageNotReadableException) ex, request, response, handler);
      }
      else if (ex instanceof HttpMessageNotWritableException) {
         return handleHttpMessageNotWritable((HttpMessageNotWritableException) ex, request, response, handler);
      }
      else if (ex instanceof MethodArgumentNotValidException) {
         return handleMethodArgumentNotValidException((MethodArgumentNotValidException) ex, request, response, handler);
      }
      else if (ex instanceof MissingServletRequestPartException) {
         return handleMissingServletRequestPartException((MissingServletRequestPartException) ex, request, response, handler);
      }
      else if (ex instanceof BindException) {
         return handleBindException((BindException) ex, request, response, handler);
      }
      else if (ex instanceof NoHandlerFoundException) {
         return handleNoHandlerFoundException((NoHandlerFoundException) ex, request, response, handler);
      }
   }
   catch (Exception handlerException) {
      logger.warn("Handling of [" + ex.getClass().getName() + "] resulted in Exception", handlerException);
   }
   return null;
}
```

拿其中一个看看

```java
protected ModelAndView handleNoSuchRequestHandlingMethod(NoSuchRequestHandlingMethodException ex,
      HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {

   pageNotFoundLogger.warn(ex.getMessage());
   response.sendError(HttpServletResponse.SC_NOT_FOUND);
   return new ModelAndView();
}
```

就是往response发送了一个错误信息，其他没有啥了

### 2.6.5 SimpleMappingExceptionResolver

SimpleMappingExceptionResolver需要==提前配置异常类和错误类型的对应关系才能够生效==，它的核心方法如下

```java
@Override
protected ModelAndView doResolveException(HttpServletRequest request, HttpServletResponse response,
      Object handler, Exception ex) {

   // 根据异常先确定错误的视图
   String viewName = determineViewName(ex, request);
   if (viewName != null) {
      // 检查是否配置了异常视图所对应的httpStatusCode
      Integer statusCode = determineStatusCode(request, viewName);
      if (statusCode != null) {
          //如果code不是空，则设置到response中去
         applyStatusCodeIfPossible(request, response, statusCode);
      }
      //得到相应的的视图
      return getModelAndView(viewName, ex, request);
   }
   else {
      //否则啥都不返回
      return null;
   }
}
```

看下怎么用的

```xml
<!-- 配置使用SimpleMappingExceptionResolver来映射异常 -->
<bean class="org.springframework.web.servlet.handler.SimpleMappingExceptionResolver">
   <!-- 给异常一个别名 -->
   <property name="exceptionAttribute" value="exception"></property>
   <property name="exceptionMappings">
      <props>
         <!--要异常的全类名,表示出现ArithmeticException异常，就跳转到error.jsp视图,
这里的视图解析器就是你配置的，不一定是jsp-->
         <prop key="java.lang.ArithmeticException">error</prop>
      </props>
   </property>
   <property name="defaultErrorView" value="error"></property>
</bean>
```

### 2.6.6 ResponseStatusExceptionResolver

ResponseStatus用来解析@ResponseStatus的异常（如自定义的注释了@ResponseStatus）

看下效果

```java
@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR,reason = "服务器错误")
public class ResponseStatusException extends RuntimeException{
}
```

![](mvcimg\10.png)

重要方法实现

```java
@Override
protected ModelAndView doResolveException(HttpServletRequest request, HttpServletResponse response,
      Object handler, Exception ex) {
   //看看这个异常信息上有没有标注@ResponseStatus注解
   ResponseStatus responseStatus = AnnotationUtils.findAnnotation(ex.getClass(), ResponseStatus.class);
    
   if (responseStatus != null) {
      try {
          //如果标注了，就返回错误信息
         return resolveResponseStatus(responseStatus, request, response, handler, ex);
      }
      catch (Exception resolveEx) {
         logger.warn("Handling of @ResponseStatus resulted in Exception", resolveEx);
      }
   }
   //否则不处理
   return null;
}
```

```java
protected ModelAndView resolveResponseStatus(ResponseStatus responseStatus, HttpServletRequest request,
      HttpServletResponse response, Object handler, Exception ex) throws Exception {
   //拿到错误码
   int statusCode = responseStatus.value().value();
   //拿到错误原因
   String reason = responseStatus.reason();
   if (this.messageSource != null) {
       //国际化处理
      reason = this.messageSource.getMessage(reason, null, reason, LocaleContextHolder.getLocale());
   }
   if (!StringUtils.hasLength(reason)) {
      response.sendError(statusCode);
   }
   else {
      //设置response
      response.sendError(statusCode, reason);
   }
   return new ModelAndView();
}
```

### 2.6.7 AnnotationMethodHandlerExceptionResolver

被废弃了，被org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver替代了

## 2.7 文件上传组件MultipartResolver

MultipartResolver是用来处理文件上传的，看下实现方法

![](mvcimg\11.png)

有两个，分别是

- StandardServletMultipartResolver：基于servlet3.0标准的

- CommonsMultipartResolver：是基于apache的common-fileupload实现的

### 2.7.1 StandardServletMultipartResolver

StandardServletMultipartResolver使用了servlet3.0规范，所以是不需要引入jar包的，但是必须要支持servlet3.0的容器才可以

servlet3.0规范（<!--规范文档在文档页可看-->）表示，如果是一个文件上传调用，只需要通过

- public Collection<Part> getParts()
- public Part getPart(String name)

就可以获得文件列表的

需要注意的是

> 1. 要注意xml中web-app节点使用的版本，**必须是3.0+**。
> 2. 要注意servlet节点下的multipart-config节点的配置。区别与使用CommonsMultipartResolver进行文件上传时配置的不同。

StandardServletMultipartResolver的实现代码其实很简单

```java
public class StandardServletMultipartResolver implements MultipartResolver {

   private boolean resolveLazily = false;


   /**
    * Set whether to resolve the multipart request lazily at the time of
    * file or parameter access.
    * <p>Default is "false", resolving the multipart elements immediately, throwing
    * corresponding exceptions at the time of the {@link #resolveMultipart} call.
    * Switch this to "true" for lazy multipart parsing, throwing parse exceptions
    * once the application attempts to obtain multipart files or parameters.
    */
   public void setResolveLazily(boolean resolveLazily) {
      this.resolveLazily = resolveLazily;
   }

   //检查是不是文件上传，先看看是不是post方法，然后再检查Content-Type是不是以multipart/开头的
   @Override
   public boolean isMultipart(HttpServletRequest request) {
      // Same check as in Commons FileUpload...
      if (!"post".equals(request.getMethod().toLowerCase())) {
         return false;
      }
      String contentType = request.getContentType();
      return (contentType != null && contentType.toLowerCase().startsWith("multipart/"));
   }

   //解析功能直接交给了StandardMultipartHttpServletRequest去实现实现了
   @Override
   public MultipartHttpServletRequest resolveMultipart(HttpServletRequest request) throws MultipartException {
      return new StandardMultipartHttpServletRequest(request, this.resolveLazily);
   }
   //清除缓存
   @Override
   public void cleanupMultipart(MultipartHttpServletRequest request) {
      // To be on the safe side: explicitly delete the parts,
      // but only actual file parts (for Resin compatibility)
      try {
         for (Part part : request.getParts()) {
            if (request.getFile(part.getName()) != null) {
               part.delete();
            }
         }
      }
      catch (Exception ex) {
         LogFactory.getLog(getClass()).warn("Failed to perform cleanup of multipart items", ex);
      }
   }

}
```

看看比较重要的StandardMultipartHttpServletRequest的实现，其中最重要的一个方法就是parseRequest方法

```java
private void parseRequest(HttpServletRequest request) {
   try {
      //拿到所有的文件
      Collection<Part> parts = request.getParts();
      this.multipartParameterNames = new LinkedHashSet<String>(parts.size());
      MultiValueMap<String, MultipartFile> files = new LinkedMultiValueMap<String, MultipartFile>(parts.size());
      //将文件全部解析到MultiValueMap<String, MultipartFile>中，并且保存在对应的属性中，以便在处理器中可以直接调用
      for (Part part : parts) {
         String filename = extractFilename(part.getHeader(CONTENT_DISPOSITION));
         if (filename != null) {
            files.add(part.getName(), new StandardMultipartFile(part, filename));
         }
         else {
            this.multipartParameterNames.add(part.getName());
         }
      }
      setMultipartFiles(files);
   }
   catch (Exception ex) {
      throw new MultipartException("Could not parse multipart servlet request", ex);
   }
}
```

### 2.7.2 CommonsMultipartResolver

CommonsMultipartResolver是依赖commons-fileupload组件的，必须要引入commons-fileupload包

```xml
<dependency>
  <groupId>commons-fileupload</groupId>
  <artifactId>commons-fileupload</artifactId>
  <version>1.3.3</version>
</dependency>
```

他的代码也比较简单

```java
public class CommonsMultipartResolver extends CommonsFileUploadSupport
      implements MultipartResolver, ServletContextAware {

   private boolean resolveLazily = false;

   //交给ServletFileUpload去判断是不是文件上传，判断逻辑也是检查Content-Type是不是以multipart/开头的
   @Override
   public boolean isMultipart(HttpServletRequest request) {
      return (request != null && ServletFileUpload.isMultipartContent(request));
   }

  
   @Override
   public MultipartHttpServletRequest resolveMultipart(final HttpServletRequest request) throws MultipartException {
      Assert.notNull(request, "Request must not be null");
      //判断是不是懒加载的
      if (this.resolveLazily) {
         return new DefaultMultipartHttpServletRequest(request) {
            @Override
            protected void initializeMultipart() {
               MultipartParsingResult parsingResult = parseRequest(request);
               setMultipartFiles(parsingResult.getMultipartFiles());
               setMultipartParameters(parsingResult.getMultipartParameters());
               setMultipartParameterContentTypes(parsingResult.getMultipartParameterContentTypes());
            }
         };
      }
      else {
         //解析文件
         MultipartParsingResult parsingResult = parseRequest(request);
         //最后生成一个DefaultMultipartHttpServletRequest来处理文件
         return new DefaultMultipartHttpServletRequest(request, parsingResult.getMultipartFiles(),
               parsingResult.getMultipartParameters(), parsingResult.getMultipartParameterContentTypes());
      }
   }

   
   protected MultipartParsingResult parseRequest(HttpServletRequest request) throws MultipartException {
      //获取配置文件中的编码
      String encoding = determineEncoding(request);
      //获取文件
      FileUpload fileUpload = prepareFileUpload(encoding);
      try {
         List<FileItem> fileItems = ((ServletFileUpload) fileUpload).parseRequest(request);
         //解析文件
         return parseFileItems(fileItems, encoding);
      }
      catch (FileUploadBase.SizeLimitExceededException ex) {
         throw new MaxUploadSizeExceededException(fileUpload.getSizeMax(), ex);
      }
      catch (FileUploadException ex) {
         throw new MultipartException("Could not parse multipart servlet request", ex);
      }
   }

  
   protected String determineEncoding(HttpServletRequest request) {
      String encoding = request.getCharacterEncoding();
      if (encoding == null) {
         encoding = getDefaultEncoding();
      }
      return encoding;
   }

   @Override
   public void cleanupMultipart(MultipartHttpServletRequest request) {
      if (request != null) {
         try {
            cleanupFileItems(request.getMultiFileMap());
         }
         catch (Throwable ex) {
            logger.warn("Failed to perform multipart cleanup for servlet request", ex);
         }
      }
   }

}
```





```java
protected MultipartParsingResult parseFileItems(List<FileItem> fileItems, String encoding) {
   MultiValueMap<String, MultipartFile> multipartFiles = new LinkedMultiValueMap<String, MultipartFile>();
   Map<String, String[]> multipartParameters = new HashMap<String, String[]>();
   Map<String, String> multipartParameterContentTypes = new HashMap<String, String>();

   // 将FileItem分为文件和参数两类，并设置到对应的map中
   for (FileItem fileItem : fileItems) {
      //如果是参数
      if (fileItem.isFormField()) {
         String value;
         //取到声明的编码方式
         String partEncoding = determineEncoding(fileItem.getContentType(), encoding);
         if (partEncoding != null) {
            try {
               //获取到属性值
               value = fileItem.getString(partEncoding);
            }
            catch (UnsupportedEncodingException ex) {
               if (logger.isWarnEnabled()) {
                  logger.warn("Could not decode multipart item '" + fileItem.getFieldName() +
                        "' with encoding '" + partEncoding + "': using platform default");
               }
               //如果找不到编码就按照默认编码走
               value = fileItem.getString();
            }
         }
         else {
            //如果没有设置编码就按照默认编码编码
            value = fileItem.getString();
         }
         String[] curParam = multipartParameters.get(fileItem.getFieldName());
         if (curParam == null) {
            //单个参数的处理
            multipartParameters.put(fileItem.getFieldName(), new String[] {value});
         }
         else {
            // 多参数的处理
            String[] newParam = StringUtils.addStringToArray(curParam, value);
            multipartParameters.put(fileItem.getFieldName(), newParam);
         }
         //保存参数的ContentType
         multipartParameterContentTypes.put(fileItem.getFieldName(), fileItem.getContentType());
      }
      else {
         // 如果是文件类型
         CommonsMultipartFile file = new CommonsMultipartFile(fileItem);
         //加入到multipartFiles中去
         multipartFiles.add(file.getName(), file);
         if (logger.isDebugEnabled()) {
            logger.debug("Found multipart file [" + file.getName() + "] of size " + file.getSize() +
                  " bytes with original filename [" + file.getOriginalFilename() + "], stored " +
                  file.getStorageDescription());
         }
      }
   }
   return new MultipartParsingResult(multipartFiles, multipartParameters, multipartParameterContentTypes);
}
```

## 2.8 HandlerMapping

看看HandlerMapping的继承结构图

![](mvcimg\12.png)

可以看到HandlerMapping分为了两大分支,分别是

- AbstractUrlHandlerMapping
- AbstractHandlerMethodMapping

### 2.8.1 接口定义

```java
public interface HandlerMapping {

   /**
    * HttpServletRequest属性名，该属性主要处理程序映射路径（如果是模式匹配），或者包含完成相关的URI
    * 注意：不是所有的HandlerMapping都需要他，基于URL的HandlerMappings通常会支持它，但处理程序不一定要求在所有方案中都存在此请求属性。
    */
   String PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE = HandlerMapping.class.getName() + ".pathWithinHandlerMapping";

  
   String BEST_MATCHING_PATTERN_ATTRIBUTE = HandlerMapping.class.getName() + ".bestMatchingPattern";

 
   String INTROSPECT_TYPE_LEVEL_MAPPING = HandlerMapping.class.getName() + ".introspectTypeLevelMapping";

 
   String URI_TEMPLATE_VARIABLES_ATTRIBUTE = HandlerMapping.class.getName() + ".uriTemplateVariables";

  
   String MATRIX_VARIABLES_ATTRIBUTE = HandlerMapping.class.getName() + ".matrixVariables";

   
   String PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE = HandlerMapping.class.getName() + ".producibleMediaTypes";

 
   HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception;

}
```

### 2.8.2 AbstractHandlerMapping

#### 2.8.2.1 初始化

AbstractHandlerMapping是HandlerMapping的最顶层实现，那么下面所有的实现都是基于它的，大家都知道，HandlerMapping的作用就是寻找Handler和Interceptors的

```java
public abstract class AbstractHandlerMapping extends WebApplicationObjectSupport
      implements HandlerMapping, Ordered {
}
```

可以看到AbstractHandlerMapping继承了WebApplicationObjectSupport类

说到这里，不得不说下获取spring bean的五种方式

**1. 直接注入ApplicationContext**

```java
@Autowired
private ApplicationContext applicationContext;
```

那么就有一个很严峻的问题，什么时候applicationContext注入到容器中的呢？

​    spring在容器刷新的时候，在prepareBeanFactory(beanFactory)阶段处理的

```java
beanFactory.registerResolvableDependency(BeanFactory.class, beanFactory);
beanFactory.registerResolvableDependency(ResourceLoader.class, this);
beanFactory.registerResolvableDependency(ApplicationEventPublisher.class, this);
beanFactory.registerResolvableDependency(ApplicationContext.class, this);
```

容器通过==registerResolvableDependency方法将这些bean放入到了容器的成员变量中==

```java
/** Map from dependency type to corresponding autowired value */
private final Map<Class<?>, Object> resolvableDependencies = new HashMap<Class<?>, Object>(16);
```

这样子，在被标注了@autowired注解的后，在属性注入的时候，通过AutowiredAnnotationBeanPostProcessor  

bean的后置处理器中拿到了容器中的resolvableDependencies值来设置到ApplicationContext中去的

2. **通过调用ApplicationContextAware的set方法**

通过ApplicationContextAwareProcessor bean的后置处理器来实现的

3. **通过spring的工具类获取WebApplicationContextUtils**

此方式适用于浏览器端获取bean工厂，可以获取到父容器的实例，通过servletContext

4. **ApplicationObjectSupport获取bean**

它实现了ApplicationContextAware接口，算得上ApplicationContextAware的一种扩展吧，扩展了国际化的一些东西

```java
ApplicationObjectSupport implements ApplicationContextAware 
```

5. **WebApplicationObjectSupport**

```java
public abstract class WebApplicationObjectSupport extends ApplicationObjectSupport implements ServletContextAware 
```

继承自ApplicationObjectSupport，并且比ApplicationObjectSupport更强大，可以获取ServletContext



回到主题，==AbstractHandlerMapping继承了WebApplicationObjectSupport==，并且实现了ApplicationObjectSupport的initApplicationContext方法

```java
@Override
protected void initApplicationContext() throws BeansException {
   //一个模板方法，让子类去实现，为了给子类去添加或者修改interceptors的一个机会，不过好像springmvc到现在都没有实现它
   extendInterceptors(this.interceptors);
   //拿到spring父子容器中所有的MappedInterceptor类型的bean
   detectMappedInterceptors(this.mappedInterceptors);
   //将interceptors属性中的拦截器按照类型的不同进行分类
   initInterceptors();
}
```

注意：springmvc如果加入了annotation-driven注解

```xml
<mvc:annotation-driven></mvc:annotation-driven>
```

![](mvcimg\13.png)

或默认向容器中添加一个拦截器和一个MappedInterceptor类型的bean,

这个拦截器的作用就是在**==每个请求之前往request中丢入ConversionService。主要用于spring:eval标签的使用==**。

```java
@Override
public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws ServletException, IOException {

   request.setAttribute(ConversionService.class.getName(), this.conversionService);
   return true;
}
```

在看看initInterceptors()方法

```java
protected void initInterceptors() {
   if (!this.interceptors.isEmpty()) {
      for (int i = 0; i < this.interceptors.size(); i++) {
         Object interceptor = this.interceptors.get(i);
         if (interceptor == null) {
            throw new IllegalArgumentException("Entry number " + i + " in interceptors array is null");
         }
         //如果是MappedInterceptor类型的就添加到mappedInterceptors中去
         if (interceptor instanceof MappedInterceptor) {
            this.mappedInterceptors.add((MappedInterceptor) interceptor);
         }
         //就加入到adaptedInterceptors属性中去
         else {
            this.adaptedInterceptors.add(adaptInterceptor(interceptor));
         }
      }
   }
}
```

总结下：

```java
private final List<Object> interceptors = new ArrayList<Object>();

private final List<HandlerInterceptor> adaptedInterceptors = new ArrayList<HandlerInterceptor>();

private final List<MappedInterceptor> mappedInterceptors = new ArrayList<MappedInterceptor>();
```

AbstractHandlerMapping中的Interceptor一共有三个，见上

interceptors：==用于配置（仅此只用于配置）==，配置的方法有两个，HandlerMapping属性注入；子类的extendInterceptors()复写，修改或者增加

mappedInterceptors：==在使用的时候要与url配合使用==，url匹配上了才会使用，匹配不上就用不上，获取方法也很简单，interceptors加入到拦截器中，要不直接注入到spring容器中，detectMappedInterceptors()方法会处理

adaptedInterceptors：这种拦截器不需要和url匹配，直接会使用



整个AbstractHandlerMapping的初始化阶段基本就是这样的，只是初始化了这三个interceptors

#### 2.8.2.2 实现方法getHandler(HttpServletRequest request) 

```java
@Override
public final HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
   //获取Handler，交给子类去复写
   Object handler = getHandlerInternal(request);
   //如果没有获取到，就获取默认的
   if (handler == null) {
      handler = getDefaultHandler();
   }
   //如果也没有配置默认的，那就只能返回空了
   if (handler == null) {
      return null;
   }
   // 如果获取到的是一个string,就直接在容器中拿
   if (handler instanceof String) {
      String handlerName = (String) handler;
      handler = getApplicationContext().getBean(handlerName);
   }
   //包装拦截器并且返回
   return getHandlerExecutionChain(handler, request);
}
```

```java
protected HandlerExecutionChain getHandlerExecutionChain(Object handler, HttpServletRequest request) {
   //包装HandlerExecutionChain
   HandlerExecutionChain chain = (handler instanceof HandlerExecutionChain ?
         (HandlerExecutionChain) handler : new HandlerExecutionChain(handler));
    //加入拦截器
   chain.addInterceptors(getAdaptedInterceptors());

   //将符合url的加入到HandlerExecutionChain中去
   String lookupPath = this.urlPathHelper.getLookupPathForRequest(request);
   for (MappedInterceptor mappedInterceptor : this.mappedInterceptors) {
      if (mappedInterceptor.matches(lookupPath, this.pathMatcher)) {
         chain.addInterceptor(mappedInterceptor.getInterceptor());
      }
   }

   return chain;
}
```

### 2.8.3 AbstractUrlHandlerMapping

AbstractUrlHandlerMapping实现原理其实很简单，就是将url和Handler保存在一个map中，然后使用url来匹配Handler，但是在AbstractUrlHandlerMapping中没有对此map的初始化动作，想必是交给子类去实现的，另外，对于  **‘/’**  的处理，专门定义了一个rootHandler来处理

来看了看AbstractUrlHandlerMapping对于AbstractHandlerMapping getHandlerInternal方法的复写

```java
@Override
protected Object getHandlerInternal(HttpServletRequest request) throws Exception {
   //获取到URI
   String lookupPath = getUrlPathHelper().getLookupPathForRequest(request);
   Object handler = lookupHandler(lookupPath, request);
   if (handler == null) {
      
      Object rawHandler = null;
       //如果是"/"的请求，就交给RootHandler去处理
      if ("/".equals(lookupPath)) {
         rawHandler = getRootHandler();
      }
      //如果是空的话就交给默认的处理器处理
      if (rawHandler == null) {
         rawHandler = getDefaultHandler();
      }
      //如果不是空的话
      if (rawHandler != null) {
         // string类型的就直接从容器中获取
         if (rawHandler instanceof String) {
            String handlerName = (String) rawHandler;
            rawHandler = getApplicationContext().getBean(handlerName);
         }
         //可以校验传入的request和Handler是否匹配，目前为止，子类没有覆盖他
         validateHandler(rawHandler, request);
         //添加两个拦截器处理，后续展开说明
         handler = buildPathExposingHandler(rawHandler, lookupPath, lookupPath, null);
      }
   }
   if (handler != null && logger.isDebugEnabled()) {
      logger.debug("Mapping [" + lookupPath + "] to " + handler);
   }
   //如果没有找到，打印日志
   else if (handler == null && logger.isTraceEnabled()) {
      logger.trace("No handler mapping found for [" + lookupPath + "]");
   }
   return handler;
}
```

```java
protected Object lookupHandler(String urlPath, HttpServletRequest request) throws Exception {
   //直接匹配
   Object handler = this.handlerMap.get(urlPath);
   //如果匹配到了，就直接处理了
   if (handler != null) {
      //string类型的就直接从容器中获取
      if (handler instanceof String) {
         String handlerName = (String) handler;
         handler = getApplicationContext().getBean(handlerName);
      }
      validateHandler(handler, request);
      return buildPathExposingHandler(handler, urlPath, urlPath, null);
   }
   // 模式匹配,这里使用的是AntPathMatcher
   List<String> matchingPatterns = new ArrayList<String>();
   for (String registeredPattern : this.handlerMap.keySet()) {
      if (getPathMatcher().match(registeredPattern, urlPath)) {
         matchingPatterns.add(registeredPattern);
      }
   }
   String bestPatternMatch = null;
   //排序，拿到第一个url
   Comparator<String> patternComparator = getPathMatcher().getPatternComparator(urlPath);
   if (!matchingPatterns.isEmpty()) {
      Collections.sort(matchingPatterns, patternComparator);
      if (logger.isDebugEnabled()) {
         logger.debug("Matching patterns for request [" + urlPath + "] are " + matchingPatterns);
      }
      bestPatternMatch = matchingPatterns.get(0);
   }
    
   if (bestPatternMatch != null) {
      handler = this.handlerMap.get(bestPatternMatch);
     //string类型的就直接从容器中获取  
      if (handler instanceof String) {
         String handlerName = (String) handler;
         handler = getApplicationContext().getBean(handlerName);
      }
      validateHandler(handler, request);
      String pathWithinMapping = getPathMatcher().extractPathWithinPattern(bestPatternMatch, urlPath);

      // 之前已经通过排序拿到了第一个，但是这里可能有多个合适的
      Map<String, String> uriTemplateVariables = new LinkedHashMap<String, String>();
      for (String matchingPattern : matchingPatterns) {
         if (patternComparator.compare(bestPatternMatch, matchingPattern) == 0) {
            Map<String, String> vars = getPathMatcher().extractUriTemplateVariables(matchingPattern, urlPath);
            Map<String, String> decodedVars = getUrlPathHelper().decodePathVariables(request, vars);
            uriTemplateVariables.putAll(decodedVars);
         }
      }
      if (logger.isDebugEnabled()) {
         logger.debug("URI Template variables for request [" + urlPath + "] are " + uriTemplateVariables);
      }
      return buildPathExposingHandler(handler, bestPatternMatch, pathWithinMapping, uriTemplateVariables);
   }
   // No handler found...
   return null;
}
```

buildPathExposingHandler给找到的handler注册了两个拦截器

```java
protected Object buildPathExposingHandler(Object rawHandler, String bestMatchingPattern,
      String pathWithinMapping, Map<String, String> uriTemplateVariables) {

   HandlerExecutionChain chain = new HandlerExecutionChain(rawHandler);
   chain.addInterceptor(new PathExposingHandlerInterceptor(bestMatchingPattern, pathWithinMapping));
   if (!CollectionUtils.isEmpty(uriTemplateVariables)) {
      chain.addInterceptor(new UriTemplateVariablesHandlerInterceptor(uriTemplateVariables));
   }
   return chain;
}
```

**PathExposingHandlerInterceptor和UriTemplateVariablesHandlerInterceptor**

将当前请求实际匹配的pattern，匹配条件和url模板参数设置到request中去，这样后面的处理中就可以直接从request中直接拿了



可以看到，最关键的数据都是从map中获取的，那么看看==**map的初始化**==

```java
protected void registerHandler(String urlPath, Object handler) throws BeansException, IllegalStateException {
   Assert.notNull(urlPath, "URL path must not be null");
   Assert.notNull(handler, "Handler object must not be null");
   Object resolvedHandler = handler;

   // Eagerly resolve handler if referencing singleton via name.
   if (!this.lazyInitHandlers && handler instanceof String) {
      String handlerName = (String) handler;
      if (getApplicationContext().isSingleton(handlerName)) {
         resolvedHandler = getApplicationContext().getBean(handlerName);
      }
   }

   Object mappedHandler = this.handlerMap.get(urlPath);
   if (mappedHandler != null) {
      if (mappedHandler != resolvedHandler) {
         throw new IllegalStateException(
               "Cannot map " + getHandlerDescription(handler) + " to URL path [" + urlPath +
               "]: There is already " + getHandlerDescription(mappedHandler) + " mapped.");
      }
   }
   else {
      if (urlPath.equals("/")) {
         if (logger.isInfoEnabled()) {
            logger.info("Root mapping to " + getHandlerDescription(handler));
         }
         setRootHandler(resolvedHandler);
      }
      else if (urlPath.equals("/*")) {
         if (logger.isInfoEnabled()) {
            logger.info("Default mapping to " + getHandlerDescription(handler));
         }
         setDefaultHandler(resolvedHandler);
      }
      else {
         this.handlerMap.put(urlPath, resolvedHandler);
         if (logger.isInfoEnabled()) {
            logger.info("Mapped URL path [" + urlPath + "] onto " + getHandlerDescription(handler));
         }
      }
   }
}
```



