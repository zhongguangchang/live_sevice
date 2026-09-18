package com.sky.config;

import com.sky.constant.MqConstant;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置类
 * <p>
 * 本项目用延迟队列实现三类「到点才做」的业务，都基于同一套
 * TTL + 死信队列（DLX）机制：
 * <ul>
 *   <li>订单超时未支付自动取消（15 分钟，固定延迟）</li>
 *   <li>服务完成后 7 天未评价自动好评（7 天，固定延迟）</li>
 *   <li>派单后师傅 5 分钟未接单自动转派（5 分钟，固定延迟）</li>
 * </ul>
 * 另外还有一个「服务前 1 小时提醒」，因为每条订单的预约时间不同、
 * 延迟时长不固定，所以采用「每条消息单独设置 expiration」的方式，
 * 详见 serviceRemindDelayQueue 的注释。
 * <p>
 * 延迟队列的核心思路：声明一个<b>没有消费者</b>的队列，消息投进去以后
 * 就躺在里面等，等 TTL 到期变成死信，被死信交换机转发到真正被监听的
 * 消费队列上，消费者这时才收到消息。整个过程不占用任何业务线程，
 * 也不像定时任务那样空转轮询数据库。
 */
@Configuration
@Slf4j
public class RabbitMQConfiguration {

    /**
     * 消息体用 JSON 序列化，而不是 Java 原生序列化（默认行为）
     * <p>
     * 两个实际好处：
     * 1. 打开 RabbitMQ 管理台能看到明文消息内容，调试和演示都方便；
     *    Java 原生序列化在管理台里是一堆乱码
     * 2. 消息体积更小，而且不和具体类版本强绑定
     * <p>
     * 注意：如果用 Java 原生序列化，消息体必须实现 Serializable；
     * 换成 JSON 之后就没有这个约束了。
     * <p>
     * <b>必须显式配置 ObjectMapper 并注册 JavaTimeModule。</b>
     * 消息体 OrderMessageDTO 里有 LocalDateTime 字段（sendTime / serviceTime），
     * 而 Jackson 默认不认识 java.time 包下的类型，
     * 用 new Jackson2JsonMessageConverter() 直接构造会抛：
     * <pre>
     *   MessageConversionException: Failed to convert Message content
     *   ... InvalidDefinitionException: Java 8 date/time type
     *   java.time.LocalDateTime not supported by default
     * </pre>
     * 这个异常在生产者里被 catch 掉只打日志，流程照常继续，
     * 所以从业务表面看不出来 —— 但消息实际上一条都没发出去。
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        //支持 LocalDateTime / LocalDate / LocalTime
        objectMapper.registerModule(new JavaTimeModule());
        //时间序列化成 ISO 字符串而不是时间戳数组
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    /**
     * 应用启动时主动声明一遍交换机、队列和绑定
     * <p>
     * 为什么要显式做这一步：
     * RabbitAdmin 默认是「等到第一次建立连接时」才去声明的，而项目现在还
     * 没有消费者（@RabbitListener），也没有任何地方用到 RabbitTemplate，
     * 所以连接根本不会被创建，队列也就不可能出现在 RabbitMQ 里。
     * <p>
     * 显式声明有两个好处：
     * 1. 启动时就把拓扑建好，打开 RabbitMQ 管理台就能直接看到，
     *    不用非得等到发第一条消息
     * 2. 配置写错时（比如地址填错、RabbitMQ 没启动、vhost 不存在）
     *    启动阶段就暴露，而不是等到业务跑起来才发现消息发不出去
     * <p>
     * 这里捕获异常只打日志、不让应用启动失败，因为排队期间 RabbitMQ
     * 短暂不可用不应该阻断整个服务；真正发消息时还有重试和确认机制兜底。
     */
    @Bean
    public ApplicationRunner rabbitTopologyDeclarer(AmqpAdmin amqpAdmin) {
        return args -> {
            try {
                amqpAdmin.initialize();
                log.info("RabbitMQ 交换机 / 队列 / 绑定声明完成");
            } catch (Exception e) {
                log.error("RabbitMQ 拓扑声明失败，请检查 RabbitMQ 服务与配置：{}", e.getMessage());
            }
        };
    }

