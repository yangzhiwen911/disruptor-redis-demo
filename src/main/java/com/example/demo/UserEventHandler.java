package com.example.demo;

import java.util.concurrent.TimeUnit;

import com.example.demo.handler.Event;
import com.example.demo.handler.EventHandler;
import com.example.demo.queue.EventQueue;

public class UserEventHandler implements EventHandler<User> {

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UserEventHandler.class);
	
	@Override
	public void onEvent(EventQueue<User> eventQueue, Event<User> event) {
		log.info("开始处理-{}", event.getObj());
		try {
			TimeUnit.SECONDS.sleep(5);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
