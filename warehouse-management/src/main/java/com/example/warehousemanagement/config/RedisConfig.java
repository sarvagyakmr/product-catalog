package com.example.warehousemanagement.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import com.example.warehousemanagement.service.RedisSubscriber;
import redis.embedded.RedisServer;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;

@Configuration
public class RedisConfig {

    private RedisServer redisServer;

    @PostConstruct
    public void startRedis() throws IOException {
        redisServer = new RedisServer(6379);
        try {
            redisServer.start();
        } catch (Exception e) {
            // Redis might already be running
        }
    }

    @PreDestroy
    public void stopRedis() {
        if (redisServer != null && redisServer.isActive()) {
            redisServer.stop();
        }
    }

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory("localhost", 6379);
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    @Bean
    public ChannelTopic allocatedOrdersTopic() {
        return new ChannelTopic("allocated-orders");
    }

    @Bean
    public ChannelTopic cancelledOrdersTopic() {
        return new ChannelTopic("cancelled-orders");
    }

    @Bean
    public ChannelTopic picklistCreateTopic() {
        return new ChannelTopic("picklist-create");
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter messageListenerAdapter,
            @Qualifier("allocatedOrdersTopic") ChannelTopic allocatedOrdersTopic,
            @Qualifier("cancelledOrdersTopic") ChannelTopic cancelledOrdersTopic,
            @Qualifier("picklistCreateTopic") ChannelTopic picklistCreateTopic) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(messageListenerAdapter, allocatedOrdersTopic);
        container.addMessageListener(messageListenerAdapter, cancelledOrdersTopic);
        container.addMessageListener(messageListenerAdapter, picklistCreateTopic);
        return container;
    }

    @Bean
    public MessageListenerAdapter messageListenerAdapter(RedisSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, "onMessage");
    }
}
