# spring bean的后置处理器

bean的后置处理器特别重要，贯穿着spring整个框架，为定制spring，扩展spring提供了非常方便的手段。

先来看看接口定义

```java
public interface BeanPostProcessor {

   @Nullable
   default Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
      return bean;
   }

   @Nullable
   default Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
      return bean;
   }

}
```

接口定义很简单，有两个方法，分别是前置处理和后置处理

```
问题一、什么是前置处理和后置处理，什么时候执行的？
```

![](beanpostprocessorimg\1.png)

在bean创建初始化的时候，在invokInitMethods方法前后，分别执行了前置和后置方法， **invokInitMethods**这个方法具体就是执行了==实现了**InitializingBean**的afterPropertiesSet方法==和用户在==bean中自定义的init-method方法==，如果想看远吗，请移步spring源码查看

### 一、BeanPostProcessor的变种

说到变种，你可能想要变种人，开个玩笑，此处的变种是为了迎合强大的spring框架，做到可灵活扩展而引入的变种，正是因为这些BeanPostProcessor的变种，使得spring高度可扩展，先来就来分析下spring框架的后置处理器有哪些变种，都应用在什么地方，我们也可以根据这些个原理，来完成自己想要扩展或者实现的功能，在介绍之前，我们先简单看下BeanPostProcessor的集成架构

![](beanpostprocessorimg\2.png)

图片可能看的不是很清楚，那么来个文字的

DataSourceInitializedPublisher (org.springframework.boot.autoconfigure.orm.jpa)
ServletContextAwareProcessor (org.springframework.web.context.support)
    WebApplicationContextServletContextAwareProcessor (org.springframework.boot.web.servlet.context)
AdvisorAdapterRegistrationManager (org.springframework.aop.framework.adapter)
SyncBeanPostProcessor (com.best.ecosphere.base)
BusExtensionPostProcessor (com.best.xingng.org.apache.cxf.bus.spring)
AbstractAdvisingBeanPostProcessor (org.springframework.aop.framework)
    AbstractBeanFactoryAwareAdvisingPostProcessor (org.springframework.aop.framework.autoproxy)
        MethodValidationPostProcessor (org.springframework.validation.beanvalidation)
        AsyncAnnotationBeanPostProcessor (org.springframework.scheduling.annotation)
        PersistenceExceptionTranslationPostProcessor (org.springframework.dao.annotation)
    StatAnnotationBeanPostProcessor (com.alibaba.druid.support.spring.stat.annotation)
HalObjectMapperConfigurer in HypermediaAutoConfiguration (org.springframework.boot.autoconfigure.hateoas)
DestructionAwareBeanPostProcessor (org.springframework.beans.factory.config)
    ScheduledAnnotationBeanPostProcessor (org.springframework.scheduling.annotation)
    SimpleServletPostProcessor (org.springframework.web.servlet.handler)
    InitDestroyAnnotationBeanPostProcessor (org.springframework.beans.factory.annotation)
        CommonAnnotationBeanPostProcessor (org.springframework.context.annotation)
    ApplicationListenerDetector (org.springframework.context.support)
BootstrapContextAwareProcessor (org.springframework.jca.context)
BeanValidationPostProcessor (org.springframework.validation.beanvalidation)
InstantiationAwareBeanPostProcessor (org.springframework.beans.factory.config)
    SmartInstantiationAwareBeanPostProcessor (org.springframework.beans.factory.config)
        InstantiationAwareBeanPostProcessorAdapter (org.springframework.beans.factory.config)
            ScriptFactoryPostProcessor (org.springframework.scripting.support)
            RequiredAnnotationBeanPostProcessor (org.springframework.beans.factory.annotation)
            ImportAwareBeanPostProcessor in ConfigurationClassPostProcessor (org.springframework.context.annotation)
            AutowiredAnnotationBeanPostProcessor (org.springframework.beans.factory.annotation)
            JsonMarshalTestersBeanPostProcessor in JsonTestersAutoConfiguration (org.springframework.boot.test.autoconfigure.json)
            MockitoPostProcessor (org.springframework.boot.test.mock.mockito)
            SpyPostProcessor in MockitoPostProcessor (org.springframework.boot.test.mock.mockito)
        AbstractAutoProxyCreator (org.springframework.aop.framework.autoproxy)
            BeanTypeAutoProxyCreator (com.alibaba.druid.support.spring.stat)
            SpringIbatisBeanTypeAutoProxyCreator (com.alibaba.druid.support.ibatis)
            BeanNameAutoProxyCreator (org.springframework.aop.framework.autoproxy)
                SpringIbatisBeanNameAutoProxyCreator (com.alibaba.druid.support.ibatis)
            AbstractAdvisorAutoProxyCreator (org.springframework.aop.framework.autoproxy)
                DefaultAdvisorAutoProxyCreator (org.springframework.aop.framework.autoproxy)
                AspectJAwareAdvisorAutoProxyCreator (org.springframework.aop.aspectj.autoproxy)
                    AnnotationAwareAspectJAutoProxyCreator (org.springframework.aop.aspectj.annotation)
                InfrastructureAdvisorAutoProxyCreator (org.springframework.aop.framework.autoproxy)
    CommonAnnotationBeanPostProcessor (org.springframework.context.annotation)
BeanPostProcessorChecker in PostProcessorRegistrationDelegate (org.springframework.context.support)
ErrorPageRegistrarBeanPostProcessor (org.springframework.boot.web.server)
LoadTimeWeaverAwareProcessor (org.springframework.context.weaving)
DataSourceInitializerPostProcessor (org.springframework.boot.autoconfigure.jdbc)
WebServerFactoryCustomizerBeanPostProcessor (org.springframework.boot.web.server)
RabbitListenerAnnotationBeanPostProcessor (org.springframework.amqp.rabbit.annotation)
PropertyMappingCheckBeanPostProcessor in PropertyMappingContextCustomizer (org.springframework.boot.test.autoconfigure.properties)
ApplicationContextAwareProcessor (org.springframework.context.support)
ConfigurationPropertiesBindingPostProcessor (org.springframework.boot.context.properties)
JaxWsWebServicePublisherBeanPostProcessor (com.best.xingng.org.apache.cxf.jaxws.spring)
MergedBeanDefinitionPostProcessor (org.springframework.beans.factory.support)
    JmsListenerAnnotationBeanPostProcessor (org.springframework.jms.annotation)
    ScheduledAnnotationBeanPostProcessor (org.springframework.scheduling.annotation)
    InitDestroyAnnotationBeanPostProcessor (org.springframework.beans.factory.annotation)
        CommonAnnotationBeanPostProcessor (org.springframework.context.annotation)
    RequiredAnnotationBeanPostProcessor (org.springframework.beans.factory.annotation)
    AutowiredAnnotationBeanPostProcessor (org.springframework.beans.factory.annotation)
    ApplicationListenerDetector (org.springframework.context.support)
