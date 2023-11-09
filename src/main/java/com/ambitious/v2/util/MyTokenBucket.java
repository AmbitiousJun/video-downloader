package com.ambitious.v2.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 自定义的令牌桶对象
 * @author Ambitious
 * @date 2023/10/24
 */
public class MyTokenBucket {

    private final Logger logger = LoggerFactory.getLogger(MyTokenBucket.class);

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

    /**
     * 存放总消耗令牌数，用于计算实际下载速率，每秒钟清空一次
     */
    private final AtomicLong totalConsume;

    /**
     * 每次最多消耗掉 8KB 的令牌
     */
    private static final Integer MAX_CONSUME = 8 * 1024;

    @SuppressWarnings("all")
    public MyTokenBucket(int refillRate) {
        this.capacity = (long) refillRate;
        this.refillRate = refillRate;
        this.tokens = 0L;
        this.lastRefillTime = System.currentTimeMillis();
        this.totalConsume = new AtomicLong(0L);
        // 启动定时器，监控速率
        new Thread(() -> {
            long lastCalcTime = System.currentTimeMillis();
            totalConsume.set(0L);
            BigDecimal unit = BigDecimal.valueOf(1024);
            BigDecimal milliUnit = BigDecimal.valueOf(1000);
            String lastRateStr = "";
            while (true) {
                try {
                    long currentTime = System.currentTimeMillis();
                    long milli = currentTime - lastCalcTime;
                    BigDecimal consume = BigDecimal.valueOf(totalConsume.get());
                    BigDecimal milliSec = BigDecimal.valueOf(milli);
                    // 计算出 MB/s 单位速率
                    BigDecimal rate = consume.multiply(milliUnit)
                                             .divide(unit, 2, RoundingMode.HALF_UP)
                                             .divide(unit, 2, RoundingMode.HALF_UP)
                                             .divide(milliSec, 2, RoundingMode.HALF_UP);
                    String rateStr = rate.setScale(1, RoundingMode.HALF_UP).toString();
                    if (!lastRateStr.equals(rateStr)) {
                        LogUtils.warning(logger, String.format("当前下载速率：%s MB/s", rateStr));
                        lastRateStr = rateStr;
                        // 清空状态
                        totalConsume.set(0L);
                        lastCalcTime = currentTime;
                    }
                } catch (Exception ignore) {} finally {
                    SleepUtils.sleep(3000);
                }
            }
        }, "t-rate-limit").start();
    }

    /**
     * 下载完成后通知令牌桶，用于计算下载速率
     * @param consume 下载的字节数
     */
    public void completeConsume(long consume) {
        totalConsume.addAndGet(consume);
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
        long consume = Math.min(Math.min(this.tokens, request), MAX_CONSUME);
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
