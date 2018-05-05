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
public class StringProducer {

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StringProducer.class);
	
	
    private int i;

    @Autowired
    private EventQueue<String> eventQueue;

    @Scheduled(fixedDelay = 1000)
    public void send() {
        String str = "num-" + (++i);
        eventQueue.enqueueToBack(str);
        log.info("开始生产-{}", str);
    }

}
