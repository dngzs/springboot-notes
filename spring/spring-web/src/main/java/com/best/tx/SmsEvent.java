package com.best.tx;

import org.springframework.context.ApplicationEvent;

/**
 * @author dngzs
 * @date 2019-06-17 16:13
 */
public class SmsEvent<SmsVo> extends ApplicationEvent {

    public SmsEvent(SmsVo smsVo) {
        super(smsVo);

    }
}
