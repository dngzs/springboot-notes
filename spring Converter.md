# Converter和TypeConverter

## 1. 转化器

spring在解析的时候提供了三种方式的解析

### 1.1  Converter（转换器）

```java
 /**
  * 将S类型的对象转换成T
  *  @since 3.0
  */
public interface Converter<S, T> {

   /**
    * Convert the source object of type {@code S} to target type {@code T}.
    * @param source the source object to convert, which must be an instance of {@code S} (never {@code null})
    * @return the converted object, which must be an instance of {@code T} (potentially {@code null})
    * @throws IllegalArgumentException if the source cannot be converted to the desired target type
    */
   @Nullable
   T convert(S source);

}
```

### 1.2 ConverterFactory（转换器工厂）

```java
 /**
  * @since 3.0 也就是从spring 3.0开始的
  */
public interface ConverterFactory<S, R> {

   /**
    * Get the converter to convert from S to target type T, where T is also an instance of R.
    * @param <T> the target type
    * @param targetType the target type to convert to
    * @return A converter from S to T
    */
   <T extends R> Converter<S, T> getConverter(Class<T> targetType);

}
```

可以看到，该接口一共有三个泛型参与

S  --------  表示要转换的类

T  --------  转换成什么（也就是这个什么）

R  --------- T继承了R（也就是把S转换成R的子类T）

### 1.3 GenericConverter（通用转换器）

```java
 /**
  * 用于在两种或更多种类型之间转换的通用转换器接口。
  */
public interface GenericConverter {

   /**
    * Return the source and target types which this converter can convert between. Each
    * entry is a convertible source-to-target type pair.
    * <p>
    * For {@link ConditionalConverter conditional} converters this method may return
    * {@code null} to indicate all source-to-target pairs should be considered. *
    */
   Set<ConvertiblePair> getConvertibleTypes();

   /**
    * Convert the source to the targetType described by the TypeDescriptor.
    * @param source 源信息
    * @param sourceType 源信息的上下文。
    * @param targetType  目标信息的上下文
    * @return the converted object
    */
   Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType);


   /**
    * Holder for a source-to-target class pair.
    */
   public static final class ConvertiblePair {

      //源类型
      private final Class<?> sourceType;

       //目标类型
      private final Class<?> targetType;

      /**
       * Create a new source-to-target pair.
       * @param sourceType the source type
       * @param targetType the target type
       */
      public ConvertiblePair(Class<?> sourceType, Class<?> targetType) {
         Assert.notNull(sourceType, "Source type must not be null");
         Assert.notNull(targetType, "Target type must not be null");
         this.sourceType = sourceType;
         this.targetType = targetType;
      }

      public Class<?> getSourceType() {
         return this.sourceType;
      }

      public Class<?> getTargetType() {
         return this.targetType;
      }

      @Override
      public boolean equals(Object obj) {
         if (this == obj) {
            return true;
         }
         if (obj == null || obj.getClass() != ConvertiblePair.class) {
            return false;
         }
         ConvertiblePair other = (ConvertiblePair) obj;
         return this.sourceType.equals(other.sourceType) && this.targetType.equals(other.targetType);
      }

      @Override
      public int hashCode() {
         return this.sourceType.hashCode() * 31 + this.targetType.hashCode();
      }

      @Override
      public String toString() {
         return this.sourceType.getName() + " -> " + this.targetType.getName();
      }
   }

}
```

## 2 .TypeConverter（类型转化器）

```java
 /**
  *  定义类型转换方法的接口。通常（但不一定）与PropertyEditorRegistry接口一起实现
  *  通常接口TypeConverter的实现是基于非线程安全的PropertyEditors类，因此也不是线程安全的
  *  @since 2.0
  */
public interface TypeConverter {

   /**
    * 将参数中的value转换成requiredType类型
    * 从String到任何类型的转换,通常使用PropertyEditor类的setAsText方法或ConversionService中的Spring Converter
    */
   <T> T convertIfNecessary(Object value, Class<T> requiredType) throws TypeMismatchException;

   /**
    * 意义同上，增加了作为转换目标的方法参数，主要用于分析泛型类型，可能是null
    */
   <T> T convertIfNecessary(Object value, Class<T> requiredType, MethodParameter methodParam)
         throws TypeMismatchException;

   /**
    *意义同上，增加了转换目标的反射field
    */
   <T> T convertIfNecessary(Object value, Class<T> requiredType, Field field)
         throws TypeMismatchException;

}
```

