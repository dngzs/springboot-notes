package reflect;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * @author dngzs
 * @date 2019-12-17 11:26
 */
public class TestReflect {

    public static void test(TestReflect p0,
                            List<String> p1,
                            Map<String, TestReflect> p2,
                            List<String>[] p3,
                            Map<String, TestReflect>[] p4,
                            List<? extends TestReflect> p5,
                            Map<? extends TestReflect, ? super TestReflect> p6
                            //T p7
    ) {
    }


    public static void main(String[] args) {
        Method[] methods = TestReflect.class.getMethods();
        for (Method method : methods) {
            if(method.getName().contains("test")){
                Type[] genericParameterTypes = method.getGenericParameterTypes();

                //po
                Class poClass = (Class)genericParameterTypes[0];
                System.out.println(poClass);

                System.out.println("----------------------------------------------------");
                //p1
                ParameterizedType p1Type = (ParameterizedType)genericParameterTypes[1];
                Type type = p1Type.getActualTypeArguments()[0];
                System.out.println(type);
                Type ownerType = p1Type.getOwnerType();
                System.out.println(ownerType);
                Type rawType = p1Type.getRawType();
                System.out.println(rawType);

                System.out.println("----------------------------------------------------");


                ParameterizedType p2Type = (ParameterizedType)genericParameterTypes[2];
                Type[] actualTypeArguments = p2Type.getActualTypeArguments();
                for (Type actualTypeArgument : actualTypeArguments) {
                    System.out.println(actualTypeArgument);
                }
                Type ownerType1 = p2Type.getOwnerType();
                System.out.println(ownerType1);

                Type rawType1 = p2Type.getRawType();
                System.out.println(rawType1);
            }
        }
    }
}
