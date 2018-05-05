package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.demo.queue.EventQueue;


/**
 * Created by wangmaocheng on 2017/10/25.
 */
@EnableScheduling
@Component
public class UserProducer {

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UserProducer.class);
	
    private int i;

    @Autowired
    private EventQueue<User> eventQueue;

    @Scheduled(fixedDelay = 1000)
    public void send() {
        User user = new User(++i, "name-"+i);
        eventQueue.enqueueToBack(user);
        log.info("开始生产-{}", user);
    }

}
