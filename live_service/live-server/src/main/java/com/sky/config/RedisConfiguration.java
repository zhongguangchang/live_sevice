package com.sky.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置类
 * <p>
 * 相比原项目做了三处补强：
 * <ol>
 *   <li>原项目只设置了 key 的序列化器，value 用的是默认的 JDK 序列化，
 *       存进 Redis 是二进制乱码，既没法用 redis-cli 排查问题，也没法跨语言读取。
 *       这里补上 JSON 序列化器，Redis 里存的就是可读的 JSON</li>
 *   <li>原项目的 RedisTemplate 是裸类型（没有泛型），这里改成
 *       {@code RedisTemplate<String, Object>}，IDE 能给出类型检查</li>
 *   <li>注册 JavaTimeModule 并关闭时间戳输出，保证实体类里的
 *       LocalDateTime / LocalDate / LocalTime 能正确序列化
 *       （Jackson 默认不支持 java.time，不注册会直接抛异常）</li>
 * </ol>
 * <p>
 * 另外要区分两个模板的用途：
 * <ul>
 *   <li>{@link RedisTemplate}：存对象，走 JSON 序列化</li>
 *   <li>StringRedisTemplate：存纯字符串和跑 Lua 脚本。
 *       <b>执行 Lua 脚本必须用 StringRedisTemplate</b>，因为 Lua 里
 *       拿到的是原始字符串，如果 value 是 JSON 或 JDK 序列化的结果，
 *       tonumber() 会直接失败</li>
 * </ul>
 */
@Configuration
@Slf4j
public class RedisConfiguration {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        log.info("开始创建redis模板对象...");

        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        //设置redis的连接工厂对象
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        //key 用字符串序列化，这样 redis-cli 里能看到可读的 key 名
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        redisTemplate.setKeySerializer(stringSerializer);
        redisTemplate.setHashKeySerializer(stringSerializer);

        //value 用 JSON 序列化
        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer(redisObjectMapper());
        redisTemplate.setValueSerializer(jsonSerializer);
        redisTemplate.setHashValueSerializer(jsonSerializer);

        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    /**
     * 专门给 Redis 用的 ObjectMapper
     * <p>
     * 注意：这里不能直接复用 {@link com.sky.json.JacksonObjectMapper}，
     * 因为它是给 HTTP 接口用的，日期格式被截断到了分钟（yyyy-MM-dd HH:mm），
     * 缓存对象时会把秒丢掉。Redis 这边需要完整精度。
     * <p>
     * activateDefaultTyping 的作用是在 JSON 里额外写入 {@code @class} 字段，
     * 记录对象的真实类型，否则反序列化时只能拿到 LinkedHashMap 而不是实体对象。
     */
    private ObjectMapper redisObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();

        //支持 java.time 包下的类型（LocalDateTime 等）
        objectMapper.registerModule(new JavaTimeModule());
        //时间序列化成 ISO 字符串而不是时间戳数字
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        //让私有字段也参与序列化（实体类通常没有 getter 之外的可写入口）
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);

        //写入类型信息，保证能还原成原来的实体对象
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);

        return objectMapper;
    }
}
