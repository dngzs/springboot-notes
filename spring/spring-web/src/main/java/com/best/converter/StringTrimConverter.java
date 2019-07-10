package com.best.converter;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.convert.converter.Converter;

public class StringTrimConverter implements Converter<String,String> {

    @Override
    public String convert(String source) {
        return StringUtils.isNotBlank(source)? source.trim():null;
    }
}
