package com.gnxrt.ticketgoapi.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.data.redis.ssl.enabled:false}")
    private boolean sslEnabled;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost);
        config.setPort(redisPort);
        if (redisPassword != null && !redisPassword.isEmpty()) {
            config.setPassword(redisPassword);
        }
        if (sslEnabled) {
            LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                    .useSsl().disablePeerVerification().build();
            return new LettuceConnectionFactory(config, clientConfig);
        }
        return new LettuceConnectionFactory(config);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();
        String scheme = sslEnabled ? "rediss" : "redis";
        String address = String.format("%s://%s:%d", scheme, redisHost, redisPort);

        if (redisPassword != null && !redisPassword.isEmpty()) {
            config.useSingleServer()
                    .setAddress(address)
                    .setPassword(redisPassword)
                    .setConnectionMinimumIdleSize(1)
                    .setConnectionPoolSize(10)
                    .setRetryAttempts(3)
                    .setRetryInterval(1500);
        } else {
            config.useSingleServer()
                    .setAddress(address)
                    .setConnectionMinimumIdleSize(1)
                    .setConnectionPoolSize(10)
                    .setRetryAttempts(3)
                    .setRetryInterval(1500);
        }

        return Redisson.create(config);
    }
}