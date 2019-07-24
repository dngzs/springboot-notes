package hashmap;

import java.math.BigDecimal;
import java.text.NumberFormat;

/**
 * @author dngzs
 * @date 2019-07-05 18:42
 */
public class NumberFormatDemo {

    public static void main(String[] args) {
        Double rate =0.03;
        NumberFormat num = NumberFormat.getPercentInstance();
        String rates = num.format(rate);
        System.out.println(rates);


        Double a = 0.3;
        BigDecimal bigDecimal1 = new BigDecimal(a);
        BigDecimal bigDecimal2 = new BigDecimal(a.toString());
        System.out.println(bigDecimal1);
        System.out.println(bigDecimal2);
    }
}