实现类**TypeConverterSupport**

```java
/**
 * 是TypeConverter基础实现（这个类继承了PropertyEditorRegistrySupport）
 * 主要服务于BeanWrapperImpl类的
 *
 * @author Juergen Hoeller
 * @since 3.2（spring 3.2才开始服务的）
 * @see SimpleTypeConverter
 */
public abstract class TypeConverterSupport extends PropertyEditorRegistrySupport implements TypeConverter {

    //委托给TypeConverterDelegate来转换
   TypeConverterDelegate typeConverterDelegate;


   @Override
   public <T> T convertIfNecessary(Object value, Class<T> requiredType) throws TypeMismatchException {
      return doConvert(value, requiredType, null, null);
   }

   @Override
   public <T> T convertIfNecessary(Object value, Class<T> requiredType, MethodParameter methodParam)
         throws TypeMismatchException {

      return doConvert(value, requiredType, methodParam, null);
   }

   @Override
   public <T> T convertIfNecessary(Object value, Class<T> requiredType, Field field)
         throws TypeMismatchException {

      return doConvert(value, requiredType, null, field);
   }

   private <T> T doConvert(Object value, Class<T> requiredType, MethodParameter methodParam, Field field)
         throws TypeMismatchException {
      try {
         //如果field不为空的情况下，默认使用field，并且委托给内部的typeConverterDelegate去实现
         if (field != null) {
            return this.typeConverterDelegate.convertIfNecessary(value, requiredType, field);
         }
         else {
            return this.typeConverterDelegate.convertIfNecessary(value, requiredType, methodParam);
         }
      }
      catch (ConverterNotFoundException ex) {
         throw new ConversionNotSupportedException(value, requiredType, ex);
      }
      catch (ConversionException ex) {
         throw new TypeMismatchException(value, requiredType, ex);
      }
      catch (IllegalStateException ex) {
         throw new ConversionNotSupportedException(value, requiredType, ex);
      }
      catch (IllegalArgumentException ex) {
         throw new TypeMismatchException(value, requiredType, ex);
      }
   }

}
```

再来看看**TypeConverterDelegate**

所有类型转换的工作都由该类完成，即将属性转换为其他类型，来看下他的重要方法，

这个方法的作用就是**==将赋予的新值转换成property所要的类型==**的方法

