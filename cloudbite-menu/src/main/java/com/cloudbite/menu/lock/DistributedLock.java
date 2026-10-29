package com.cloudbite.menu.lock;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redisson 分布式锁封装。
 *
 * 与手写 SET NX PX 的区别：
 * 1. 看门狗（watchdog）：leaseTime 传 -1 时，锁默认 30s 过期，后台每 10s 自动续期，
 *    业务执行超过 30s 也不会提前释放；进程宕机则停止续期，锁自然过期，不会死锁。
 * 2. 可重入：同一线程重复加锁不会自己把自己锁死。
 * 3. 解锁带线程校验：只允许持有者释放，避免误删别人的锁。
 */
@Component
public class DistributedLock {
    private final RedissonClient redisson;

    public DistributedLock(RedissonClient redisson) {
        this.redisson = redisson;
    }

    public RLock lock(String key) {
        return redisson.getLock(key);
    }

    /** 尝试加锁，最多等 waitSeconds 秒；leaseTime=-1 表示启用看门狗自动续期 */
    public boolean tryLock(RLock lock, long waitSeconds) {
        try {
            return lock.tryLock(waitSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public void unlock(RLock lock) {
        try {
            if (lock != null && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        } catch (Exception ignored) {
        }
    }
}