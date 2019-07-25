#                                 面向切面的spring-aop编程

工作好几年了，一直没有的深入去了解spring Aop的源码，最近刚好在搞这块，所以抽空来研究下它的源码，研究源码之前，我简单的先去总结下可能下面会遇到的一些组件，这样才能更加清晰的阅读源码，去了解spring Aop的奥秘

## 一、Advice

Advice在中文中的意思是==通知、忠告==的意思，他在spring AOP里发挥着重要的作用，重要是用来在目标方法之前或者之后来处理一些事情的，它的接口定义非常简单，而且标注也特别明确，是一个标注接口，可以是任何的类型

```java
/**
 * Tag interface for Advice. Implementations can be any type
 * of advice, such as Interceptors.
 * @author Rod Johnson
 * @version $Id: Advice.java,v 1.1 2004/03/19 17:02:16 johnsonr Exp $
 */
public interface Advice {

}
```

简单的看下它的实现接口以及实现类，有很多，这里我们关注下我们平时用的最多的

![](aopimg\1.png)



## 二、spring官方引导文档的一些感悟

​        Aspect-Oriented Programming (AOP) 面向切面的编程，在oop编程中，我们知道是以类为单元的，而在Aop编程中，则以切面为单元来展开的；







## 三、spring-aop 源码解析

### 1、aop标签解析



#### 1.1  <aop:aspectj-autoproxy />解析方式

在spring 标签解析中，我们知道，要实现自定义标签解析，必须在标签实现的相应的jar包的`META-INF`目录下创建`spring.handlers`文件，我们看到spring-aop下也有个相应的文件

![](aopimg\3.png)

```properties
http\://www.springframework.org/schema/aop=org.springframework.aop.config.AopNamespaceHandler
```

是的，这个`org.springframework.aop.config.AopNamespaceHandler` 就是自定义标签的解析类，进入AopNamespaceHandler

```java
public class AopNamespaceHandler extends NamespaceHandlerSupport {

	@Override
	public void init() {
		// In 2.0 XSD as well as in 2.1 XSD.
		registerBeanDefinitionParser("config", new ConfigBeanDefinitionParser());
		registerBeanDefinitionParser("aspectj-autoproxy", new AspectJAutoProxyBeanDefinitionParser());
		registerBeanDefinitionDecorator("scoped-proxy", new ScopedProxyBeanDefinitionDecorator());

		// Only in 2.0 XSD: moved to context namespace as of 2.1
		registerBeanDefinitionParser("spring-configured", new SpringConfiguredBeanDefinitionParser());
	}

}
```

可以看到aspectj-autoproxy 是在 `AspectJAutoProxyBeanDefinitionParser` 标签解析器中进行了注册

```java
class AspectJAutoProxyBeanDefinitionParser implements BeanDefinitionParser {

	@Override
	@Nullable
	public BeanDefinition parse(Element element, ParserContext parserContext) {
	     //向容器中注册AnnotationAwareAspectJAutoProxyCreator，该类是spring aop的核心
        AopNamespaceUtils.registerAspectJAnnotationAutoProxyCreatorIfNecessary(parserContext, element);
        //读取配置，填充<aop:include name=''>
		extendBeanDefinition(element, parserContext);
		return null;
	}
    
	private void extendBeanDefinition(Element element, ParserContext parserContext) {
		//获取上述创建的AnnotationAwareAspectJAutoProxyCreator的bean定义信息
        BeanDefinition beanDef =
				parserContext.getRegistry().getBeanDefinition(AopConfigUtils.AUTO_PROXY_CREATOR_BEAN_NAME);
        //解析当前标签的子标签
		if (element.hasChildNodes()) {
			addIncludePatterns(element, parserContext, beanDef);
		}
	}
    
    
    // 解析子标签中的name属性，其可以有多个，这个name属性最终会被添加到
    // AnnotationAwareAspectJAutoProxyCreator的includePatterns属性中，
    // Spring在判断一个类是否需要进行代理的时候会判断当前bean的名称是否与includePatterns中的
    // 正则表达式相匹配，如果不匹配，则不进行代理
	private void addIncludePatterns(Element element, ParserContext parserContext, BeanDefinition beanDef) {
		ManagedList<TypedStringValue> includePatterns = new ManagedList<>();
		NodeList childNodes = element.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node instanceof Element) {
				Element includeElement = (Element) node;
                //解析子标签的name属性
				TypedStringValue valueHolder = new TypedStringValue(includeElement.getAttribute("name"));
				valueHolder.setSource(parserContext.extractSource(includeElement));
				//解析到的加入到includePatterns中
                includePatterns.add(valueHolder);
			}
		}
        // 将解析到的name属性设置到AnnotationAwareAspectJAutoProxyCreator的
        //includePatterns属性中
		if (!includePatterns不等于空，则将加入到.isEmpty()) {
			includePatterns.setSource(parserContext.extractSource(element));
			beanDef.getPropertyValues().add("includePatterns", includePatterns);
		}
	}

}

```