```java
/**
 *  将值转换为指定属性所需的类型 (如果需要，从string转),
 * 
 * @param propertyName 属性名称
 * @param oldValue 之前的值（如果可用）（可能是null）
 * @param newValue 拟定的新的值，还不是最终转换后的值
 * @param requiredType 要转换的类型
 * @param typeDescriptor 目标属性或字段的描述符
 * @return 新值，可能是类型转换的结果
 * @throws 如果类型转换失败，则出现IllegalArgumentException
 */
public <T> T convertIfNecessary(String propertyName, Object oldValue, Object newValue,
      Class<T> requiredType, TypeDescriptor typeDescriptor) throws IllegalArgumentException {

   //新值（需要转换的值）
   Object convertedValue = newValue;

   //先去找此类型对应的自定义的属性编辑器
   //找寻方法很简单，先去按照特定的属性名称去找对应的编辑器，如果没有找到，就检查特定于类型的编辑器。
    //没有特定于属性的编辑器 - > 检查特定于类型的编辑器
    //特定类型的编辑器（先通过已类型为key,在自定义的编辑器customEditors里面查找；如果没有找到，再到customEditorCache缓存里面找，如果还是没有；就遍历自定义的编辑器customEditors里面根据类型的父类找，如果还是没找到，那就返回空，如果找到了，那就将这个结果缓存到customEditorCache里面，下次直接就去里面拿了，不用再去找了，很是麻烦）
   PropertyEditor editor = this.propertyEditorRegistry.findCustomEditor(requiredType, propertyName);

   ConversionFailedException firstAttemptEx = null;

   // 没有自定义编辑器但指定了自定义ConversionService？
   ConversionService conversionService = this.propertyEditorRegistry.getConversionService();
    //如果conversionService不等于空，并且有新值，而且新的值有对应的类型
   if (editor == null && conversionService != null && convertedValue != null && typeDescriptor != null) {
      //拿到新值的类型
      TypeDescriptor sourceTypeDesc = TypeDescriptor.forObject(newValue);
      //获取到目标值的类型
      TypeDescriptor targetTypeDesc = typeDescriptor;
      //并且判断是可转的
      if (conversionService.canConvert(sourceTypeDesc, targetTypeDesc)) {
         try {
            //那么就转换，返回一个新的值
            return (T) conversionService.convert(convertedValue, sourceTypeDesc, targetTypeDesc);
         }
         catch (ConversionFailedException ex) {
            // fallback to default conversion logic below
            firstAttemptEx = ex;
         }
      }
   }

   //属性编辑器不等于空，或者（要转换的类型不是空并且新的值不是需要转换的值的子接口或者子类）
   if (editor != null || (requiredType != null && !ClassUtils.isAssignableValue(requiredType, convertedValue))) {
      if (requiredType != null && Collection.class.isAssignableFrom(requiredType) && convertedValue instanceof String) {
         TypeDescriptor elementType = typeDescriptor.getElementTypeDescriptor();
         if (elementType != null && Enum.class.isAssignableFrom(elementType.getType())) {
            convertedValue = StringUtils.commaDelimitedListToStringArray((String) convertedValue);
         }
      }
      if (editor == null) {
         editor = findDefaultEditor(requiredType);
      }
      convertedValue = doConvertValue(oldValue, convertedValue, requiredType, editor);
   }

   boolean standardConversion = false;
 //如果需要转换的类型不是空
   if (requiredType != null) {
      // 在适当的时候，尝试应用一些标准类型转换规则。
      if (convertedValue != null) {
         //如果requiredType是Object，直接返回
         if (Object.class.equals(requiredType)) {
            return (T) convertedValue;
         }
         //如果类型是数组
         if (requiredType.isArray()) {
            //如果新值是string类型，并且requiredType数组里面装的是枚举
            if (convertedValue instanceof String && Enum.class.isAssignableFrom(requiredType.getComponentType())) {
               //则将值通过逗号分隔产生一个string数组
               convertedValue = StringUtils.commaDelimitedListToStringArray((String) convertedValue);
            }
            //最后将值设置进去
            return (T) convertToTypedArray(convertedValue, propertyName, requiredType.getComponentType());
         }
         //如果是集合则转换
         else if (convertedValue instanceof Collection) {
            // Convert elements to target type, if determined.
            convertedValue = convertToTypedCollection(
                  (Collection<?>) convertedValue, propertyName, requiredType, typeDescriptor);
            standardConversion = true;
         }
         //如果是Map则转换
         else if (convertedValue instanceof Map) {
            // Convert keys and values to respective target type, if determined.
            convertedValue = convertToTypedMap(
                  (Map<?, ?>) convertedValue, propertyName, requiredType, typeDescriptor);
            standardConversion = true;
         }
          //如果值是数组，并且数组长度是1，则转换
         if (convertedValue.getClass().isArray() && Array.getLength(convertedValue) == 1) {
            convertedValue = Array.get(convertedValue, 0);
            standardConversion = true;
         }
         //如果转化的类型是string类型，并且新值是原始或者包装类型
         if (String.class.equals(requiredType) && ClassUtils.isPrimitiveOrWrapper(convertedValue.getClass())) {
            // 直接返回
            return (T) convertedValue.toString();
         }
         //如果值是String类型，并且新值不能转化为requiredType类型
         else if (convertedValue instanceof String && !requiredType.isInstance(convertedValue)) {
            if (firstAttemptEx == null && !requiredType.isInterface() && !requiredType.isEnum()) {
               try {
                  Constructor<T> strCtor = requiredType.getConstructor(String.class);
                  return BeanUtils.instantiateClass(strCtor, convertedValue);
               }
               catch (NoSuchMethodException ex) {
                  // proceed with field lookup
                  if (logger.isTraceEnabled()) {
                     logger.trace("No String constructor found on type [" + requiredType.getName() + "]", ex);
                  }
               }
               catch (Exception ex) {
                  if (logger.isDebugEnabled()) {
                     logger.debug("Construction via String failed for type [" + requiredType.getName() + "]", ex);
                  }
               }
            }
            String trimmedValue = ((String) convertedValue).trim();
            if (requiredType.isEnum() && "".equals(trimmedValue)) {
               // It's an empty enum identifier: reset the enum value to null.
               return null;
            }
            convertedValue = attemptToConvertStringToEnum(requiredType, trimmedValue, convertedValue);
            standardConversion = true;
         }
      }
      else {
         // convertedValue == null
         if (javaUtilOptionalEmpty != null && requiredType.equals(javaUtilOptionalEmpty.getClass())) {
            convertedValue = javaUtilOptionalEmpty;
         }
      }
      //convertedValue不能够转换成requiredType类型
      if (!ClassUtils.isAssignableValue(requiredType, convertedValue)) {
         if (firstAttemptEx != null) {
            throw firstAttemptEx;
         }
         // Definitely doesn't match: throw IllegalArgumentException/IllegalStateException
         StringBuilder msg = new StringBuilder();
         msg.append("Cannot convert value of type [").append(ClassUtils.getDescriptiveType(newValue));
         msg.append("] to required type [").append(ClassUtils.getQualifiedName(requiredType)).append("]");
         if (propertyName != null) {
            msg.append(" for property '").append(propertyName).append("'");
         }
         if (editor != null) {
            msg.append(": PropertyEditor [").append(editor.getClass().getName()).append(
                  "] returned inappropriate value of type [").append(
                  ClassUtils.getDescriptiveType(convertedValue)).append("]");
            throw new IllegalArgumentException(msg.toString());
         }
         else {
            msg.append(": no matching editors or conversion strategy found");
            throw new IllegalStateException(msg.toString());
         }
      }
   }
   //如果没有异常抛出
   if (firstAttemptEx != null) {
      //如果没有找到属性编辑器并且不能够转换，并且需要转换的类型不能为空，并且，需要转换的类型不是Object类型，则抛错，否则debug
      if (editor == null && !standardConversion && requiredType != null && !Object.class.equals(requiredType)) {
         throw firstAttemptEx;
      }
      logger.debug("Original ConversionService attempt failed - ignored since " +
            "PropertyEditor based conversion eventually succeeded", firstAttemptEx);
   }
   //则直接强转
   return (T) convertedValue;
}
```

