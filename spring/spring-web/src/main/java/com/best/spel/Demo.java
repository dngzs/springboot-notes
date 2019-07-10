package com.best.spel;

import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * @author dngzs
 * @date 2019-06-14 10:34
 */
public class Demo {

    public static void main(String[] args) {
        ExpressionParser parser = new SpelExpressionParser();

        String helloWorld =(String)parser.parseExpression("'Hello World'").getValue();
        System.out.println(helloWorld);

        double avogadrosNumber =(Double)parser.parseExpression("6.0221415E+23").getValue();
        System.out.println(avogadrosNumber);
    }
}
