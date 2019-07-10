package com.best.converter;

import org.springframework.core.convert.converter.Converter;

import java.text.SimpleDateFormat;
import java.util.Date;

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