    // ========================================================================
    //  场景一：订单超时未支付自动取消
    //  下单 --15分钟--> 到期检查订单状态 --仍待付款--> 关单 + 回补 Redis 名额
    // ========================================================================

    /**
     * 订单延迟交换机：生产者把「15 分钟后要处理」的消息发到这里
     */
    @Bean
    public DirectExchange orderDelayExchange() {
        return ExchangeBuilder.directExchange(MqConstant.ORDER_DELAY_EXCHANGE).durable(true).build();
    }

    /**
     * 订单延迟队列：<b>没有消费者</b>，消息在这里干等 15 分钟
     * <p>
     * 三个关键参数：
     * <ul>
     *   <li>ttl：消息存活时间，超时即成为死信</li>
     *   <li>deadLetterExchange：死信被转发到哪个交换机</li>
     *   <li>deadLetterRoutingKey：转发时用的路由键</li>
     * </ul>
     */
    @Bean
    public Queue orderDelayQueue() {
        return QueueBuilder.durable(MqConstant.ORDER_DELAY_QUEUE)
                .ttl((int) MqConstant.ORDER_TIMEOUT_TTL)
                .deadLetterExchange(MqConstant.ORDER_DLX_EXCHANGE)
                .deadLetterRoutingKey(MqConstant.ORDER_TIMEOUT_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding orderDelayBinding() {
        return BindingBuilder.bind(orderDelayQueue())
                .to(orderDelayExchange())
                .with(MqConstant.ORDER_DELAY_ROUTING_KEY);
    }

    /**
     * 订单死信交换机
     */
    @Bean
    public DirectExchange orderDlxExchange() {
        return ExchangeBuilder.directExchange(MqConstant.ORDER_DLX_EXCHANGE).durable(true).build();
    }

    /**
     * 订单超时消费队列：这才是真正被 @RabbitListener 监听的队列
     */
    @Bean
    public Queue orderTimeoutQueue() {
        return QueueBuilder.durable(MqConstant.ORDER_TIMEOUT_QUEUE).build();
    }

    @Bean
    public Binding orderTimeoutBinding() {
        return BindingBuilder.bind(orderTimeoutQueue())
                .to(orderDlxExchange())
                .with(MqConstant.ORDER_TIMEOUT_ROUTING_KEY);
    }

    // ========================================================================
    //  场景二：服务完成后 7 天未评价，自动好评并结单
    //  为什么需要：不这么做的话，订单会永远停在「待评价」，统计口径就乱了
    // ========================================================================

    @Bean
    public DirectExchange autoReviewExchange() {
        return ExchangeBuilder.directExchange(MqConstant.AUTO_REVIEW_EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue autoReviewDelayQueue() {
        return QueueBuilder.durable(MqConstant.AUTO_REVIEW_DELAY_QUEUE)
                .ttl((int) MqConstant.AUTO_REVIEW_TTL)
                .deadLetterExchange(MqConstant.AUTO_REVIEW_DLX_EXCHANGE)
                .deadLetterRoutingKey(MqConstant.AUTO_REVIEW_CONSUME_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding autoReviewDelayBinding() {
        return BindingBuilder.bind(autoReviewDelayQueue())
                .to(autoReviewExchange())
                .with(MqConstant.AUTO_REVIEW_ROUTING_KEY);
    }

    @Bean
    public DirectExchange autoReviewDlxExchange() {
        return ExchangeBuilder.directExchange(MqConstant.AUTO_REVIEW_DLX_EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue autoReviewQueue() {
        return QueueBuilder.durable(MqConstant.AUTO_REVIEW_QUEUE).build();
    }

    @Bean
    public Binding autoReviewBinding() {
        return BindingBuilder.bind(autoReviewQueue())
                .to(autoReviewDlxExchange())
                .with(MqConstant.AUTO_REVIEW_CONSUME_ROUTING_KEY);
    }

    // ========================================================================
    //  场景三：派单后师傅 5 分钟未接单，自动转派给其他师傅
    // ========================================================================

    @Bean
    public DirectExchange dispatchDelayExchange() {
        return ExchangeBuilder.directExchange(MqConstant.DISPATCH_DELAY_EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue dispatchDelayQueue() {
        return QueueBuilder.durable(MqConstant.DISPATCH_DELAY_QUEUE)
                .ttl((int) MqConstant.DISPATCH_TIMEOUT_TTL)
                .deadLetterExchange(MqConstant.DISPATCH_DLX_EXCHANGE)
                .deadLetterRoutingKey(MqConstant.DISPATCH_TIMEOUT_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding dispatchDelayBinding() {
        return BindingBuilder.bind(dispatchDelayQueue())
                .to(dispatchDelayExchange())
                .with(MqConstant.DISPATCH_DELAY_ROUTING_KEY);
    }

    @Bean
    public DirectExchange dispatchDlxExchange() {
        return ExchangeBuilder.directExchange(MqConstant.DISPATCH_DLX_EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue dispatchTimeoutQueue() {
        return QueueBuilder.durable(MqConstant.DISPATCH_TIMEOUT_QUEUE).build();
    }

    @Bean
    public Binding dispatchTimeoutBinding() {
        return BindingBuilder.bind(dispatchTimeoutQueue())
                .to(dispatchDlxExchange())
                .with(MqConstant.DISPATCH_TIMEOUT_ROUTING_KEY);
    }

    // ========================================================================
    //  场景四：服务开始前 1 小时提醒用户和师傅
    //
    //  这个场景和上面三个不一样：延迟时长不固定。
    //  用户约的是"明天 9 点"，那 TTL 是 23 小时；约的是"后天 9 点"，
    //  TTL 就是 47 小时。所以不能像上面那样在队列上设固定 TTL，
    //  而是由生产者在每条消息上单独设 expiration。
    //
    //  ⚠ 已知问题（答辩可以主动提，是加分项）：
    //  队列里的消息是"先进先出"排队过期的，只有队头消息过期了才会被
    //  检查。如果队头是一条 TTL 23 小时的消息，后面就算有一条 TTL 1 小时
    //  的消息，也得等队头先过期——这就是「队头阻塞」。
    //
    //  解决方案二选一：
    //    a) 装上 rabbitmq_delayed_message_exchange 插件，改用
    //       x-delayed-message 类型交换机，它内部用时间轮实现，没有队头阻塞
    //    b) 拆成多个固定档位的延迟队列（1小时内 / 1天 / 3天 / 7天），
    //       落到最近的一个档位上，用轻微的时间误差换取实现的简单
    // ========================================================================

    @Bean
    public DirectExchange serviceRemindExchange() {
        return ExchangeBuilder.directExchange(MqConstant.SERVICE_REMIND_EXCHANGE).durable(true).build();
    }

    /**
     * 服务提醒延迟队列：这里<b>故意不设队列级 TTL</b>，
     * 由生产者在消息上设置 expiration，实现每条消息独立的延迟时长
     */
    @Bean
    public Queue serviceRemindDelayQueue() {
        return QueueBuilder.durable(MqConstant.SERVICE_REMIND_DELAY_QUEUE)
                .deadLetterExchange(MqConstant.SERVICE_REMIND_DLX_EXCHANGE)
                .deadLetterRoutingKey(MqConstant.SERVICE_REMIND_CONSUME_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding serviceRemindDelayBinding() {
        return BindingBuilder.bind(serviceRemindDelayQueue())
                .to(serviceRemindExchange())
                .with(MqConstant.SERVICE_REMIND_ROUTING_KEY);
    }

    @Bean
    public DirectExchange serviceRemindDlxExchange() {
        return ExchangeBuilder.directExchange(MqConstant.SERVICE_REMIND_DLX_EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue serviceRemindQueue() {
        return QueueBuilder.durable(MqConstant.SERVICE_REMIND_QUEUE).build();
    }

    @Bean
    public Binding serviceRemindBinding() {
        return BindingBuilder.bind(serviceRemindQueue())
                .to(serviceRemindDlxExchange())
                .with(MqConstant.SERVICE_REMIND_CONSUME_ROUTING_KEY);
    }
}
