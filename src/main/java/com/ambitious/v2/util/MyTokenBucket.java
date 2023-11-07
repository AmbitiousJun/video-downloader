package com.ambitious.v2.util;

/**
 * 自定义的令牌桶对象
 * @author Ambitious
 * @date 2023/10/24
 */
public class MyTokenBucket {


    /**
     * 桶容量
     */
    private final Long capacity;

    /**
     * 当前桶中含有的令牌数
     */
    private Long tokens;

    /**
     * 每秒补充多少令牌（一个令牌表示 1 Byte）
     */
    private final Integer refillRate;

    /**
     * 最后一次补充令牌的时间
     */
    private Long lastRefillTime;

    public MyTokenBucket(int refillRate) {
        this.capacity = (long) refillRate;
        this.refillRate = refillRate;
        this.tokens = 0L;
        this.lastRefillTime = System.currentTimeMillis();
    }

    /**
     * 消耗一定数量的令牌
     * @param request 要消耗的令牌数
     * @return 消耗掉的令牌数
     */
    public synchronized long tryConsume(long request) {
        // 1 补充令牌
        refillTokens();
        // 2 计算出当前能够消耗的令牌数
        long consume = Math.min(this.tokens, request);
        this.tokens -= consume;
        return consume;
    }

    /**
     * 补充 token
     */
    @SuppressWarnings("all")
    private void refillTokens() {
        // 1 获取当前的时间
        long curTime = System.currentTimeMillis();
        long sub = curTime - lastRefillTime;
        // 2 补充相应的令牌
        this.tokens = Math.min(this.capacity, this.tokens + sub * refillRate / 1000);
        // 3 更新时间
        lastRefillTime = curTime;
    }
}
