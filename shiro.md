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

