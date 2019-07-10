package com.best.converter;

import org.apache.commons.lang3.StringUtils;
import org.springframework.format.Formatter;

import java.text.ParseException;
import java.util.Locale;

public class StringTrimFormatter implements Formatter<String> {

    @Override
    public String parse(String text, Locale locale) throws ParseException {
        return StringUtils.isNotBlank(text)? text.trim():null;
    }

    @Override
    public String print(String object, Locale locale) {
        return object;
    }
}
