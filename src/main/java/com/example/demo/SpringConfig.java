package com.example.demo;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.example.demo.handler.EventHandler;
import com.example.demo.queue.EventQueue;
import com.example.demo.task.EventWorker;


@Configuration
public class SpringConfig {
	
    @Bean(name="eventQueueUser")
    public EventQueue<User> eventQueueUser(RedisTemplate<String, User> redisTemplate) {
        EventQueue<User> eventQueue = new EventQueue<>("user", redisTemplate, 2);
        return eventQueue;
    }
    
    @Bean(name="eventQueueString")
    public EventQueue<String> eventQueueString(RedisTemplate<String, String> redisTemplate) {
        EventQueue<String> eventQueue = new EventQueue<>("string", redisTemplate, 2);
        return eventQueue;
    }

    @Bean(name="eventHandlerString")
    public EventHandler<String> eventHandlerString() {
    	EventHandler<String> eventHandler = new StringEventHandler();
        return eventHandler;
    }

    @Bean(name="eventHandlerUser")
    public EventHandler<User> eventHandlerUser() {
    	EventHandler<User> eventHandler = new UserEventHandler();
        return eventHandler;
    }
    
    
    @Bean(initMethod="init",destroyMethod="stop")
    public EventWorker eventWorker(EventQueue<User> eventQueueUser, EventQueue<String> eventQueueString, EventHandler<String> eventHandlerString, EventHandler<User> eventHandlerUser ) {
        EventWorker eventWorker = new EventWorker();
        Map<EventQueue<?>, EventHandler<?>> eventHandlerMap = new HashMap<>();
        eventHandlerMap.put(eventQueueUser, eventHandlerUser);
        eventHandlerMap.put(eventQueueString, eventHandlerString);
        eventWorker.setEventHandlerMap(eventHandlerMap);
        eventWorker.setRingBufferSize(64);
        eventWorker.setThreadPoolSize(1);
        return eventWorker;
    }

    @Bean
    public RedisTemplate<String, ?> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, ?> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(factory);
        redisTemplate.setValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class));
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }
	
}