总结下这里，其实可以用两句话来概括

先去找合适的propertyEditorRegistry的注册器中找PropertyEditor，再去找合适的convertionService(在==PropertyEditorRegistrySupport==propertyEditorRegistry的实现)，如果找到convertionService，并且没有找到PropertyEditor，那么尝试用convertionService去解析，如果一旦解析失败，那么就用一些spring定制的标准类型转换规则来转换，如果还发现转换不成功，就只能抛出异常了，如果是springMVC环境，就抛到BindingResult中去，如果是spring就直接丢给容器

## 3. spring中有哪些方法可以注册PropertyEditor和convertionService呢？

### 3.1  ConversionServiceFactoryBean

```xml
<bean id="conversionService" class="org.springframework.context.support.ConversionServiceFactoryBean"/>
```

需要这样子配置，并且名字一定为conversionService，为啥？

> ```
> 因为在spring刷新容器的时候，在初始化bean的时候调用finishBeanFactoryInitialization(beanFactory)的时候要调用
> ConfigurableBeanFactory.setConversionService(ConversionService conversionService)
> 方法，给abstractBeanFactory设置一个conversionService，在调用的时候，源码是这样写的
> ```

![](image\12.png)

判断容器中有没有一个叫做conversionService名字的bean,如果有，就实例化并且设置给BeanFactory

```
String CONVERSION_SERVICE_BEAN_NAME = "conversionService";
```

那什么时候设置进去beanWrapper的呢？在这里

![](image\13.png)

也就是创建bean的时候生成tBeanWrapper，并且initBeanWrapper的时候，通过容器中的conversionService来设置进入beanWrapper，然后在beanWrapper类型与值映射的时候派上用场的



