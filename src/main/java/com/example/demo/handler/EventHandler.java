package com.example.demo.handler;

import com.example.demo.queue.EventQueue;

/**
 * Created by machine on 2017/10/22.
 */
public interface EventHandler<T> {
	
    void onEvent(EventQueue<T> eventQueue, Event<T> event);

}
