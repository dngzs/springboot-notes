package com.best.controller;

import com.best.exception.ResponseStatusException;
import com.best.service.UserService;
import com.best.vo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

@Controller
@SessionAttributes(value = "name")
@Validated
public class Index implements EnvironmentAware {

    private Environment environment;

    @Autowired
    private UserService userService;

    @RequestMapping("/")
    @ResponseBody
    public List<User> index(User user, ModelMap map, HttpServletRequest request) {
        List<User> users = userService.test1();
        HttpSession session = request.getSession();
        user.setUsername("芳芳");
        map.addAttribute("name", 1);
        //exception();
        return users;
    }


    @RequestMapping("/test")
    @ResponseBody
    public void test(User user, ModelMap map, HttpServletRequest request) {

    }


    public void exception() {
        throw new ResponseStatusException();
    }

    @RequestMapping("/lang")
    @ResponseBody
    public Long genet(@NotNull(message = "id不能为空") Long id) {
        System.out.println();
        return id;
    }

    @InitBinder
    public void InitBinder(WebDataBinder webDataBinder){
        webDataBinder.registerCustomEditor(Date.class,new DateEditor());
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

}