```java
public class ConversionServiceFactoryBean implements FactoryBean<ConversionService>, InitializingBean {

   //请注意下，为啥这里是个？问好，继续往下看
   private Set<?> converters;

   private GenericConversionService conversionService;
    
   ....
       
   public void setConverters(Set<?> converters) {
	    this.converters = converters;
   }

	@Override
	public void afterPropertiesSet() {
		this.conversionService = createConversionService();
		ConversionServiceFactory.registerConverters(this.converters, this.conversionService);
	}
    
    protected GenericConversionService createConversionService() {
		return new DefaultConversionService();
	}
}
```

可以看到，ConversionServiceFactoryBean实现了InitializingBean接口，在实例化阶段就会向conversionService里面注入一些spring默认的ConversionService，大概有30多个，如果要实现自己的conversionService，那么你只需要这样配置就可以了

```xml
<bean id="conversionService" class="org.springframework.context.support.ConversionServiceFactoryBean">
   <property name="converters">
      <set>
         <bean class="com.best.MyConverter"
              p:datePattern="yyyy-MM-dd"></bean>
      </set>
   </property>
</bean>
```



```java
public class MyConverter implements Converter<String,Date> {

    private String datePattern;

    public void setDatePattern(String datePattern) {
        this.datePattern = datePattern;
    }

    // Converter<S,T>接口的类型转换方法
    @Override
    public Date convert(String date) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat(this.datePattern);
            // 将日期字符串转换成Date类型返回
            return dateFormat.parse(date);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("日期转换失败!");
            return null;
        }

    }
}
```

ConverterFactory怎么注入？上面

> ConversionServiceFactoryBean
>
> ​            private Set<?> converters; 
>
>  这个问号仅仅只能注入Converter呢？当然不是，看看解析源码

```java
@Override
public void afterPropertiesSet() {
   this.conversionService = createConversionService();
    //在这里通过ConversionServiceFactory来想ConverterRegistry来向
   // GenericConversionService conversionService;注入的
   ConversionServiceFactory.registerConverters(this.converters, this.conversionService);
}
```

注入的逻辑，可以注入三种方式的converter

​      Converter                  1-1的转换

​     ConverterFactory      1-n 的转换

​    GenericConverter       n-n的转换

```java
public static void registerConverters(Set<?> converters, ConverterRegistry registry) {
   if (converters != null) {
      for (Object converter : converters) {
         if (converter instanceof GenericConverter) {
            registry.addConverter((GenericConverter) converter);
         }
         else if (converter instanceof Converter<?, ?>) {
            registry.addConverter((Converter<?, ?>) converter);
         }
         else if (converter instanceof ConverterFactory<?, ?>) {
            registry.addConverterFactory((ConverterFactory<?, ?>) converter);
         }
         else {
            throw new IllegalArgumentException("Each converter object must implement one of the " +
                  "Converter, ConverterFactory, or GenericConverter interfaces");
         }
      }
   }
}
```

### 3.2 FormattingConversionServiceFactoryBean

除了ConversionServiceFactoryBean，还提供了FormattingConversionServiceFactoryBean，用来注册转换器

那么这两者有什么不同呢？

![14](image\14.png)

在Web项目中，通常需要将数据转换为具有某种格式的字符串进行展示，因此Spring3引入了格式化转换器（Formatter SPI） 和格式化服务API（FormattingConversionService）从而支持这种需求。现在来看看格式转换器相关的接口定义：

#### 3.2.1 Formatter SPI

​    

```java
/**
 * Formats objects of type T.
 * A Formatter is both a Printer <i>and</i> a Parser for an object type.
 *
 * @author Keith Donald
 * @since 3.0
 * @param <T> the type of object this Formatter formats
 */
public interface Formatter<T> extends Printer<T>, Parser<T> {

}
```

可以看到是从spring3.0开始的，Formatter又继承了两个接口Printer（翻译过来就是打印器）和Parser（解析器）

```java
public interface Printer<T> {

   String print(T object, Locale locale);

}
```

```java
public interface Parser<T> {

   T parse(String text, Locale locale) throws ParseException;

}
```

