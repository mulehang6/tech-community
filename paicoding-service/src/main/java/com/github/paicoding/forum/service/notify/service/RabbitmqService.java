package com.github.paicoding.forum.service.notify.service;

import com.rabbitmq.client.BuiltinExchangeType;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * @author YiHui
 * 创建于 2022/9/3
 */
public interface RabbitmqService {

    boolean enabled();

    /**
     * 发布消息
     */
    void publishMsg(String exchange,
                    BuiltinExchangeType exchangeType,
                    String toutingKey,
                    String message);


    /**
     * 消费消息
     */
    void consumerMsg(String exchange,
                     String queue,
                     String routingKey) throws IOException, TimeoutException;


    void processConsumerMsg();
}
