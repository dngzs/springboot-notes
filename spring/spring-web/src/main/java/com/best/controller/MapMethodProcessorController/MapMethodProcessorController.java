package com.best.controller.MapMethodProcessorController;


import com.best.service.UserService;
import com.best.vo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

@Controller
public class MapMethodProcessorController {

    @Autowired
    private UserService userService;


    @RequestMapping("/map1")
    @ResponseBody
    public List<User> mapMethodProcessor(Map map){
        if(map != null){
            map.forEach((key,value)->{
                System.out.println(key+"--------"+value);
            });
        }
        List<User> users = userService.test1();
        return users;
    }
}
