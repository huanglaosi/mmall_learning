package com.mmall.task;

import com.mmall.common.Const;
import com.mmall.service.IOrderService;
import com.mmall.util.PropertiesUtil;
import com.mmall.util.RedisShardedPoolUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;

@Component
@Slf4j
public class CloseOrderTask {

    @Autowired
    private IOrderService iOrderService;

    @PreDestroy
    public void delLock(){
        RedisShardedPoolUtil.del(Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK);
    }

//    @Scheduled(cron = "0 */1 * * * ?")//每一分钟(每一分钟的整数倍)
    public void closeOrderTaskV1(){
        log.info("关闭订单定时任务启动");
        int hour = Integer.parseInt(PropertiesUtil.getProperty("close.order.task.time.hour=2","2"));
//        iOrderService.closeOrder(hour);
        log.info("关闭订单定时任务关闭");

    }


//    @Scheduled(cron = "0 */1 * * * ?")//每一分钟(每一分钟的整数倍)
    public void closeOrderTaskV2(){  //无法较好的防止死锁
        log.info("关闭订单定时任务启动");
        long lockTimeout = Long.parseLong(PropertiesUtil.getProperty("lock.timeout","5000"));

        Long setnxResult = RedisShardedPoolUtil.setnx(Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK,String.valueOf(System.currentTimeMillis()+lockTimeout));
        if(setnxResult != null && setnxResult.intValue() == 1){
            //如果返回值是1，代表设置成功，获取锁
            closeOrder(Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK);

        }else{
            log.info("没有获取分布式锁:{}",Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK);
        }

        log.info("关闭订单定时任务关闭");
    }

    @Scheduled(cron = "0 */1 * * * ?")//每一分钟(每一分钟的整数倍)
    public void closeOrderTaskV3(){  //无法较好的防止死锁
        log.info("关闭订单定时任务启动");
        long lockTimeout = Long.parseLong(PropertiesUtil.getProperty("lock.timeout","5000"));

        Long setnxResult = RedisShardedPoolUtil.setnx(Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK,String.valueOf(System.currentTimeMillis()+lockTimeout));
        if(setnxResult != null && setnxResult.intValue() == 1){
            closeOrder(Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK);
        }else {
            //因为锁已经存在，未获取到锁，继续判断，判断时间戳，看是否可以获取到锁
            String lockValueStr = RedisShardedPoolUtil.get(Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK);
            //这种情况属于已经设置锁，但还未设置有效期，tomcat就已经关闭的情况，此时锁就没有过期时间，这就是时间戳的作用
            if(lockValueStr != null && System.currentTimeMillis() > Long.parseLong(lockValueStr)){

                //getSetResult = lockValueStr,重置新值
                String getSetResult = RedisShardedPoolUtil.getSet(Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK,String.valueOf(System.currentTimeMillis()+lockTimeout));
                //再次用当前的时间戳getSet
                //返回给定的key的旧值，->旧值判断，是否可以获取锁
                //当key没有旧值时，即key不存在时，返回nil,->获取锁（这种情况是因为别的进程刚好把key设置没了）
                if (getSetResult == null || (getSetResult != null && StringUtils.equals(lockValueStr,getSetResult))){
                    //真正获取到锁
                    closeOrder(Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK);
                }else {
                    log.info("没有获取分布式锁:{}",Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK);
                }
            }else {
                log.info("没有获取分布式锁:{}",Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK);
            }
        }
        log.info("关闭订单定时任务关闭");
    }

    private void closeOrder(String lockName){
        RedisShardedPoolUtil.setpire(lockName,5);//有效期50秒，防止死锁
        log.info("获取{}，ThreadName:{}",Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK,Thread.currentThread().getName());
        int hour = Integer.parseInt(PropertiesUtil.getProperty("close.order.task.time.hour=2","2"));
//        iOrderService.closeOrder(hour);
        RedisShardedPoolUtil.del(Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK);
        log.info("释放{}，ThreadName:{}",Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK,Thread.currentThread().getName());
        log.info("======================================================");
    }



}
