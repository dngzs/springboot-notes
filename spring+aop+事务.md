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



#### 1.1 <!--<aop:aspectj-autoproxy/>-->解析方式

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
		AopNamespaceUtils.registerAspectJAnnotationAutoProxyCreatorIfNecessary(parserContext, element);
		extendBeanDefinition(element, parserContext);
		return null;
	}

	private void extendBeanDefinition(Element element, ParserContext parserContext) {
		BeanDefinition beanDef =
				parserContext.getRegistry().getBeanDefinition(AopConfigUtils.AUTO_PROXY_CREATOR_BEAN_NAME);
		if (element.hasChildNodes()) {
			addIncludePatterns(element, parserContext, beanDef);
		}
	}

	private void addIncludePatterns(Element element, ParserContext parserContext, BeanDefinition beanDef) {
		ManagedList<TypedStringValue> includePatterns = new ManagedList<>();
		NodeList childNodes = element.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node instanceof Element) {
				Element includeElement = (Element) node;
				TypedStringValue valueHolder = new TypedStringValue(includeElement.getAttribute("name"));
				valueHolder.setSource(parserContext.extractSource(includeElement));
				includePatterns.add(valueHolder);
			}
		}
		if (!includePatterns.isEmpty()) {
			includePatterns.setSource(parserContext.extractSource(element));
			beanDef.getPropertyValues().add("includePatterns", includePatterns);
		}
	}

}

```

