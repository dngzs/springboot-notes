#                                 springAOP

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