ProjectingArgumentResolverBeanPostProcessor in ProjectingArgumentResolverRegistrar (org.springframework.data.web.config)

可以看到，真是的很是丰富，鉴于这么多，笔者这里就不一一的去看了，可以简单来看看比较常用的几个，具体是做什么用的，具体的源码细节你可以参照spring的源码来分析到底是怎么实现的，相信聪明的你一定会看懂

ServletContextAwareProcessor   ----------------------------   注入servlet容器相关的信息的

AsyncAnnotationBeanPostProcessor  ---------------------  @async直接的实现

AbstractAdvisingBeanPostProcessor   --------------------- 也算是aop实现的一种后置处理器

AbstractAutoProxyCreator            ----------------------------- 与上面AbstractAdvisingBeanPostProcessor  对应的，                        也是spring aop实现的一种主方式

ScheduledAnnotationBeanPostProcessor   ---------------   定时器注解的实现

SimpleServletPostProcessor          ---------------------------  如果bean是servlet就调用 init 方法

CommonAnnotationBeanPostProcessor     ------------   spring对于jsr250的实现，主要有以下注解

```
@WebServiceRef  我也没用过，哈哈
@Resource 注入
@EJB  我也没用过，哈哈
@PostConstruct  初始化
@PreDestroy   销毁
```

BeanValidationPostProcessor    --------------------   校验bean参数的，beanPostProcessor 阶段已完成属性注入

AutowiredAnnotationBeanPostProcessor   ---------------  spring属性注入的实现 ，主要实现了以下注解

```
@Autowired
@value
@inject
```

RequiredAnnotationBeanPostProcessor   ------------------------ @Required注解的实现，用于检查一个Bean的属性的值**在配置期间是否被赋予或设置**



#### 1.1   变种一   ==MergedBeanDefinitionPostProcessor==

看到这个名字，合并BeanDefinition的后置处理器，在之前后置处理器的基础上，增加了MergedBeanDefinitionPostProcessor，那么疑问来了？

```
问题二：MergedBeanDefinitionPostProcessor干啥的，有啥作用？
```

带着这个问题，笔者带大家看下接口定义以及它的一些实现

```
public interface MergedBeanDefinitionPostProcessor extends BeanPostProcessor {

   /**
    * 
    */
   void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName);

}
```

spring 5.1，在原来的接口基础上，又扩展了新的方法

```java
//重置指定的bean定义
default void resetBeanDefinition(String beanName) {}
```

![](beanpostprocessorimg\3.png)

看了下，基本上是和注解相关的，你有没有发现？但是也有不是和注解相关的，但是这里我想用两个实现来对比着看下，到底这个接口都干了些什么东西，有没有值得借鉴的东西

##### 1.1.1  AutowiredAnnotationBeanPostProcessor  的 MergedBeanDefinitionPostProcessor实现

```java
@Override
public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {  
    //找到注入的源信息
    InjectionMetadata metadata = findAutowiringMetadata(beanName, beanType, null); 
    //检查成员去重
    metadata.checkConfigMembers(beanDefinition);
}
```

看下是怎么找的

```java
private InjectionMetadata findAutowiringMetadata(String beanName, Class<?> clazz, @Nullable PropertyValues pvs) {
		String cacheKey = (StringUtils.hasLength(beanName) ? beanName : clazz.getName());
		// Quick check on the concurrent map first, with minimal locking.
        //先从缓存中找
		InjectionMetadata metadata = this.injectionMetadataCache.get(cacheKey);
        //如果需要刷新缓存，也就是metadata是空的
		if (InjectionMetadata.needsRefresh(metadata, clazz)) {
            //这里要加锁，双重检查
			synchronized (this.injectionMetadataCache) {
				metadata = this.injectionMetadataCache.get(cacheKey);
				if (InjectionMetadata.needsRefresh(metadata, clazz)) {
					//如果metadata不是空的，但是他的属性targetClass不是空，那就清理掉pvs
                    if (metadata != null) {
						metadata.clear(pvs);
					}
                    //查找源信息（这里是最重要的）
					metadata = buildAutowiringMetadata(clazz);
                    //查完后再放回缓存就是了
					this.injectionMetadataCache.put(cacheKey, metadata);
				}
			}
		}
		return metadata;
	}
```

```java
private InjectionMetadata buildAutowiringMetadata(final Class<?> clazz) {
		List<InjectionMetadata.InjectedElement> elements = new ArrayList<>();
		Class<?> targetClass = clazz;
        //循环
		do {
			final List<InjectionMetadata.InjectedElement> currElements = new ArrayList<>();
            //查找所有的字段带有@Autowired、@Inject、@value注解的，加入到currElements中
			ReflectionUtils.doWithLocalFields(targetClass, field -> {
				AnnotationAttributes ann = findAutowiredAnnotation(field);
				if (ann != null) {
                    //如果发现是静态的，直接返回了，不处理了
					if (Modifier.isStatic(field.getModifiers())) {
						if (logger.isInfoEnabled()) {
							logger.info("Autowired annotation is not supported on static fields: " + field);
						}
						return;
					}
					boolean required = determineRequiredStatus(ann);
					currElements.add(new AutowiredFieldElement(field, required));
				}
			});
            //查找所有的方法带有@Autowired、@Inject、@value注解的，加入到currElements中
			ReflectionUtils.doWithLocalMethods(targetClass, method -> {
				Method bridgedMethod = BridgeMethodResolver.findBridgedMethod(method);
				if (!BridgeMethodResolver.isVisibilityBridgeMethodPair(method, bridgedMethod)) {
					return;
				}
				AnnotationAttributes ann = findAutowiredAnnotation(bridgedMethod);
				if (ann != null && method.equals(ClassUtils.getMostSpecificMethod(method, clazz))) {           //如果发现是静态的，直接返回了，不处理了
					if (Modifier.isStatic(method.getModifiers())) {
						if (logger.isInfoEnabled()) {
							logger.info("Autowired annotation is not supported on static methods: " + method);
						}
						return;
					}
                    //如果方法没有参数，也不报错，就给个提示
					if (method.getParameterCount() == 0) {
						if (logger.isInfoEnabled()) {
							logger.info("Autowired annotation should only be used on methods with parameters: " +
									method);
						}
					}
					boolean required = determineRequiredStatus(ann);
                    //查看是不是java自省的get set方法
					PropertyDescriptor pd = BeanUtils.findPropertyForMethod(bridgedMethod, clazz);
                    //加入currElements
					currElements.add(new AutowiredMethodElement(method, required, pd));
				}
			});
            //将currElements再加入到elements中
			elements.addAll(0, currElements);
            //如果还存在父类，则继续循环找
			targetClass = targetClass.getSuperclass();
		}while (targetClass != null && targetClass != Object.class);
        //然后构造一个注入的源数据信息类
		return new InjectionMetadata(clazz, elements);
	}
```

从上述代码中我们可以分析得出，总共做了这几件时间

