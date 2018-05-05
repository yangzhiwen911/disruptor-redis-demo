package com.example.demo.handler;

import com.example.demo.queue.EventQueue;
import com.lmax.disruptor.RingBuffer;

/**
 * Created by machine on 2017/10/22.
 */

public class EventPublishThread<T> extends Thread {
    
	private EventQueue<T> eventQueue;
    private RingBuffer<Event<T>> ringBuffer;
    private String eventType;
    private boolean running;

    public EventPublishThread(String eventType,EventQueue<T> eventQueue, RingBuffer<Event<T>> ringBuffer) {
        this.eventQueue = eventQueue;
        this.ringBuffer = ringBuffer;
        this.eventType = eventType;
        this.running = true;
    }

    public void run() {
        while (running) {
            T next = eventQueue.next();
            if (next != null) {            	
                ringBuffer.publishEvent((event,sequence)->{
                	event.setEventType(eventType);
                	event.setObj(next);
                });
            }
        }
    }

    public void shutdown() {
        running = false;
    }
}
