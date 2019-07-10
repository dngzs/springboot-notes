package com.best.HandlerMapping;

import com.best.service.UserService;
import com.best.vo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * urlHandlerMapping
 *
 * @author dngzs
 * @date 2019-05-20 17:03
 */
@Controller
@RequestMapping("/product?")
public class UrlHandlerMappingController{

    @Autowired
    private UserService userService;

    @ResponseBody
    public List<User> index(User user, ModelMap map, HttpServletRequest request) {
        List<User> users = userService.test1();
        return users;
    }
}