可以看到这两个接口的方法正好相反，一个是打印的，一个是解析的。

那么Formatter 又是怎么工作的？

==FormattingConversionService==内部有两个内部类

```java
private static class PrinterConverter implements GenericConverter {

   private final Class<?> fieldType;

   private final TypeDescriptor printerObjectType;

    ...
}
```


```java

private static class ParserConverter implements GenericConverter {
        //需要打印的类型
        private final Class<?> fieldType;
        //Converter委托给parser进行解析
		private final Parser<?> parser;

		private final ConversionService conversionService;

		public ParserConverter(Class<?> fieldType, Parser<?> parser, ConversionService conversionService) {
			this.fieldType = fieldType;
			this.parser = parser;
			this.conversionService = conversionService;
		}

		@Override
		public Set<ConvertiblePair> getConvertibleTypes() {
			return Collections.singleton(new ConvertiblePair(String.class, this.fieldType));
		}

		@Override
		public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
			String text = (String) source;
			if (!StringUtils.hasText(text)) {
				return null;
			}
			Object result;
			try {
                //可以看到，在调用convert方法的时候，内部也用的parser转换的
				result = this.parser.parse(text, LocaleContextHolder.getLocale());
			}
			catch (ParseException ex) {
				throw new IllegalArgumentException("Unable to parse '" + text + "'", ex);
			}
			if (result == null) {
				throw new IllegalStateException("Parsers are not allowed to return null");
			}
			TypeDescriptor resultType = TypeDescriptor.valueOf(result.getClass());
			if (!resultType.isAssignableTo(targetType)) {
				result = this.conversionService.convert(result, resultType, targetType);
			}
			return result;
		}

}
```

#### 3.2.2 FormattingConversionService体系结构

![](image\14.png)

看看FormattingConversionService继承体系，他间接继承了ConversionService，那么肯定就有ConversionService的一系列方法；同时他实现了FormatterRegistry方法，方法有一处实现是这样的，

```java
@Override
public void addFormatterForFieldType(Class<?> fieldType, Formatter<?> formatter) {
   addConverter(new PrinterConverter(fieldType, formatter, this));
   addConverter(new ParserConverter(fieldType, formatter, this));
}
```

他会将formatter转换成内部类的Converter来使用，==说来说去，其实还是用的spring Conversion体系==

#### 3.2.2 注解实现（DateTimeFormat）

```java
package org.springframework.format.annotation;
/*
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0
 * @see org.joda.time.format.DateTimeFormat
 */
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface DateTimeFormat {

   String style() default "SS";

   ISO iso() default ISO.NONE;

   String pattern() default "";


   public enum ISO {

      DATE,

      TIME,

      DATE_TIME,

     
      NONE
   }

}
```

想必大家都用过这个注解，他就是将string类型的转换成date类型的注解；Spring为了支持注解类型的parse和print定义了AnnotationFormatterFactory接口

```java
package org.springframework.format;

import java.lang.annotation.Annotation;
import java.util.Set;

/**
 * 注解格式化工厂
 * @author Keith Donald
 *
 *<p>For example, a {@code DateTimeFormatAnnotationFormatterFactory} might create a          formatter
 * that formats {@code Date} values set on fields annotated with {@code @DateTimeFormat}.
 */
public interface AnnotationFormatterFactory<A extends Annotation> {

   /**
    * 可以使用注释的字段类型
    */
   Set<Class<?>> getFieldTypes();

   /**
    * 获取打印机以打印带注释的{@code fieldType}字段的值
    */
   Printer<?> getPrinter(A annotation, Class<?> fieldType);

  /**
    * 获取解析器以解析{@code fieldType}字段的已提交值
    */
   Parser<?> getParser(A annotation, Class<?> fieldType);

}
```

看看Spring内部用的DateTimeFormat的Factory是如何实现的