在这里其实就是干了两件事

1. ==注册AnnotationAwareAspectJAutoProxyCreator的BeanDefinition，aop的核心代码都在这里AnnotationAwareAspectJAutoProxyCreator==
2. 解析aop:aspectj-autoproxy 标签的子标签<aop:include name=''>，将其内容加入到 AnnotationAwareAspectJAutoProxyCreator  的属性includePatterns中去

其中我们主要来看下AspectJAnnotationAutoProxyCreator的注入

```java
public static void registerAspectJAnnotationAutoProxyCreatorIfNecessary(
			ParserContext parserContext, Element sourceElement) {
    // 注册AnnotationAwareAspectJAutoProxyCreator的BeanDefinition
	BeanDefinition beanDefinition = AopConfigUtils.registerAspectJAnnotationAutoProxyCreatorIfNecessary(
			parserContext.getRegistry(), parserContext.extractSource(sourceElement));
	// 解析标签中的proxy-target-class和expose-proxy属性值，
    // proxy-target-class主要控制是使用Jdk代理还是Cglib代理实现，expose-proxy用于控制
    // 是否将生成的代理类的实例防御AopContext中，并且暴露给相关子类使用
    useClassProxyingIfNecessary(parserContext.getRegistry(), sourceElement);
    //将注册的BeanDefinition封装到BeanComponentDefinition中,并注册进入ParserContext中
	registerComponentIfNecessary(beanDefinition, parserContext);
}
```

我们看到，在第一步AopConfigUtils#registerAspectJAnnotationAutoProxyCreatorIfNecessary，这里其实就已经注册了AnnotationAwareAspectJAutoProxyCreator进入IOC容器

然后读取到标签中proxy-target-class和expose-proxy两大属性值，填入到注入的bean定义中

最后将生成的beanDefinition封装进BeanComponentDefinition中，进而注册到parserContext中去

```java
private static void useClassProxyingIfNecessary(BeanDefinitionRegistry registry, 
       @Nullable Element sourceElement) {
    if (sourceElement != null) {
        // 解析标签中的proxy-target-class属性值
        boolean proxyTargetClass =
            Boolean.valueOf(sourceElement.getAttribute(PROXY_TARGET_CLASS_ATTRIBUTE));
        if (proxyTargetClass) {
            // 将解析得到的proxy-target-class属性值设置到上面生成的
            // AnnotationAwareAspectJAutoProxyCreator的BeanDefinition的proxyTargetClass
            // 属性中
            AopConfigUtils.forceAutoProxyCreatorToUseClassProxying(registry);
        }
        // 解析标签中的expose-proxy属性值
        boolean exposeProxy = 
            Boolean.valueOf(sourceElement.getAttribute(EXPOSE_PROXY_ATTRIBUTE));
        if (exposeProxy) {
            // 将解析得到的expose-proxy属性值设置到
            // AnnotationAwareAspectJAutoProxyCreator的exposeProxy属性中
            AopConfigUtils.forceAutoProxyCreatorToExposeProxy(registry);
        }
    }
}

private static void registerComponentIfNecessary(@Nullable BeanDefinition beanDefinition, 
       ParserContext parserContext) {
    // 如果生成的AnnotationAwareAspectJAutoProxyCreator的BeanDefinition成功，则将其封装到
    // BeanComponentDefinition中，并且将其添加到ParserContext中
    if (beanDefinition != null) {
        BeanComponentDefinition componentDefinition =
            new BeanComponentDefinition(beanDefinition, 
                AopConfigUtils.AUTO_PROXY_CREATOR_BEAN_NAME);
        parserContext.registerComponent(componentDefinition);
    }
}
```