1、找到带有@Autowired、@Inject、@value注解的字段

2、找到带有@Autowired、@Inject、@value注解的方法，如果方法总没有任何参数，也不会报错，就打印下提示

3、如果发现方法或者字段中存在静态，立即返回掉，注入失败

4、然后将找到的这些信息封装到InjectionMetadata（注入源信息中）返回

5、检查去重，并设置到RootBeanDefinition中



可以看到，spring的MergedBeanDefinitionPostProcessor在该类中的作用，那就是找到属性或者方法中存在@Autowired、@Inject、@value的注解，然后将这些个方法和属性去重设置到RootBeanDefinition中去，就是这么简单，如果你理解了AutowiredAnnotationBeanPostProcessor ，那么CommonAnnotationBeanPostProcessor， InitDestroyAnnotationBeanPostProcessor  其实是一样的实现策略

##### 1.1.2  ApplicationListenerDetector

```java
@Override
public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {
		this.singletonNames.put(beanName, beanDefinition.isSingleton());
}
```

这个方法就更简单了，直接将beanName是不是单例纪录到本地变量中。



这个后置处理器是在容器准备阶段加入的，主要用户探测ApplicationListener的bean，然后将其加入到容器中。就是这么简单



看到这里，基本上，spring的变种后置处理器之一就讲到这里了，还记得上面的问题吗

==MergedBeanDefinitionPostProcessor干啥的，有啥作用？==

简单的说就是在创建bean的前期（创建过程中，此时bean已经创建完成，但是还没有属性设置等），可以收集一系列bean定义的属性，包括bean Class的属性，收集起来，方便后续应用，就是这么简单，那么可以构思下，如果你想自定义一个注解，该怎么办？有思路吗？



#### 1.2  变种二  DestructionAwareBeanPostProcessor

从字面意思理解来看，Destruction（销毁），那么这个bean后置处理器的变种可能和bean销毁相关，先看下接口定义

```java
public interface DestructionAwareBeanPostProcessor extends BeanPostProcessor {
    //对给定的bean进行后置处理（bean的名字和bean的实例化对象）
	void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException;
    //判断给定的bean实例是否需要通过此后处理器进行销毁。
	default boolean requiresDestruction(Object bean) {
		return true;
	}

}
```

那么问题来了？

```
问题三、这个类型的后置处理器作用在什么时候？什么时候被调用的？
```

先看下接口实现吧，一步步来分析下

![](beanpostprocessorimg\4.png)

比较熟悉的InitDestroyAnnotationBeanPostProcessor、ApplicationListenerDetector，SimpleServletPostProcessor，在MergedBeanDefinitionPostProcessor我们也分析了这两个后置处理器，我们继续来分析他们两个

ApplicationListenerDetector

```java
@Override
public void postProcessBeforeDestruction(Object bean, String beanName) {
		if (bean instanceof ApplicationListener) {
			try {
                //从IOC容器中拿到时间广播器，然后移除掉里面的事件
				ApplicationEventMulticaster multicaster = this.applicationContext.getApplicationEventMulticaster();
				multicaster.removeApplicationListener((ApplicationListener<?>) bean);
				multicaster.removeApplicationListenerBean(beanName);
			}
			catch (IllegalStateException ex) {
				// ApplicationEventMulticaster not initialized yet - no need to remove a listener
			}
		}
	}
```

InitDestroyAnnotationBeanPostProcessor

```java

@Override
public void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException {
   //查找声明周期方法
   LifecycleMetadata metadata = findLifecycleMetadata(bean.getClass());
   try {
      //执行Destroy方法
	 metadata.invokeDestroyMethods(bean, beanName);
	 }catch (InvocationTargetException ex) {
		String msg = "Destroy method on bean with name '" + beanName + "' threw an exception";
		if (logger.isDebugEnabled()) {
			logger.warn(msg, ex.getTargetException());
		}else {
			logger.warn(msg + ": " + ex.getTargetException());
			}
	}catch (Throwable ex) {
			logger.warn("Failed to invoke destroy method on bean with name '" + beanName + "'", ex);
		}
	}
```

SimpleServletPostProcessor

```java
@Override
public void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException {
		if (bean instanceof Servlet) {
		
		     //调用Servlet的销毁方法
			((Servlet) bean).destroy();
		}
	}
```

从这三个上面来看，很简单，都是要销毁一些东西，那么执行时机是在哪里呢？

在创建bean的时候

![](beanpostprocessorimg\5.png)



![](beanpostprocessorimg\6.png)

**registerDisposableBeanIfNecessary(beanName, bean, mbd);**

```java
protected void registerDisposableBeanIfNecessary(String beanName, Object bean, RootBeanDefinition mbd) {
		AccessControlContext acc = (System.getSecurityManager() != null ? getAccessControlContext() : null);
                 // 非 Prototype且需要销毁处理（满足任意条件）
				// 条件1：实现 DisposableBean/AutoCloseable（不存在使用 Closeable）
                // 条件2：没配置 <destroy-method>，默认是 close和 shutdown方法
                // 条件3：配置了 <destory-method>，则使用配置的
                // 条件3：以上都不满足，可以通过注册 DestructionAwareBeanPostProcessor，且 requiresDestruction返回 true
		if (!mbd.isPrototype() && requiresDestruction(bean, mbd)) {
			if (mbd.isSingleton()) {
                // 注册一个 DisposableBean实现，该实现为给定的 bean执行所有销毁工作：
                // DestructionAwareBeanPostProcessors，DisposableBean接口，自定义销毁方法
                // 注册单例 beanName和 DisposableBeanAdapter映射关系
				registerDisposableBean(beanName,
						new DisposableBeanAdapter(bean, beanName, mbd, getBeanPostProcessors(), acc));
			}
			else {
				// 在各 Scope下存储 beanName和 DisposableBeanAdapter映射关系
				Scope scope = this.scopes.get(mbd.getScope());
				if (scope == null) {
					throw new IllegalStateException("No Scope registered for scope name '" + mbd.getScope() + "'");
				}
				scope.registerDestructionCallback(beanName,
						new DisposableBeanAdapter(bean, beanName, mbd, getBeanPostProcessors(), acc));
			}
		}
	}
```

可以看到，在spring IOC创建bean完成后，需要调用registerDisposableBeanIfNecessary方法，该方法主要干了以下几个步骤的工作

1.如果是非Prototype类型的bean，并且满足以下几个条件的，才去销毁bean

```
（1）如果实现了DisposableBean或者AutoCloseable接口的，直接可以使用
（2）没配置 <destroy-method>，默认是 close和 shutdown方法
（3）配置了 <destory-method>，则使用配置的
（4）以上都不满足，可以通过注册 DestructionAwareBeanPostProcessor，且 requiresDestruction返回 true的时候
```

2.如果满足条件，并且是单例，则将bean封装成DisposableBeanAdapter（它实现了DisposableBean），并将其注册到容器中的disposableBeans，然后在销毁时通过映射关系找到并调用其 destroy 方法。

