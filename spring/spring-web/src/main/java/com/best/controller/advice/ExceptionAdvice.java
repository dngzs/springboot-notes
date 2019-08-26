package com.best.controller.advice;

import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.net.BindException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@ControllerAdvice
public class ExceptionAdvice {

    @ExceptionHandler(value = BindException.class)
    @ResponseBody
    public Map exception2(BindException ex, BindingResult bindingResult){
        Map<String,Object> map = new HashMap<>();
        map.put("success",false);
        map.put("error",bindingResult.getAllErrors().get(0).getDefaultMessage());
        return map;
    }

    /**
     * 处理@RequestParam校验不通过异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseBody
    public Map validationError(ConstraintViolationException ex) {
        Set<ConstraintViolation<?>> violations = ex.getConstraintViolations();
        StringBuilder builder = new StringBuilder();
        for (ConstraintViolation<?> item : violations) {
            builder.append(item.getMessage());
        }
        Map<String,Object> map = new HashMap<>();
        map.put("success",false);
        map.put("error",builder.toString());
        return map;
    }

    @ExceptionHandler(value = MaxUploadSizeExceededException.class)
    @ResponseBody
    public Map maxUploadSizeExceededException(MaxUploadSizeExceededException exception){
        Map<String,Object> map = new HashMap<>();
        map.put("success",false);

        map.put("error","当前文件大小"+exception.getMaxUploadSize()+"字节");
        return map;
    }




}
