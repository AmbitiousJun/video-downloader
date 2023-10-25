package com.ambitious.v2.util;

import cn.hutool.core.util.IdUtil;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 自定义的令牌桶对象
 * @author Ambitious
 * @date 2023/10/24
 */
public class MyTokenBucket {


    /**
     * 桶容量
     */
    private final Integer capacity;

    /**
     * 当前桶中含有的令牌数
     */
    private final AtomicInteger tokens;

    /**
     * 每秒补充多少令牌（一个令牌一个字节）
     */
    private final Integer refillRate;

    @SuppressWarnings("all")
    public MyTokenBucket(int refillRate) {
        this.capacity = Integer.MAX_VALUE;
        this.refillRate = refillRate;
        this.tokens = new AtomicInteger(0);

        // 创建一个专门用于生成令牌的线程，每秒生成一次
        new Thread(() -> {
            while (true) {
                SleepUtils.sleep(1000);
                refillTokens();
            }
        }, "t-token-bucket").start();
    }

    /**
     * 消耗一定数量的令牌
     * @param request 要消耗的令牌数
     * @return 消耗掉的令牌数
     */
    public int tryConsume(int request) {
        for (;;) {
            int currentTokens = this.tokens.get();
            int consume = Math.min(currentTokens, request);
            if (this.tokens.compareAndSet(currentTokens, currentTokens - consume)) {
                // CAS 更新成功
                return consume;
            }
        }
    }

    /**
     * 补充一秒 token
     */
    private void refillTokens() {
        long next = this.tokens.get() + refillRate;
        this.tokens.set(next > capacity ? capacity : Long.valueOf(next).intValue());
    }
}