```java
public class DateTimeFormatAnnotationFormatterFactory  extends EmbeddedValueResolutionSupport
      implements AnnotationFormatterFactory<DateTimeFormat> {


   private static final Set<Class<?>> FIELD_TYPES;
  //只支持Date，Calendar，Long这三种类型加上注解
   static {
      Set<Class<?>> fieldTypes = new HashSet<Class<?>>(4);
      fieldTypes.add(Date.class);
      fieldTypes.add(Calendar.class);
      fieldTypes.add(Long.class);
      FIELD_TYPES = Collections.unmodifiableSet(fieldTypes);
   }


   @Override
   public Set<Class<?>> getFieldTypes() {
      return FIELD_TYPES;
   }

   @Override
   public Printer<?> getPrinter(DateTimeFormat annotation, Class<?> fieldType) {
      return getFormatter(annotation, fieldType);
   }

   @Override
   public Parser<?> getParser(DateTimeFormat annotation, Class<?> fieldType) {
      return getFormatter(annotation, fieldType);
   }

   protected Formatter<Date> getFormatter(DateTimeFormat annotation, Class<?> fieldType) {
      DateFormatter formatter = new DateFormatter();
      formatter.setStylePattern(resolveEmbeddedValue(annotation.style()));
      formatter.setIso(annotation.iso());
      formatter.setPattern(resolveEmbeddedValue(annotation.pattern()));
      return formatter;
   }

}
```





而对于AnnotationFormatterFactory类型，FormattingConversionService又是怎么实现的

==AnnotationParserConverter==

```java
//它继承了ConditionalGenericConverter，表示有条件的类型转换
private class AnnotationParserConverter implements ConditionalGenericConverter {
   
   private Class<? extends Annotation> annotationType;

   private AnnotationFormatterFactory annotationFormatterFactory;

   private Class<?> fieldType;

   public AnnotationParserConverter(Class<? extends Annotation> annotationType,
         AnnotationFormatterFactory<?> annotationFormatterFactory, Class<?> fieldType) {
      this.annotationType = annotationType;
      this.annotationFormatterFactory = annotationFormatterFactory;
      this.fieldType = fieldType;
   }

   public Set<ConvertiblePair> getConvertibleTypes() {
      return Collections.singleton(new ConvertiblePair(String.class, fieldType));
   }
   
   public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
      return targetType.hasAnnotation(annotationType);
   }

   @SuppressWarnings("unchecked")
   public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
      AnnotationConverterKey converterKey =
            new AnnotationConverterKey(targetType.getAnnotation(annotationType), targetType.getObjectType());
      //converter缓存中拿
      GenericConverter converter缓存中拿 = cachedParsers.get(converterKey);
       //如果拿不到就放生成一个ParserConverter来解析，并且放入到缓存，不用每次都生成一个ParserConverter
      if (converter == null) {
         
         Parser<?> parser = annotationFormatterFactory.getParser(
               converterKey.getAnnotation(), converterKey.getFieldType());
         converter = new ParserConverter(fieldType, parser, FormattingConversionService.this);
         cachedParsers.put(converterKey, converter);
      }
      //调用ParserConverter的convert方法通过fomatter的parse进行转换
      return converter.convert(source, sourceType, targetType);
   }

   public String toString() {
      return String.class.getName() + " -> @" + annotationType.getName() + " " +
            fieldType.getName() + ": " + annotationFormatterFactory;
   }
}
```

最终的结果是**==Formatting体系会转换成Conversion体系进行转换==**

## 4.自定义PropertyEditor

### 4.1 配置CustomEditorConfigurer

上面说了convertionService体系的东西，现在说下自定义的PropertyEditor体系的东西

通过spring注入自己定义的PropertyEditor可以这样配置

![](image\15.png)

```java
public class DateEditor extends PropertyEditorSupport {

    private static final String CROSSBAR = "-";

    private static final String COLON = ":";

    private static final String PLUS = "+";

    /**
     * 数字检验
     */
    private static final String DIGITAL_VERIFICATION = "^\\d+$";

    private static DateTimeFormatter dateFormaterHolder = DateTimeFormat.forPattern("yyyy-MM-dd");

    private static DateTimeFormatter timeFormaterHolder = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");


    /**
     * 入参String转换为Date 可接受：[yyyy-MM-dd、yyyy-MM-dd HH:mm:ss、毫秒数，以及原生日期类型]
     */
    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        if (StringUtils.isEmpty(text)) {
            setValue(null);
        } else {
            if (text.contains(CROSSBAR)) {
                if (text.contains(COLON)) {
                    setValue(DateTime.parse(text, timeFormaterHolder).toDate());
                } else {
                    setValue(DateTime.parse(text, dateFormaterHolder).toDate());
                }
            } else if (text.matches(DIGITAL_VERIFICATION)) {
                setValue(new Date(Long.valueOf(text)));
            } else if (text.contains(PLUS)) {
                setValue(new Date(text));
            } else {
                throw new IllegalArgumentException("可接受时间格式[yyyy-MM-dd、yyyy-MM-dd HH:mm:ss、毫秒数],时间格式异常,异常数据:" + text);
            }
        }
    }
}
```