3.如果不是单例，则在Scope下存储各自的关系



完成bean创建的时候，如果发生异常，则要销毁bean

![](beanpostprocessorimg\7.png)

容器关闭的时候也要去销毁bean

![](beanpostprocessorimg\8.png)



```java
 public void destroySingleton(String beanName) {
        // 移除一个已注册的单例
        removeSingleton(beanName);

        // 取出对应的 DisposableBean
        DisposableBean disposableBean;
        synchronized (this.disposableBeans) {
            disposableBean = (DisposableBean) this.disposableBeans.remove(beanName);
        }
        // 调用相关 bean、自身、以及包含的成员 bean
        // DisposableBean.destroy()方法
        destroyBean(beanName, disposableBean);
    }
```

#### 1.3 变种三 InstantiationAwareBeanPostProcessor

InstantiationAwareBeanPostProcessor代表了Spring的另外一段生命周期：实例化。先区别一下Spring Bean的实例化和初始化两个阶段的主要作用：

1、实例化—-实例化的过程是一个创建Bean的过程，即调用Bean的构造函数，单例的Bean放入单例池中

2、初始化—-初始化的过程是一个赋值的过程，即调用Bean的setter，设置Bean的属性

之前的BeanPostProcessor作用于过程（2）前后，现在的InstantiationAwareBeanPostProcessor则作用于过程（1）前后；

InstantiationAwareBeanPostProcessor接口继承BeanPostProcessor接口，它内部提供了3个方法，再加上BeanPostProcessor接口内部的2个方法，所以实现这个接口需要实现5个方法。InstantiationAwareBeanPostProcessor接口的主要作用在于目标对象的实例化过程中需要处理的事情，包括实例化对象的前后过程以及实例的属性设置

来看看接口定义

```java
public interface InstantiationAwareBeanPostProcessor extends BeanPostProcessor {

	/**
	 * 
	 */
	@Nullable
	default Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
		return null;
	}
    
    
    default boolean postProcessAfterInstantiation(Object bean, String beanName) throws BeansException {
		return true;
	}


	/**
	 * 
	 * @since 5.1
	 * @see #postProcessPropertyValues
	 */
	@Nullable
	default PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName)
			throws BeansException {

		return null;
	}

	/**
	 *
	 * @deprecated as of 5.1, in favor of {@link #postProcessProperties(PropertyValues, Object, String)}
	 */
	@Deprecated
	@Nullable
	default PropertyValues postProcessPropertyValues(
			PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName) throws BeansException {

		return pvs;
	}

}

```

可以看到，目前有四个方法，和上面说的三个方法不一致，在spring5.1版本，重载了一个==**postProcessProperties(PropertyValues, Object, String)**==方法，而把postProcessPropertyValues（PropertyValues，PropertyDescriptor[]，Object，String）给废弃掉了,

也就是去掉了PropertyDescriptor[]参数，那么我们不尽的想问，为什么？

