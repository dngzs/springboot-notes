package com.best.controller;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.util.StringUtils;

import java.beans.PropertyEditorSupport;
import java.util.Date;

/**
 * MVC Date与字符串转换
 *
 * @author BG317957 - zhangbo
 */
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