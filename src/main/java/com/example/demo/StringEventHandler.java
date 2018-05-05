package com.example.demo;

import java.util.concurrent.TimeUnit;

import com.example.demo.handler.Event;
import com.example.demo.handler.EventHandler;
import com.example.demo.queue.EventQueue;

public class StringEventHandler implements EventHandler<String> {

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StringEventHandler.class);
	
	@Override
	public void onEvent(EventQueue<String> eventQueue, Event<String> event) {
		log.info("开始处理-{}", event.getObj());
		try {
			TimeUnit.SECONDS.sleep(5);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