[去掉用原因](https://jira.spring.io/browse/SPR-16918?redirect=false)、[去掉的原因](https://github.com/spring-projects/spring-framework/issues/21457)

> In a typical Spring Boot app, a significant amount of the startup cost is due to `org.springframework.beans.CachedIntrospectionResults.forClass(Class<?>)`. There's a direct cost in terms of the CPU time taken for introspection and a secondary cost due to increased memory usage and GC pressure.
>
> In the vast majority of cases, the introspection results are retrieved beneath `AbstractAutowireCapableBeanFactory.populateBean(String, RootBeanDefinition, BeanWrapper)` so that it can retrieve the property descriptors and then filter them. If I short-circuit `populateBean` so that it returns early if the bean definition's property values are empty, an app that took 2.7 seconds to start then starts in as little as 2.3 seconds.
>
> The short circuiting loses two pieces of functionality:
>
> 1. The opportunity for `InstantationAwareBeanPostProcessors` to add property values
> 2. `@Required` support
>
> Boot itself doesn't make use of either of these so the current ~400ms cost is a high price to pay. I'd like to explore the possibility of lowering or removing this cost.
>
> As far as I can tell only one of the built-in post-processors, `RequiredAnnotationBeanPostProcessor`, uses the property descriptors, and it only does so if the bean definition hasn't been marked to skip the required check. One possibility may be to retrieve the property descr

> 在典型的Spring Boot应用程序中，大量的启动成本是由org.springframework.beans.CachedIntrospectionResults.forClass（Class <？>）引起的。由于内存使用和GC压力的增加，内省占用的CPU时间和成本都直线上升。
>
> 在绝大多数情况下，内省结果在AbstractAutowireCapableBeanFactory.populateBean（String，RootBeanDefinition，BeanWrapper）下检索，以便它可以检索属性然后过滤它们。如果我将populateBean去掉，以便在bean定义的属性值为空时提前返回，则启动2.7秒的应用程序将在2.3秒内启动。
>
> 去掉populateBean失去了两个功能：
>
> InstantationAwareBeanPostProcessors添加属性值的机会
> @Required  @Autowired等支持
>
> 启动本身并没有使用其中两个中的任何一个，因此目前约400毫秒的成本是一个很高的代价。我想探讨降低或降低这笔消耗的可能性。
>
> 据我所知，只有一个内置的后处理器RequiredAnnotationBeanPostProcessor使用属性描述符，并且只有在bean定义没有标记为跳过所需的检查时才会这样做。一种可能性是懒惰地检索属性描述符，以及Boot以某种方式用skipRequiredCheck标记其bean定义。

说了这么多，就是springboot项目启动时间因为java的内省机制而导致性能上的问题，而且鉴于PropertyDescriptor[] pds基本就RequiredAnnotationBeanPostProcessor用，但是项目启动的时候又不用，所以，spring团队直接把RequiredAnnotationBeanPostProcessor在5.1废弃掉了，而且把InstantiationAwareBeanPostProcessor的接口定义方法换了。

------

好了，说了这么多，进入正题吧，InstantiationAwareBeanPostProcessor到底在哪里使用的？

##### 1.3.1  postProcessBeforeInstantiation方法调用

![](beanpostprocessorimg\9.png)

在创建bean实例之前，会调用resolveBeforeInstantiation方法给后置处理器一个机会，让他返回一个代理bean

```java
protected Object resolveBeforeInstantiation(String beanName, RootBeanDefinition mbd) {
		Object bean = null;
		if (!Boolean.FALSE.equals(mbd.beforeInstantiationResolved)) {
			// Make sure bean class is actually resolved at this point.
			if (!mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
                //拿到bean的类型
				Class<?> targetType = determineTargetType(beanName, mbd);
				if (targetType != null) {
                    //执行InstantiationAwareBeanPostProcessor实例化方法
					bean = applyBeanPostProcessorsBeforeInstantiation(targetType, beanName);          //如果得到了代理bean，则执行BeanPostProcessor的postProcessAfterInitialization初始化后置方法
					if (bean != null) {
						bean = applyBeanPostProcessorsAfterInitialization(bean, beanName);
					}
				}
			}
            //然后再RootBeanDefinition记录下提前被InstantiationAwareBeanPostProcessor代理的方法
			mbd.beforeInstantiationResolved = (bean != null);
		}
		return bean;
	}
```

##### 1.3.2 boolean postProcessAfterInstantiation(Object bean, String beanName)方法调用

可以看到，和上面的实例化方法不同的是，传入的参数也发生了变化，传入了beanName和实例化后的对象，返回值也直接返回一个布尔类型的值。那么我们简单的可以判定，这个方法调用，肯定是已经实例化了bean。

来看下代码

```java
protected void populateBean(String beanName, RootBeanDefinition mbd, @Nullable BeanWrapper bw) {
  ......
  // Give any InstantiationAwareBeanPostProcessors the opportunity to modify the
	// state of the bean before properties are set. This can be used, for example,
	// to support styles of field injection.
	boolean continueWithPropertyPopulation = true;
    //在这里判断了是不是InstantiationAwareBeanPostProcessor类型的
	if (!mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
		for (BeanPostProcessor bp : getBeanPostProcessors()) {
			if (bp instanceof InstantiationAwareBeanPostProcessor) {
				InstantiationAwareBeanPostProcessor ibp = (InstantiationAwareBeanPostProcessor) bp;
                //如果是就直接调用postProcessAfterInstantiation
				if (!ibp.postProcessAfterInstantiation(bw.getWrappedInstance(), beanName)) {       //如果返回了false，就直接重新跳出，不再继续注入了
					continueWithPropertyPopulation = false;
					break;
				}
			}
		}
	}
    //如果返回了false，那么注入就直接完成了
   if (!continueWithPropertyPopulation) {
			return;
		}

	PropertyValues pvs = (mbd.hasPropertyValues() ? mbd.getPropertyValues() : null);

	if (mbd.getResolvedAutowireMode() == AUTOWIRE_BY_NAME || mbd.getResolvedAutowireMode() == AUTOWIRE_BY_TYPE) {
		MutablePropertyValues newPvs = new MutablePropertyValues(pvs);
		// Add property values based on autowire by name if applicable.
		if (mbd.getResolvedAutowireMode() == AUTOWIRE_BY_NAME) {
            //按照名称注入
			autowireByName(beanName, mbd, bw, newPvs);
		}
		//按照类型注入
		if (mbd.getResolvedAutowireMode() == AUTOWIRE_BY_TYPE) {
				autowireByType(beanName, mbd, bw, newPvs);
		}
		pvs = newPvs;
    }    
    //下面的代码是判断是否需要执行postProcessPropertyValues改变bean的属性   
    boolean hasInstAwareBpps = hasInstantiationAwareBeanPostProcessors();
	boolean needsDepCheck = (mbd.getDependencyCheck() != AbstractBeanDefinition.DEPENDENCY_CHECK_NONE);
    PropertyDescriptor[] filteredPds = null;
	if (hasInstAwareBpps) {
		if (pvs == null) {
			pvs = mbd.getPropertyValues();
		}
		for (BeanPostProcessor bp : getBeanPostProcessors()) {
			if (bp instanceof InstantiationAwareBeanPostProcessor) {
				InstantiationAwareBeanPostProcessor ibp = (InstantiationAwareBeanPostProcessor) bp;
				PropertyValues pvsToUse = ibp.postProcessProperties(pvs, bw.getWrappedInstance(), beanName);
				if (pvsToUse == null) {
					if (filteredPds == null) {
						filteredPds = filterPropertyDescriptorsForDependencyCheck(bw, mbd.allowCaching);
					}
					pvsToUse = ibp.postProcessPropertyValues(pvs, filteredPds, bw.getWrappedInstance(), beanName);
					if (pvsToUse == null) {
						return;
					}
				}
				pvs = pvsToUse;
			}
		}
	}
	
}
```
从上面的方法调用，大概能看到postProcessAfterInstantiation是什么时候被调用的，下面来总结下

1.在注入的时候，调用了postProcessAfterInstantiation方法，如果postProcessAfterInstantiation方法一旦返回false，那么就中断属性注入了，这就是postProcessAfterInstantiation的作用

##### 1.3.3 PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName)方法调用

​    这里我们暂且不看以前老的被废弃掉的方法，只要你看懂了上面为啥这个方法被废弃掉以后，我想你也不一定非看这个方法，因为没必要；

​    在1.3.2中,方法之下就有一个postProcessProperties函数的调用，那么这个方法有什么作用呢？没错，就是修改属性（也就是俗称的属性注入）

在这里，我们来看下AutowiredAnnotationBeanPostProcessor的实现，就当是你了解并掌握了它吧

```java
@Override
public PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName) {
    //还记得我们在MergedBeanDefinitionPostProcessor里面获取到带有那些注解的属性和方法了吗，没错，这里就是从缓存里拿，如果拿不到的，那么就再拿一次
	InjectionMetadata metadata = findAutowiringMetadata(beanName, bean.getClass(), pvs);
	try {
        //重点了，这个方法我们单独拿出来，看看我们的属性注入怎么注入的
		metadata.inject(bean, beanName, pvs);
    }
	catch (BeanCreationException ex) {
		throw ex;
	}
	catch (Throwable ex) {
		throw new BeanCreationException(beanName, "Injection of autowired dependencies failed", ex);
	}
	return pvs;
}
```

接下来，我们看看inject这个方法干了些什么

```java
public void inject(Object target, @Nullable String beanName, @Nullable PropertyValues pvs) throws Throwable {
	Collection<InjectedElement> checkedElements = this.checkedElements;
	Collection<InjectedElement> elementsToIterate =
			(checkedElements != null ? checkedElements : this.injectedElements);
	if (!elementsToIterate.isEmpty()) {
		for (InjectedElement element : elementsToIterate) {
			if (logger.isTraceEnabled()) {
				logger.trace("Processing injected element of bean '" + beanName + "': " + element);
			}
            //关键代码
			element.inject(target, beanName, pvs);
		}
	}
}
```

`InjectionMetadata`对象本身是一个包含了一系列`AutowiredFieldElement`和`AutowiredMethodElement`对象所构成；这里呢，通过迭代`InjectedElement`依次处理`AutowiredFieldElement`或`AutowiredMethodElement`元素

继续深挖代码

```java
protected void inject(Object target, @Nullable String requestingBeanName, @Nullable PropertyValues pvs)
				throws Throwable {

	if (this.isField) {
        //如果是属性
		Field field = (Field) this.member;
        // 如果该属性是 private 的，那么 make accessiable
		ReflectionUtils.makeAccessible(field);
        //通过反射将 @Autowired annotated ref-bean 写入当前 bean 的属性中
		field.set(target, getResourceToInject(target, requestingBeanName));
	}
	else {
		if (checkPropertySkipping(pvs)) {
			return;
		}
		try {
            //如果该属性是 private 的，那么 make accessiable
			Method method = (Method) this.member;
			ReflectionUtils.makeAccessible(method);
            //通过反射将 @Autowired annotated ref-bean 写入当前 bean 的属性中
			method.invoke(target, getResourceToInject(target, requestingBeanName));
		}
		catch (InvocationTargetException ex) {
			throw ex.getTargetException();
		}
	}
}
```

特别的简单，是吧，别着急，还没完，这只是把超类分析完了，我们来看下InjectedElement下面的两个子类

> AutowiredFieldElement
>
> AutowiredMethodElement

这里还是有点复杂的，后续再专门抽个篇幅将，这里就不展开讨论了，先记着有这这么一件事情



好，那我们来总结下

1. InstantiationAwareBeanPostProcessor接口继承BeanPostProcessor接口，它内部提供了3个方法，再加上BeanPostProcessor接口内部的2个方法，所以实现这个接口需要实现5个方法。InstantiationAwareBeanPostProcessor接口的主要作用在于目标对象的实例化过程中需要处理的事情，包括实例化对象的前后过程以及实例的属性设置
2. postProcessBeforeInstantiation方法是最先执行的方法，它在目标对象实例化之前调用，该方法的返回值类型是Object，我们可以返回任何类型的值。由于这个时候目标对象还未实例化，所以这个返回值可以用来代替原本该生成的目标对象的实例(比如代理对象)。如果该方法的返回值代替原本该生成的目标对象，后续只有postProcessAfterInitialization方法会调用，其它方法不再调用；否则按照正常的流程走
3. postProcessAfterInstantiation方法在目标对象实例化之后调用，这个时候对象已经被实例化，但是该实例的属性还未被设置，都是null。因为它的返回值是决定要不要调用postProcessPropertyValues方法；如果该方法返回false,那么postProcessPropertyValues就会被忽略不执行；如果返回true，postProcessPropertyValues就会被执行
4. postProcessPropertyValues方法对属性值进行修改(这个时候属性值还未被设置，但是我们可以修改原本设置进去的属性值)。如果postProcessAfterInstantiation方法返回false，该方法不会被调用。可以在该方法内对属性值进行修改
5. 父接口BeanPostProcessor的2个方法postProcessBeforeInitialization和postProcessAfterInitialization都是在目标对象被实例化之后，并且属性也被设置之后调用的
6. Instantiation表示实例化，Initialization表示初始化。实例化的意思在对象还未生成，初始化的意思是对象已经生成

#### 1.4 变种四、SmartInstantiationAwareBeanPostProcessor（智能实例化Bean后置处理器）

先看看接口定义

```java
public interface SmartInstantiationAwareBeanPostProcessor extends InstantiationAwareBeanPostProcessor {

	/**
	 * 预测Bean的类型，返回第一个预测成功的Class类型，如果不能预测返回null；当你调用BeanFactory.getType(name)时当通过Bean定义无法得到Bean类型信息时就调用该回调方法来决定类型信息；BeanFactory.isTypeMatch(name, targetType)用于检测给定名字的Bean是否匹配目标类型（如在依赖注入时需要使用）；
	 */
	@Nullable
	default Class<?> predictBeanType(Class<?> beanClass, String beanName) throws BeansException {
		return null;
	}

	/**
	 * 检测Bean的构造器，可以检测出多个候选构造器，再有相应的策略决定使用哪一个，如AutowiredAnnotationBeanPostProcessor实现将自动扫描通过@Autowired/@Value注解的构造器从而可以完成构造器注入
	 */
	@Nullable
	default Constructor<?>[] determineCandidateConstructors(Class<?> beanClass, String beanName)
			throws BeansException {

		return null;
	}

	/**
	 * 当正在创建A时，A依赖B，此时通过（8将A作为ObjectFactory放入单例工厂中进行early expose，此处B需要引用A，但A正在创建，从单例工厂拿到ObjectFactory（其通过getEarlyBeanReference获取及早暴露Bean），从而允许循环依赖，此时AspectJAwareAdvisorAutoProxyCreator（完成xml风格的AOP配置(<aop:config>)将目标对象（A）包装到AOP代理对象）或AnnotationAwareAspectJAutoProxyCreator（完成@Aspectj注解风格（<aop:aspectj-autoproxy> @Aspect）将目标对象（A）包装到AOP代理对象），其返回值将替代原始的Bean对象，即此时通过early reference能得到正确的代理对象
	 */
	default Object getEarlyBeanReference(Object bean, String beanName) throws BeansException {
		return bean;
	}

}
```

主要还是用来实例化bean来处理的

##### 1.4.1 getEarlyBeanReference调用时机

准备两个类，让他们相互引用

```xml
<bean id="circulationa" class="src.bean.CirculationA">
    <property name="circulationB" ref="circulationb"/>
  </bean>
  <bean id="circulationb" class="src.bean.CirculationB" >
    <property name="circulationA" ref="circulationa"/>
  </bean>
```

启动； 

（1）加载circulationa,然后将调用了代码,提前将bean暴露出去，但是这个时候getEarlyBeanReference还没有被调用; 因为没有出现循环引用的情况;现在放入缓存是为了预防有循环引用的情况可以通过这个getEarlyBeanReference获取对象;

```java
addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, mbd, bean));

//然后将其加入缓存中
protected void addSingletonFactory(String beanName, ObjectFactory<?> singletonFactory) {
	Assert.notNull(singletonFactory, "Singleton factory must not be null");
	synchronized (this.singletonObjects) {
		if (!this.singletonObjects.containsKey(beanName)) {
			this.singletonFactories.put(beanName, singletonFactory);
			this.earlySingletonObjects.remove(beanName);
			this.registeredSingletons.add(beanName);
		}
	}
}

protected Object getEarlyBeanReference(String beanName, RootBeanDefinition mbd, Object bean) {
	Object exposedObject = bean;
    //此时，如果没有SmartInstantiationAwareBeanPostProcessor，那么返回的就是他本身
	if (!mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
		for (BeanPostProcessor bp : getBeanPostProcessors()) {
			if (bp instanceof SmartInstantiationAwareBeanPostProcessor) {
				SmartInstantiationAwareBeanPostProcessor ibp = (SmartInstantiationAwareBeanPostProcessor) bp;
				exposedObject = ibp.getEarlyBeanReference(exposedObject, beanName);
			}
		}
	}
	return exposedObject;
}

```

（2） 然后填充属性值;调用下面的方法，在填充属性的时候发现引用了circulationb；然后就去获取circulationb来填充

==populateBean(beanName, mbd, instanceWrapper);==

（3）加载circulationb, 执行的操作跟 1,2一样; circulationb发现了引用了circulationa；然后直接调用getSingleton获取circulationa;


这一步返回的就是 getEarlyBeanReference得到的值； 
4.执行getEarlyBeanReference方法

```java
protected Object getSingleton(String beanName, boolean allowEarlyReference) {
	Object singletonObject = this.singletonObjects.get(beanName);
	if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
		synchronized (this.singletonObjects) {
			singletonObject = this.earlySingletonObjects.get(beanName);
			if (singletonObject == null && allowEarlyReference) {
				ObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName);
				if (singletonFactory != null) {
					//这个地方就是调用getEarlyBeanReference的地方了;
					singletonObject = singletonFactory.getObject();
					this.earlySingletonObjects.put(beanName, singletonObject);
					this.singletonFactories.remove(beanName);
				}
			}
		}
	}
	return (singletonObject != NULL_OBJECT ? singletonObject : null);
}
```

4.1 一般情况下，如果系统中没有SmartInstantiationAwareBeanPostProcessor接口；就是直接返回exposedObject什么也不做; 

4.2  所以利用SmartInstantiationAwareBeanPostProcessor可以改变一下提前暴露的对象;

==得出结论，如果存在循环依赖，就会调用getEarlyBeanReference，那么，如果想修改该实例，那么就可以实现SmartInstantiationAwareBeanPostProcessor的getEarlyBeanReference==

##### 1.4.2 determineCandidateConstructors的调用时机

检测Bean的构造器，可以检测出多个候选构造器，再有相应的策略决定使用哪一个，简单的说，就是多个中确定一个，并且使用

那么调用时机是哪里呢？构造器确定，那么肯定是创建bean的时候咯

![](beanpostprocessorimg\10.png)

![](beanpostprocessorimg\11.png)

在创建的时候，要去选择构造器，这里就可以自己来过滤，要使用哪一个



总体来说，这个变种4，可能真的在写业务代码的时候用不到的，而且spring的实现中也是寥寥无几的，所以我们可以看看，不一定啥时候遇到问题，可能你想到这里就突然有了想法呢？



### 二、应用场景



#### 2.1  sync注解的aop暴露问题

```java
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.scheduling.annotation.AsyncAnnotationBeanPostProcessor;
import org.springframework.scheduling.config.TaskManagementConfigUtils;

/**
 * @author dngzs
 * @date 2019-06-03 17:44
 */
public class SyncBeanPostProcessor implements BeanPostProcessor,PriorityOrdered {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if(beanName.equals(TaskManagementConfigUtils.ASYNC_ANNOTATION_PROCESSOR_BEAN_NAME)){
            ((AsyncAnnotationBeanPostProcessor) bean).setExposeProxy(true);
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {

        return bean;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}

```

#### 2.2 dubbo扫包注入（老版本，已经被废弃了）

```java
package com.alibaba.dubbo.config.spring;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.ConcurrentHashSet;
import com.alibaba.dubbo.common.utils.ReflectUtils;
import com.alibaba.dubbo.config.*;
import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * AnnotationBean（dubbo经典扫包，注入）
 *
 * @export
 */
@Deprecated
public class AnnotationBean extends AbstractConfig implements DisposableBean, BeanFactoryPostProcessor, BeanPostProcessor, ApplicationContextAware {

    private static final long serialVersionUID = -7582802454287589552L;

    private static final Logger logger = LoggerFactory.getLogger(Logger.class);
    private final Set<ServiceConfig<?>> serviceConfigs = new ConcurrentHashSet<ServiceConfig<?>>();
    private final ConcurrentMap<String, ReferenceBean<?>> referenceConfigs = new ConcurrentHashMap<String, ReferenceBean<?>>();
    private String annotationPackage;
    private String[] annotationPackages;
    private ApplicationContext applicationContext;

    public String getPackage() {
        return annotationPackage;
    }

    public void setPackage(String annotationPackage) {
        this.annotationPackage = annotationPackage;
        this.annotationPackages = (annotationPackage == null || annotationPackage.length() == 0) ? null
                : Constants.COMMA_SPLIT_PATTERN.split(annotationPackage);
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
            throws BeansException {
        if (annotationPackage == null || annotationPackage.length() == 0) {
            return;
        }
        if (beanFactory instanceof BeanDefinitionRegistry) {
            try {
                // init scanner
                Class<?> scannerClass = ReflectUtils.forName("org.springframework.context.annotation.ClassPathBeanDefinitionScanner");
                Object scanner = scannerClass.getConstructor(new Class<?>[]{BeanDefinitionRegistry.class, boolean.class}).newInstance(new Object[]{(BeanDefinitionRegistry) beanFactory, true});
                // add filter
                Class<?> filterClass = ReflectUtils.forName("org.springframework.core.type.filter.AnnotationTypeFilter");
                Object filter = filterClass.getConstructor(Class.class).newInstance(Service.class);
                Method addIncludeFilter = scannerClass.getMethod("addIncludeFilter", ReflectUtils.forName("org.springframework.core.type.filter.TypeFilter"));
                addIncludeFilter.invoke(scanner, filter);
                // scan packages
                String[] packages = Constants.COMMA_SPLIT_PATTERN.split(annotationPackage);
                Method scan = scannerClass.getMethod("scan", new Class<?>[]{String[].class});
                scan.invoke(scanner, new Object[]{packages});
            } catch (Throwable e) {
                // spring 2.0
            }
        }
    }

    public void destroy() throws Exception {
        for (ServiceConfig<?> serviceConfig : serviceConfigs) {
            try {
                serviceConfig.unexport();
            } catch (Throwable e) {
                logger.error(e.getMessage(), e);
            }
        }
        for (ReferenceConfig<?> referenceConfig : referenceConfigs.values()) {
            try {
                referenceConfig.destroy();
            } catch (Throwable e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    public Object postProcessAfterInitialization(Object bean, String beanName)
            throws BeansException {
        if (!isMatchPackage(bean)) {
            return bean;
        }
        Service service = bean.getClass().getAnnotation(Service.class);
        if (service != null) {
            ServiceBean<Object> serviceConfig = new ServiceBean<Object>(service);
            serviceConfig.setRef(bean);
            if (void.class.equals(service.interfaceClass())
                    && "".equals(service.interfaceName())) {
                if (bean.getClass().getInterfaces().length > 0) {
                    serviceConfig.setInterface(bean.getClass().getInterfaces()[0]);
                } else {
                    throw new IllegalStateException("Failed to export remote service class " + bean.getClass().getName() + ", cause: The @Service undefined interfaceClass or interfaceName, and the service class unimplemented any interfaces.");
                }
            }
            if (applicationContext != null) {
                serviceConfig.setApplicationContext(applicationContext);
                if (service.registry() != null && service.registry().length > 0) {
                    List<RegistryConfig> registryConfigs = new ArrayList<RegistryConfig>();
                    for (String registryId : service.registry()) {
                        if (registryId != null && registryId.length() > 0) {
                            registryConfigs.add((RegistryConfig) applicationContext.getBean(registryId, RegistryConfig.class));
                        }
                    }
                    serviceConfig.setRegistries(registryConfigs);
                }
                if (service.provider() != null && service.provider().length() > 0) {
                    serviceConfig.setProvider((ProviderConfig) applicationContext.getBean(service.provider(), ProviderConfig.class));
                }
                if (service.monitor() != null && service.monitor().length() > 0) {
                    serviceConfig.setMonitor((MonitorConfig) applicationContext.getBean(service.monitor(), MonitorConfig.class));
                }
                if (service.application() != null && service.application().length() > 0) {
                    serviceConfig.setApplication((ApplicationConfig) applicationContext.getBean(service.application(), ApplicationConfig.class));
                }
                if (service.module() != null && service.module().length() > 0) {
                    serviceConfig.setModule((ModuleConfig) applicationContext.getBean(service.module(), ModuleConfig.class));
                }
                if (service.provider() != null && service.provider().length() > 0) {
                    serviceConfig.setProvider((ProviderConfig) applicationContext.getBean(service.provider(), ProviderConfig.class));
                } else {

                }
                if (service.protocol() != null && service.protocol().length > 0) {
                    List<ProtocolConfig> protocolConfigs = new ArrayList<ProtocolConfig>();
                    for (String protocolId : service.protocol()) {
                        if (protocolId != null && protocolId.length() > 0) {
                            protocolConfigs.add((ProtocolConfig) applicationContext.getBean(protocolId, ProtocolConfig.class));
                        }
                    }
                    serviceConfig.setProtocols(protocolConfigs);
                }
                try {
                    serviceConfig.afterPropertiesSet();
                } catch (RuntimeException e) {
                    throw (RuntimeException) e;
                } catch (Exception e) {
                    throw new IllegalStateException(e.getMessage(), e);
                }
            }
            serviceConfigs.add(serviceConfig);
            serviceConfig.export();
        }
        return bean;
    }

    public Object postProcessBeforeInitialization(Object bean, String beanName)
            throws BeansException {
        if (!isMatchPackage(bean)) {
            return bean;
        }
        Method[] methods = bean.getClass().getMethods();
        for (Method method : methods) {
            String name = method.getName();
            if (name.length() > 3 && name.startsWith("set")
                    && method.getParameterTypes().length == 1
                    && Modifier.isPublic(method.getModifiers())
                    && !Modifier.isStatic(method.getModifiers())) {
                try {
                    Reference reference = method.getAnnotation(Reference.class);
                    if (reference != null) {
                        Object value = refer(reference, method.getParameterTypes()[0]);
                        if (value != null) {
                            method.invoke(bean, new Object[]{value});
                        }
                    }
                } catch (Throwable e) {
                    logger.error("Failed to init remote service reference at method " + name + " in class " + bean.getClass().getName() + ", cause: " + e.getMessage(), e);
                }
            }
        }
        Field[] fields = bean.getClass().getDeclaredFields();
        for (Field field : fields) {
            try {
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }
                Reference reference = field.getAnnotation(Reference.class);
                if (reference != null) {
                    Object value = refer(reference, field.getType());
                    if (value != null) {
                        field.set(bean, value);
                    }
                }
            } catch (Throwable e) {
                logger.error("Failed to init remote service reference at filed " + field.getName() + " in class " + bean.getClass().getName() + ", cause: " + e.getMessage(), e);
            }
        }
        return bean;
    }

    private Object refer(Reference reference, Class<?> referenceClass) { //method.getParameterTypes()[0]
        String interfaceName;
        if (!"".equals(reference.interfaceName())) {
            interfaceName = reference.interfaceName();
        } else if (!void.class.equals(reference.interfaceClass())) {
            interfaceName = reference.interfaceClass().getName();
        } else if (referenceClass.isInterface()) {
            interfaceName = referenceClass.getName();
        } else {
            throw new IllegalStateException("The @Reference undefined interfaceClass or interfaceName, and the property type " + referenceClass.getName() + " is not a interface.");
        }
        String key = reference.group() + "/" + interfaceName + ":" + reference.version();
        ReferenceBean<?> referenceConfig = referenceConfigs.get(key);
        if (referenceConfig == null) {
            referenceConfig = new ReferenceBean<Object>(reference);
            if (void.class.equals(reference.interfaceClass())
                    && "".equals(reference.interfaceName())
                    && referenceClass.isInterface()) {
                referenceConfig.setInterface(referenceClass);
            }
            if (applicationContext != null) {
                referenceConfig.setApplicationContext(applicationContext);
                if (reference.registry() != null && reference.registry().length > 0) {
                    List<RegistryConfig> registryConfigs = new ArrayList<RegistryConfig>();
                    for (String registryId : reference.registry()) {
                        if (registryId != null && registryId.length() > 0) {
                            registryConfigs.add((RegistryConfig) applicationContext.getBean(registryId, RegistryConfig.class));
                        }
                    }
                    referenceConfig.setRegistries(registryConfigs);
                }
                if (reference.consumer() != null && reference.consumer().length() > 0) {
                    referenceConfig.setConsumer((ConsumerConfig) applicationContext.getBean(reference.consumer(), ConsumerConfig.class));
                }
                if (reference.monitor() != null && reference.monitor().length() > 0) {
                    referenceConfig.setMonitor((MonitorConfig) applicationContext.getBean(reference.monitor(), MonitorConfig.class));
                }
                if (reference.application() != null && reference.application().length() > 0) {
                    referenceConfig.setApplication((ApplicationConfig) applicationContext.getBean(reference.application(), ApplicationConfig.class));
                }
                if (reference.module() != null && reference.module().length() > 0) {
                    referenceConfig.setModule((ModuleConfig) applicationContext.getBean(reference.module(), ModuleConfig.class));
                }
                if (reference.consumer() != null && reference.consumer().length() > 0) {
                    referenceConfig.setConsumer((ConsumerConfig) applicationContext.getBean(reference.consumer(), ConsumerConfig.class));
                }
                try {
                    referenceConfig.afterPropertiesSet();
                } catch (RuntimeException e) {
                    throw (RuntimeException) e;
                } catch (Exception e) {
                    throw new IllegalStateException(e.getMessage(), e);
                }
            }
            referenceConfigs.putIfAbsent(key, referenceConfig);
            referenceConfig = referenceConfigs.get(key);
        }
        return referenceConfig.get();
    }

    private boolean isMatchPackage(Object bean) {
        if (annotationPackages == null || annotationPackages.length == 0) {
            return true;
        }
        String beanClassName = bean.getClass().getName();
        for (String pkg : annotationPackages) {
            if (beanClassName.startsWith(pkg)) {
                return true;
            }
        }
        return false;
    }

}

```

### 三、模块总结

该模块主要讲了beanPostProcess相关的东西，大概了解了spring四种变种，了解了spring IOC  bean的实例化和初始化的可扩展插卡，了解了这个，我们可以对spring做一些定制化的东西，具体根据自己的需求可以做一些扩展。本次就先到这里吧，后续在做补充