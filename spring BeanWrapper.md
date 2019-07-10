# spring beanWrapper详解

## 1. beanWrapper接口

```java
/**
 * 通过BeanWrapper,spring ioc容器可以用统一的方式来访问bean的属性,一般情况下你是用不到的
 *
 * @see PropertyAccessor 属性访问器
 * @see PropertyEditorRegistry 属性编辑注册器
 * @see PropertyAccessorFactory#forBeanPropertyAccess  属性访问器工厂
 * @see org.springframework.beans.factory.BeanFactory  bean工厂
 * @see org.springframework.validation.BeanPropertyBindingResult  bean属性绑定结果，用来注册和评估绑定错误的
 * @see org.springframework.validation.DataBinder#initBeanPropertyAccess() 时间绑定器的默认初始化属性访问方式
 */
public interface BeanWrapper extends ConfigurablePropertyAccessor {

   /**
    * 返回此对象包装的bean实例，如果没有的话就返回空
    */
   Object getWrappedInstance();

   /**
    * 返回包装的JavaBean对象的类型。如果没有此包装类型，返回空
    */
   Class<?> getWrappedClass();

   /**
    * 获取包装对象的PropertyDescriptors
    * PropertyDescriptors属性描述符类。通过该类提供的一系列方法来访问java类中的私有属性
    */
   PropertyDescriptor[] getPropertyDescriptors();

   /**
    * 获取特定属性的属性描述符
    * 如果没有该属性抛出无效的属性异常
    */
   PropertyDescriptor getPropertyDescriptor(String propertyName) throws InvalidPropertyException;

   /**
    * 指定数组和集合自动增长的限制
    * 普通BeanWrapper的默认值是无限制的。
    */
   void setAutoGrowCollectionLimit(int autoGrowCollectionLimit);

   /**
    * 获取数组和集合自动增长的限制值
    */
   int getAutoGrowCollectionLimit();

}
```

其中==BeanWrapperImpl==类是对BeanWrapper接口的默认实现，它包装了一个bean对象，缓存了bean的内省结果，并可以访问bean的属性、设置bean的属性值。BeanWrapperImpl类提供了许多默认属性编辑器，支持多种不同类型的类型转换，可以将数组、集合类型的属性转换成指定特殊类型的数组或集合。用户也可以注册自定义的属性编辑器在BeanWrapperImpl中

## 1.2 BeanWrapperImpl

```java
/**
 * 默认的BeanWrapper实现,应该足够了适用于所有典型用例。 缓存内省效率的结果.
 *
 * 注意：除了jdk标准的PropertyEditors外，还默认自动注册                   org.springframework.beans.propertyeditors包下的属性编辑器
 * 如果你有一些特殊的情况需要处理，你也可以注册自己的属性编辑器，详情你可以看看PropertyEditorRegistrySupport
 *
 * 如果你自己也要用，请访问PropertyAccessorFactory#forBeanPropertyAccess
 * 
 * @see #registerCustomEditor
 * @see #setPropertyValues
 * @see #setPropertyValue
 * @see #getPropertyValue
 * @see #getPropertyType
 * @see BeanWrapper
 * @see PropertyEditorRegistrySupport
 */
public class BeanWrapperImpl extends AbstractPropertyAccessor implements BeanWrapper {

   /**
    * We'll create a lot of these objects, so we don't want a new logger every time.
    */
   private static final Log logger = LogFactory.getLog(BeanWrapperImpl.class);

   private static Class<?> javaUtilOptionalClass = null;

   static {
      try {
         javaUtilOptionalClass =
               ClassUtils.forName("java.util.Optional", BeanWrapperImpl.class.getClassLoader());
      }
      catch (ClassNotFoundException ex) {
         // Java 8 not available - Optional references simply not supported then.
      }
   }


   /** 被包装的类 */
   private Object object;

   private String nestedPath = "";

   private Object rootObject;

   /**
    * 用于调用属性方法的安全上下文
    */
   private AccessControlContext acc;

   /**
    * Cached introspections results for this object, to prevent encountering
    * the cost of JavaBeans introspection every time.
    */
   private CachedIntrospectionResults cachedIntrospectionResults;

   /**
    * Map with cached nested BeanWrappers: nested path -> BeanWrapper instance.
    */
   private Map<String, BeanWrapperImpl> nestedBeanWrappers;

   private int autoGrowCollectionLimit = Integer.MAX_VALUE;


   /**
    * Create new empty BeanWrapperImpl. Wrapped instance needs to be set afterwards.
    * Registers default editors.
    * @see #setWrappedInstance
    */
   public BeanWrapperImpl() {
      this(true);
   }

   /**
    * Create new empty BeanWrapperImpl. Wrapped instance needs to be set afterwards.
    * @param registerDefaultEditors whether to register default editors
    * (can be suppressed if the BeanWrapper won't need any type conversion)
    * @see #setWrappedInstance
    */
   public BeanWrapperImpl(boolean registerDefaultEditors) {
      if (registerDefaultEditors) {
         registerDefaultEditors();
      }
      this.typeConverterDelegate = new TypeConverterDelegate(this);
   }

   /**
    * Create new BeanWrapperImpl for the given object.
    * @param object object wrapped by this BeanWrapper
    */
   public BeanWrapperImpl(Object object) {
      registerDefaultEditors();
      setWrappedInstance(object);
   }

   /**
    * Create new BeanWrapperImpl, wrapping a new instance of the specified class.
    * @param clazz class to instantiate and wrap
    */
   public BeanWrapperImpl(Class<?> clazz) {
      registerDefaultEditors();
      setWrappedInstance(BeanUtils.instantiateClass(clazz));
   }

   /**
    * Create new BeanWrapperImpl for the given object,
    * registering a nested path that the object is in.
    * @param object object wrapped by this BeanWrapper
    * @param nestedPath the nested path of the object
    * @param rootObject the root object at the top of the path
    */
   public BeanWrapperImpl(Object object, String nestedPath, Object rootObject) {
      registerDefaultEditors();
      setWrappedInstance(object, nestedPath, rootObject);
   }

   /**
    * Create new BeanWrapperImpl for the given object,
    * registering a nested path that the object is in.
    * @param object object wrapped by this BeanWrapper
    * @param nestedPath the nested path of the object
    * @param superBw the containing BeanWrapper (must not be {@code null})
    */
   private BeanWrapperImpl(Object object, String nestedPath, BeanWrapperImpl superBw) {
      setWrappedInstance(object, nestedPath, superBw.getWrappedInstance());
      setExtractOldValueForEditor(superBw.isExtractOldValueForEditor());
      setAutoGrowNestedPaths(superBw.isAutoGrowNestedPaths());
      setAutoGrowCollectionLimit(superBw.getAutoGrowCollectionLimit());
      setConversionService(superBw.getConversionService());
      setSecurityContext(superBw.acc);
   }
   ..............
}
```

