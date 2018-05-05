package com.example.demo.queue;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.data.redis.core.RedisTemplate;

import com.example.demo.utils.LocalIpUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
/**
 * Created by machine on 2017/10/22.
 */
public class EventQueue<T> {
	
    public static final long DEFAULT_AWAIT_IN_MILLIS = 500;
    
    private RedisTemplate<String, T> queueRedis;
    
    private String pendingQueueName;
    private String processingQueueName;
    private String failedQueueName;

    private int processingErrorRetryCount;

    private long awaitInMillis = DEFAULT_AWAIT_IN_MILLIS;

    private Lock lock = new ReentrantLock();
    private Condition notEmpty = lock.newCondition();

    public EventQueue(String queueName, RedisTemplate<String, T> queueRedis, int processingErrorRetryCount) {
    	this.queueRedis = queueRedis;
    	this.pendingQueueName = queueName + "_pending";
        this.processingQueueName = queueName + "_processing_" + LocalIpUtils.getLocalHostIP();
        this.failedQueueName = queueName + "_failed";
        this.processingErrorRetryCount = processingErrorRetryCount;
    }

    public T next() {
        while (true) {
            T obj = null;
            try {
                // 从等待 Queue POP，然后 PUSH 到本地处理队列
                obj = (T) queueRedis.opsForList().rightPopAndLeftPush(pendingQueueName, processingQueueName);
            } catch (Exception e) {
                // 发生了网络异常后警告，然后人工或定期检测
                // 将本地任务队列长时间未消费的任务推送回等待队列
                continue;
            }

            //4. 返回获取的任务
            if (obj != null) {
                return obj;
            }
            lock.lock();
            try {
                notEmpty.await(awaitInMillis, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                //ignore
            } finally {
                lock.unlock();
            }
        }
    }

    public void success(T obj) {
        queueRedis.opsForList().remove(processingQueueName, 0, obj);
    }

    public void fail(T obj) {
        String failCountKey = getFailCountName(obj);
        final int failedCount = queueRedis.opsForValue().increment(failCountKey, 1).intValue();
        //如果小于等于重试次数，则直接添加到等待队列队尾
        if (failedCount <= processingErrorRetryCount) {
            queueRedis.opsForList().leftPush(pendingQueueName, obj);
            queueRedis.opsForList().remove(processingQueueName, 0, obj);
        } else {
            queueRedis.opsForList().leftPush(failedQueueName, obj);
            queueRedis.opsForList().remove(processingQueueName, 0, obj);
            queueRedis.delete(failCountKey);
        }
    }

    //接收其他系统推送的任务
    public void enqueueToBack(T obj) {
        queueRedis.opsForList().leftPush(pendingQueueName, obj);
    }

    private String getFailCountName(T obj) {
        String md5 = null;
        try {
            md5 = DigestUtils.md5Hex(new ObjectMapper().writeValueAsString(obj));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return new StringBuffer("failed:").append(md5).append(":count").toString();
    }
    
	public RedisTemplate<String, T> getQueueRedis() {
		return queueRedis;
	}

	public void setQueueRedis(RedisTemplate<String, T> queueRedis) {
		this.queueRedis = queueRedis;
	}

	public String getQueueName() {
		return pendingQueueName;
	}

	public void setQueueName(String queueName) {
		this.pendingQueueName = queueName;
	}

	public String getProcessingQueueName() {
		return processingQueueName;
	}

	public void setProcessingQueueName(String processingQueueName) {
		this.processingQueueName = processingQueueName;
	}

	public String getFailedQueueName() {
		return failedQueueName;
	}

	public void setFailedQueueName(String failedQueueName) {
		this.failedQueueName = failedQueueName;
	}

	public int getProcessingErrorRetryCount() {
		return processingErrorRetryCount;
	}

	public void setProcessingErrorRetryCount(int processingErrorRetryCount) {
		this.processingErrorRetryCount = processingErrorRetryCount;
	}

	public long getAwaitInMillis() {
		return awaitInMillis;
	}

	public void setAwaitInMillis(long awaitInMillis) {
		this.awaitInMillis = awaitInMillis;
	}
}
