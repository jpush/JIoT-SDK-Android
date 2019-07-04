package cn.jiguang.iot.mqtt;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * @author : ouyangshengduo
 * e-mail : ouysd@jiguang.cn
 * date  : 2019/5/7 17:01
 * desc : 发布消息失败或者超时回调
 */
public interface PublishCallback {

    /**
     * mqtt发布消息失败或者超时回调
     * @param topic 对应的topic
     * @param message 发布的mqtt消息
     * @param mqttContext mqtt操作上下文
     * @param token 当前mqtt的发布的token
     * @param throwable 异常
     */
    void onFailure(String topic, MqttMessage message,MqttContext mqttContext,IMqttDeliveryToken token ,Throwable throwable);
}
