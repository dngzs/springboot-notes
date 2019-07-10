package com.best.resource;

import com.best.vo.User;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author dngzs
 * @date 2019-06-13 11:08
 */
@Component
public class ResourceBean {

    @Resource
    private User user;
}
