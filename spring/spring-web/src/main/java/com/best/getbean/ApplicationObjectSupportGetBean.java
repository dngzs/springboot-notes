package com.best.getbean;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.stereotype.Component;

/**
 * 获取bean的第三种方式
 *
 * @author dngzs
 * @date 2019-05-17 15:08
 */
@Component
public class ApplicationObjectSupportGetBean extends ApplicationObjectSupport {

   public void getBean(){
       ApplicationContext applicationContext = this.getApplicationContext();
       System.out.println(applicationContext);
   }


}
