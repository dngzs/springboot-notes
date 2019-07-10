package hashmap;

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
    }
}