## 1.3 jdk内省 （Introspection）

内省(IntroSpector)是Java 语言对 Bean 类属性、事件的一种缺省处理方法。例如类 A 中有属性 name, 那我们可以通过 getName,setName 来得到其值或者设置新的值。通过 getName/setName 来访问 name 属性，这就是默认的规则

Java 中提供了一套 API 用来访问某个属性的 getter/setter 方法，通过这些 API 可以使你不需要了解这个规则（但你最好还是要搞清楚），这些 API 存放于包 java.beans 中,一般的做法是通过类 Introspector 的 getBeanInfo方法 来获取某个对象的 BeanInfo 信息,然后通过 BeanInfo 来获取属性的描述器(PropertyDescriptor),通过这个属性描述器就可以获取某个属性对应的 getter/setter 方法,然后我们就可以通过反射机制来调用这些方法。

### 1.3.1 PropertyDescriptor

![](jdkImg\1.png)

### 1.3.2  BeanInfo

![](jdkImg\2.png)

说到jdk的内省，不得不提，这样写代码很是费劲，那么有更加优雅的代码来描述吗？有的

==commons-beanutils.jar==

### 1.3.3 commons-beanutils工具类的使用

![](jdkImg\3.png)

## 1.4  spring自省的封装(CachedIntrospectionResults)

### 1.4.1 为给定的bean class创建CachedIntrospectionResults对象

```java
/**
	 * Map keyed by Class containing CachedIntrospectionResults, strongly held.
	 * This variant is being used for cache-safe bean classes.
	 */
	static final ConcurrentMap<Class<?>, CachedIntrospectionResults> strongClassCache =
			new ConcurrentHashMap<Class<?>, CachedIntrospectionResults>(64);

	/**
	 * Map keyed by Class containing CachedIntrospectionResults, softly held.
	 * This variant is being used for non-cache-safe bean classes.
	 */
	static final ConcurrentMap<Class<?>, CachedIntrospectionResults> softClassCache =
			new ConcurrentReferenceHashMap<Class<?>, CachedIntrospectionResults>(64);
//CachedIntrospectionResults内部维护了一个cache安全和非安全的ConcurrentMap来缓存
//（判断类是否是可以缓存的，原理很简单，就是判断该类型是否在指定classloader或者其parent classloader中，双亲委派的原理）
static CachedIntrospectionResults forClass(Class<?> beanClass) throws BeansException {
   //如果能在安全的ConcurrentMap中拿到，则直接返回
   CachedIntrospectionResults results = strongClassCache.get(beanClass);
   if (results != null) {
      return results;
   }
   //如果能在非安全的ConcurrentMap中拿到，则直接返回
   results = softClassCache.get(beanClass);
   if (results != null) {
      return results;
   }
   //如果都拿不到，则创建一个
   results = new CachedIntrospectionResults(beanClass);
   ConcurrentMap<Class<?>, CachedIntrospectionResults> classCacheToUse;

   if (ClassUtils.isCacheSafe(beanClass, CachedIntrospectionResults.class.getClassLoader()) ||
         isClassLoaderAccepted(beanClass.getClassLoader())) {
      classCacheToUse = strongClassCache;
   }
   else {
      if (logger.isDebugEnabled()) {
         logger.debug("Not strongly caching class [" + beanClass.getName() + "] because it is not cache-safe");
      }
      classCacheToUse = softClassCache;
   }

   CachedIntrospectionResults existing = classCacheToUse.putIfAbsent(beanClass, results);
   return (existing != null ? existing : results);
}
```

