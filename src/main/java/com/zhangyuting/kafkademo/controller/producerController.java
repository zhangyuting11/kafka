package com.zhangyuting.kafkademo.controller;

import cn.hutool.core.util.RandomUtil;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Description //TODO
 * @Date 2023/9/4 9:16
 * @Author zhangyuting
 **/
@RequestMapping(value = "producer")
@RestController
public class producerController {

    @GetMapping(value = "test")
    public String test(@RequestParam(value = "random", required = false, defaultValue = "16") Integer random) {
        Assert.isTrue(random <= 100, "不支持100以上的值");
        return RandomUtil.randomString(random);
    }

    @KafkaListener(topics = {"kafka_demo"}, groupId = "0", containerFactory = "kafkaListenerContainerFactory")
    public void kafkaListener(ConsumerRecord<?, ?> record) {
        try {
            System.out.println("这是消费者在消费消息：" + record.topic() + "----" + record.partition() + "----" + record.value());
        } catch (Exception e) {
            System.out.println(String.format("kafka监听消息失败:%s-%s", record.topic(), record.value()));

        }
    }
}
