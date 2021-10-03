package com.we.javaapi.rocketmq.batch;


import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;


import java.util.ArrayList;
import java.util.List;

/**
 * @author BigRedCaps
 * @date 2021/10/3 10:07
 */
public class BatchProducer
{
    public static void main (String[] args) throws Exception
    {
        DefaultMQProducer producer = new DefaultMQProducer("pg");
        producer.setNamesrvAddr("rocketmqOS:9876");
        // 指定要发送的消息的最大大小，默认是4M
        // 不过，仅修改该属性是不行的，还需要同时修改broker加载的配置文件中的maxMessageSize属性
        producer.setMaxMessageSize(4*1024*1024);
        producer.start();

        // 定义要发送的消息集合
        List<Message> messages = new ArrayList<>();
        for (int i = 0; i < 100; i++)
        {
            byte[] body = ("Hi," + i).getBytes();
            Message msg = new Message("someTopic", "someTag", body);
            messages.add(msg);
        }
        // 定义消息列表分割器，将消息列表分割为多个不超过4M大小的小列表
        MessageListSplitter splitter = new MessageListSplitter(messages);
        while(splitter.hasNext()){
            try{
                List<Message> listItem = splitter.next();
                producer.send(listItem);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        producer.shutdown();
    }
}
