import java.math.BigDecimal;

/**
 * @author dngzs
 * @date 2019-09-09 17:45
 */
public class BigDecimalDemo {

    public static void main(String[] args) {

        System.out.println( new BigDecimal(0.99).toString());
        System.out.println( new BigDecimal("0.99").toString());
        System.out.println( BigDecimal.valueOf(0.99).toString());
    }
}