```java
@Nullable
public static BeanDefinition registerAutoProxyCreatorIfNecessary(
BeanDefinitionRegistry registry, @Nullable Object source) {

   return registerOrEscalateApcAsRequired(InfrastructureAdvisorAutoProxyCreator.class, registry, source);
}
```

```java
@Nullable
private static BeanDefinition registerOrEscalateApcAsRequired(
		Class<?> cls, BeanDefinitionRegistry registry, @Nullable Object source) {

	Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
    //先去判断容器中是否已经注入了
	if (registry.containsBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME)) {
        //如果注入，则获取容器中现有的bean定义
		BeanDefinition apcDefinition = registry.getBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME);
        //判断容器中注入的类型和现在要注入的类型是否相等
		if (!cls.getName().equals(apcDefinition.getBeanClassName())) {
            //如果不等，则获取容器中现有注册的bean的优先级
			int currentPriority = findPriorityForClass(apcDefinition.getBeanClassName());
			//现正准备注入的优先级
            int requiredPriority = findPriorityForClass(cls);
            //如果先注入的优先级比目前容器中的大
			if (currentPriority < requiredPriority) {
                //则将其类名设置为当前要注册的BeanDefinition的名称
				apcDefinition.setBeanClassName(cls.getName());
			}
		}
		return null;
	}
    //如果容器中不存在，则注入，并且将该实例化顺序设置为优先级最高
	RootBeanDefinition beanDefinition = new RootBeanDefinition(cls);
	beanDefinition.setSource(source);
	beanDefinition.getPropertyValues().add("order", Ordered.HIGHEST_PRECEDENCE);
	beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
    //加入到容器中
	registry.registerBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME, beanDefinition);
	return beanDefinition;
}
```

上面就是注入的全部流程了

1. 先判断容器中是否已经注入了，如果已经注入，就判断当前注入的和已经注入的是不是同一类型

    (1)  如果是不同类型的就比较优先级，这里比较优先级的方法是这样的

```java
private static final List<Class<?>> APC_PRIORITY_LIST = new ArrayList<>(3);
APC_PRIORITY_LIST.add(InfrastructureAdvisorAutoProxyCreator.class);   
APC_PRIORITY_LIST.add(AspectJAwareAdvisorAutoProxyCreator.class);
APC_PRIORITY_LIST.add(AnnotationAwareAspectJAutoProxyCreator.class);
```

​           根据在list中加入的顺序，最后加入的优先级最高

​        (2)如果目前正要注入的比容器中的优先级更高，将其类名设置为当前要注册的BeanDefinition的名称

2. 否则，容器中肯定是没有，则生成bean定义信息，并且注入到容器

   **==这里需要知道的是，Spring注册该bean的时候使用的order是`Ordered.HIGHEST_PRECEDENCE`，这么设置的原因在于Spring使用该bean进行切面逻辑的织入，因而这个bean必须在所有用户自定义的bean实例化之前进行实例化，而用户自定义的bean的实例化优先级是比较低的，这样才能实现织入代理逻辑的功能==**

### 2、



