package com.best.controller.modelAttribute;

import com.best.vo.User;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class ModelAttributeControllerAdvice {

    @ModelAttribute
    public User modelAttribute( Long name){
        User user1 = new User();
        user1.setId(2L);
        user1.setUsername("爱丽丝");
        return user1;
    }
}
