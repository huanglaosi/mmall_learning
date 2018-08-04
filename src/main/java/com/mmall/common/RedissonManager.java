package com.mmall.common;

import com.mmall.util.PropertiesUtil;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.jackson.map.SerializerFactory;
import org.redisson.Redisson;
import org.redisson.config.Config;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
@Slf4j
public class RedissonManager {

    private Config config = new Config();

    private Redisson redisson = null;

    private static String redisIp1 = PropertiesUtil.getProperty("redis1.ip");
    private static Integer redisPort1 = Integer.parseInt(PropertiesUtil.getProperty("redis1.port"));

    private static String redisIp2 = PropertiesUtil.getProperty("redis2.ip");
    private static Integer redisPort2 = Integer.parseInt(PropertiesUtil.getProperty("redis2.port"));

    @PostConstruct
    private void init(){
        try {
            config.useSingleServer().setAddress(new StringBuilder().append(redisIp1).append(":").append(redisPort1).toString());

            redisson = (Redisson) Redisson.create(config);

            log.info("初始化Redisson结束");
        } catch (Exception e) {
            log.info("redisson init error",e);
            e.printStackTrace();
        }
    }

    public Redisson getRedisson() {
        return redisson;
    }
}
