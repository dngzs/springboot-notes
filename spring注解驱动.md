# spring boot 笔记

## 1、spring boot注解

### 1.1 @Configuration 注解

####  1.1.1   @Configuration介绍

  我们都知道Springboot是基于java config的，虽然可以将SpringApplication与XML一起混合使用，但spring更推荐你使用@Configuration注解来配置单个java bean,通常main方法所在的类被定义为@Configuration

但是，你也没有必要将所有的java bean都放入@Configuration修饰的类中，你可以用**@import**注解导入配置类，或者你也可以用**@ComponentScan**自动获取所有spring组件，其中就包括@Configuration 注释的类

其中需要注意的是

* springboot推荐使用@Configuration 来配置你的java bean, 如果你必须使用xml，那么还是建议你用@Configuration，然后通过@ImportResource注解来加载xml配置文件，比如这样

![](image\1.png)

* @SpringBootConfiguration注解底层就是@Configuration注解

  ![](image\2.png)

#### 1.1.2  @Configuration底层原理

先来看下整体调用过程

```
SpringApplication.run()
SpringApplication.ConfigurableApplicationContext run(String... args)
SpringApplication.prepareContext()
SpringApplication.load(ApplicationContext context, Object[] sources)
SpringApplication.createBeanDefinitionLoader()
   BeanDefinitionLoader.BeanDefinitionLoader(BeanDefinitionRegistry, Object)构造方法
   AnnotatedBeanDefinitionReader.AnnotatedBeanDefinitionReader(BeanDefinitionRegistry      )构造方法
   AnnotatedBeanDefinitionReader.AnnotatedBeanDefinitionReader(BeanDefinitionRegistry，    Environment)构造方法
   AnnotationConfigUtils.registerAnnotationConfigProcessors(BeanDefinitionRegistry)
      
```

springboot启动期间，会调用**SpringApplication.prepareContext()**方法进行容器准备阶段的配置，然后通过BeanDefinitionLoader加载注解方式的配置处理类，然后通过生产AnnotatedBeanDefinitionReader的方式，向IOC容器中添加这些处理的组件

那么处理@Configuration注解的类就是ConfigurationClassPostProcessor处理器

```java
//bean的名字叫org.springframework.context.annotation.internalConfigurationAnnotationProcessor

if (!registry.containsBeanDefinition(CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME)) {
   RootBeanDefinition def = new RootBeanDefinition(ConfigurationClassPostProcessor.class);
   def.setSource(source);
   beanDefs.add(registerPostProcessor(registry, def, CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME));
}
```

来看下怎么处理的





