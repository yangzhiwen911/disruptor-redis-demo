package com.example.demo.task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import com.example.demo.handler.Event;
import com.example.demo.handler.EventHandler;
import com.example.demo.handler.EventPublishThread;
import com.example.demo.queue.EventQueue;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.ExceptionHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.WorkHandler;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

/**
 * Created by machine on 2017/10/22.
 */
public class EventWorker {
	
	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EventWorker.class);

    private int ringBufferSize = 1024;
    
    private int threadPoolSize = 8;
    
    private Map<EventQueue<?>, EventHandler<?>> eventHandlerMap;
    
    private Map<String, EventQueue<?>> eventQueueMap;
    
    private Disruptor<Event<?>> disruptor;
    
    private List<EventPublishThread<?>> eventPublishThreads = new ArrayList<>();
    
    private RingBuffer<Event<?>> ringBuffer;
    

    /**
     * 初始化
     */
    public void init() {
        // 创建Disruptor
        disruptor = new Disruptor<>(
                Event::new,
                ringBufferSize,
                Executors.defaultThreadFactory(),
                ProducerType.MULTI,
                new BlockingWaitStrategy()
        );
        
        // 获得ringBuffer
        ringBuffer = disruptor.getRingBuffer();
        
        // 处理异常
        disruptor.setDefaultExceptionHandler(new ExceptionHandler<Event<?>>() {
            public void handleEventException(Throwable throwable, long l, Event<?> event) {
                log.error("handleEventException", throwable);
            }

            public void handleOnStartException(Throwable throwable) {
                log.error("handleOnStartException", throwable);
            }

            public void handleOnShutdownException(Throwable throwable) {
                log.error("handleOnShutdownException", throwable);
            }
        });

        // 创建工作者处理器
        WorkHandler<Event<?>> workHandler = event -> {
            String eventType = event.getEventType();
            // 根据事件类型取出EventQueue
            EventQueue eventQueue = eventQueueMap.get(eventType);
            // 根据EventQueue取出EventHandler
            EventHandler eventHandler = eventHandlerMap.get(eventQueue);
            // EventHanlder处理事件
            try {
            	eventHandler.onEvent(eventQueue, event);
                // 处理成功
                eventQueue.success(event.getObj());
            } catch (Exception e) {
                eventQueue.fail(event.getObj());
            }
        };

        // 创建工作者处理器
        WorkHandler[] workHandlers = new WorkHandler[threadPoolSize];
        for (int i = 0; i < threadPoolSize; i++) {
            workHandlers[i] = workHandler;
        }

        // 告知 Disruptor 由工作者处理器处理
        disruptor.handleEventsWithWorkerPool(workHandlers);
        
        // 启动disruptor
        disruptor.start();

        // 初始化线程
        for (Map.Entry<String, EventQueue<?>> eventQueueEntry : eventQueueMap.entrySet()) {
        	String eventType = eventQueueEntry.getKey();
        	EventQueue<?> eventQueue = eventQueueEntry.getValue();
        	
        	//每个类型创建一个发布者
        	EventPublishThread eventPublishThread = new EventPublishThread(eventType, eventQueue, ringBuffer);
        	eventPublishThreads.add(eventPublishThread);
        	eventPublishThread.start();
		}
        
    }

    public void stop() {
        // 停止发布者线程
        for (EventPublishThread<?> eventPublishThread : eventPublishThreads) {
        	eventPublishThread.shutdown();
		}
        // 停止 Disruptor
        disruptor.shutdown();
    }

	public void setEventHandlerMap(Map<EventQueue<?>, EventHandler<?>> eventHandlerMap) {
		this.eventHandlerMap = eventHandlerMap;
		if(eventHandlerMap != null && !eventHandlerMap.isEmpty()) {
			this.eventQueueMap = new HashMap<>();
			for(Map.Entry<EventQueue<?>, EventHandler<?>> entry :eventHandlerMap.entrySet()) {
				EventQueue<?> eventQueue = entry.getKey();
				this.eventQueueMap.put(eventQueue.getQueueName(), eventQueue);
			}
		}
	}

	public int getRingBufferSize() {
		return ringBufferSize;
	}

	public void setRingBufferSize(int ringBufferSize) {
		this.ringBufferSize = ringBufferSize;
	}

	public int getThreadPoolSize() {
		return threadPoolSize;
	}

	public void setThreadPoolSize(int threadPoolSize) {
		this.threadPoolSize = threadPoolSize;
	}
	
}
