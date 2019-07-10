package com.best.controller.advice;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class ExceptionAdvice {

    @ExceptionHandler(value = Exception.class)
    @ResponseBody
    public Map exception1(Exception exception){
        Map<String,Object> map = new HashMap<>();
        map.put("success",false);

        //map.put("error",bindingResult.getAllErrors().get(0).getDefaultMessage());
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