```java
public class DatePropertyEditorRegistrar implements PropertyEditorRegistrar {

    @Override
    public void registerCustomEditors(PropertyEditorRegistry registry) {
        registry.registerCustomEditor(Date.class,new DateEditor());
    }
}
```

CustomEditorConfigurer这个类实现了BeanFactoryPostProcessor接口，会在容器准备好之后对容器进行一些初始化操作

```java
//BeanFactoryPostProcessor方法的postProcessBeanFactory
@Override
public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
    //会将注入的PropertyEditorRegistrar，customEditor向容器中的PropertyEditorRegistrars和customEditors添加注入的东西
   if (this.propertyEditorRegistrars != null) {
      for (PropertyEditorRegistrar propertyEditorRegistrar : this.propertyEditorRegistrars) {
         beanFactory.addPropertyEditorRegistrar(propertyEditorRegistrar);
      }
   }
   if (this.customEditors != null) {
      for (Map.Entry<Class<?>, Class<? extends PropertyEditor>> entry : this.customEditors.entrySet()) {
         Class<?> requiredType = entry.getKey();
         Class<? extends PropertyEditor> propertyEditorClass = entry.getValue();
         beanFactory.registerCustomEditor(requiredType, propertyEditorClass);
      }
   }
}
```

在initBeanWrapper的时候才将这些PropertyEditorRegistrars和customEditors注入到PropertyEditorRegistry中使用的

```java
protected void initBeanWrapper(BeanWrapper bw) {
   //之前讲的将容器中的ConversionService加入到BeanWrapper中取得
   bw.setConversionService(getConversionService());
    //下面就是加入自定义的CustomEditor
   registerCustomEditors(bw);
}
```

```java
protected void registerCustomEditors(PropertyEditorRegistry registry) {
   PropertyEditorRegistrySupport registrySupport =
         (registry instanceof PropertyEditorRegistrySupport ? (PropertyEditorRegistrySupport) registry : null);
   if (registrySupport != null) {
      registrySupport.useConfigValueEditors();
   }
   //将容器中的propertyEditorRegistrars加入到PropertyEditorRegistry中
   if (!this.propertyEditorRegistrars.isEmpty()) {
      for (PropertyEditorRegistrar registrar : this.propertyEditorRegistrars) {
         try {
            registrar.registerCustomEditors(registry);
         }
         catch (BeanCreationException ex) {
            Throwable rootCause = ex.getMostSpecificCause();
            if (rootCause instanceof BeanCurrentlyInCreationException) {
               BeanCreationException bce = (BeanCreationException) rootCause;
               if (isCurrentlyInCreation(bce.getBeanName())) {
                  if (logger.isDebugEnabled()) {
                     logger.debug("PropertyEditorRegistrar [" + registrar.getClass().getName() +
                           "] failed because it tried to obtain currently created bean '" +
                           ex.getBeanName() + "': " + ex.getMessage());
                  }
                  onSuppressedException(ex);
                  continue;
               }
            }
            throw ex;
         }
      }
   }
    //将容器中的customEditors加入到PropertyEditorRegistry中
   if (!this.customEditors.isEmpty()) {
      for (Map.Entry<Class<?>, Class<? extends PropertyEditor>> entry : this.customEditors.entrySet()) {
         Class<?> requiredType = entry.getKey();
         Class<? extends PropertyEditor> editorClass = entry.getValue();
         registry.registerCustomEditor(requiredType, BeanUtils.instantiateClass(editorClass));
      }
   }
}
```

