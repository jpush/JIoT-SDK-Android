package cn.jiguang.iot.mqtt;

import android.content.Context;
import android.support.annotation.NonNull;

import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttSuback;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttWireMessage;

import cn.jiguang.iot.bean.DeviceInfo;
import cn.jiguang.iot.util.JiotConstant;
import cn.jiguang.iot.util.JiotLogger;

/**
 * @author : ouyangshengduo
 * e-mail : ouysd@jiguang.cn
 * date  : 2019/5/7 17:21
 * desc : mqtt操作类
 */
public class MqttConnection implements MqttCallback {

    /**设备三元组信息**/
    private DeviceInfo deviceInfo;
    /**mqtt连接操作的回调**/
    private AbstractMqttHandleCallback mqttHandleCallback;
    /**mqtt客户端**/
    private MqttAsyncClient mqttClient = null;
    /**mqtt需要的心跳定时器对象**/
    private JiotAlarmPingSender alarmPingSender;
    /**服务器uri**/
    private String serverURI;
    /**mqtt客户端的clientId**/
    private String clientId;
    /**应用上下文**/
    private Context context;
    /**消息id**/
    private static int INVALID_MESSAGE_ID = -1;
    /**上一条消息的非法id**/
    private int mLastReceivedMessageId = INVALID_MESSAGE_ID;
    /**重连次数**/
    private int mqttReconnectTimes = 0;
    /**mqtt重连定时任务**/
    private JiotAlarmMqttReconnect alarmMqttReconnect;
    /**mqtt连接参数**/
    private MqttConnectOptions options;
    /**mqtt客户端连接状态**/
    private volatile MqttConstans.MQTT_CONNECT_STATUS clientConnectStatus = MqttConstans.MQTT_CONNECT_STATUS.CONNECT_IDLE;
    /**publish消息池分发器**/
    private MqttMessageDispather mqttMessageDispather;
    /** 断连状态下buffer缓冲区，当连接重新建立成功后自动将buffer中数据写出**/
    private DisconnectedBufferOptions bufferOpts = null;
    /**QOS值非法**/
    private static final int INVALID_QOS_VALUE = 128;
    /**mqtt重连次数**/
    private static final int MAX_MQTT_RECONNECT_COUNT = 3;
    /**mqtt消息发布消息首次超时时间为10 秒**/
    private static final int MQTT_PUBLISH_TIME_OUT = 10 * 1000;
    /**mqtt协议栈心跳时间间隔 300秒**/
    private static final int MQTT_KEEP_ALIVE_INTERVAL = 300;
    /**mqtt连接操作超时时间 10秒**/
    private static final int MQTT_CONNECT_TIME_OUT = 10;

    /**标志是否已经调用客户端的release**/
    private volatile boolean hasReleased = false;
    /**网络异常标识字符串**/
    private static final String EXCEPTION_TYPE_STRING = "SocketTimeoutException";

    public void setHasReleased(boolean hasReleased) {
        this.hasReleased = hasReleased;
    }

    /**
     * 获取mqtt的客户端的Id
     * @return 返回当前客户端的clientId
     */
    String getClientId() {
        return clientId;
    }

    /**
     * 获取设备的三元组信息
     * @return 返回设备的信息
     */
    public DeviceInfo getDeviceInfo(){
        return deviceInfo;
    }

    /**
     * mqtt操作类的构造方法
     * @param context 应用上下文
     * @param mqttHandleCallback mqtt连接的状态回调
     */
    public MqttConnection(Context context, AbstractMqttHandleCallback mqttHandleCallback){
        this.context = context;
        this.mqttHandleCallback = mqttHandleCallback;
        this.mqttMessageDispather = new MqttMessageDispather();
    }

    /**
     * 释放定时服务资源
     */
    public void releaseAlarm(){
        if(null != alarmPingSender){
            alarmPingSender.stop();
        }
        if(null != alarmMqttReconnect){
            alarmMqttReconnect.stop();
        }
    }

    /**
     * 关闭当前连接
     */
    public void close(){

        if(null != mqttClient){
            try {
                mqttClient.close();
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 连接mqtt
     * @param mqttContext mqtt操作上下文
     * @param mqttServer mqtt服务器地址
     * @param deviceInfo 设备三元组
     * @param protocolType 协议类型 有ssl tcp wss ws等方式
     */
    public synchronized void connect(@NonNull final MqttContext mqttContext, @NonNull final String mqttServer, @NonNull DeviceInfo deviceInfo,int protocolType){
        if(protocolType == JiotConstant.SIS_PROTOCOL_TYPE_SSL) {
            this.serverURI = "ssl://" + mqttServer;
        }else{
            this.serverURI = "tcp://" + mqttServer;
        }
        this.deviceInfo = deviceInfo;
        this.clientId = deviceInfo.getProductKey() + "." + deviceInfo.getDeviceName();


        if(null != mqttClient && mqttClient.isConnected()){
            return ;
        }
        IMqttActionListener mActionListener = new IMqttActionListener() {

            @Override
            public void onSuccess(IMqttToken token) {
                setConnectState(MqttConstans.MQTT_CONNECT_STATUS.CONNECTED);
                mqttHandleCallback.onConnectCompleted(Status.OK, false, token.getUserContext(), "connected to " + serverURI);
            }

            @Override
            public void onFailure(IMqttToken token, Throwable exception) {
                JiotLogger.e( "onFailure! ",exception);
                setConnectState(MqttConstans.MQTT_CONNECT_STATUS.CONNTECT_FALIED);
                if(null != mqttClient){
                    alarmPingSender.stop();
                    try {
                        mqttClient.close();
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                }

                if(exception.toString().contains(EXCEPTION_TYPE_STRING)) {
                    mqttHandleCallback.onConnectCompleted(Status.TIME_OUT, false, token.getUserContext(), exception.toString());
                }else{
                    mqttHandleCallback.onConnectCompleted(Status.ERROR, false, token.getUserContext(), exception.toString());
                }
            }
        };
        bufferOpts = new DisconnectedBufferOptions();
        bufferOpts.setBufferEnabled(true);
        bufferOpts.setBufferSize(1024);
        try {
            alarmPingSender = new JiotAlarmPingSender(context);
            mqttClient = new MqttAsyncClient(serverURI,clientId,null,alarmPingSender );
            mqttClient.setCallback(this);
            mqttClient.setBufferOpts(bufferOpts);
            mqttClient.setManualAcks(false);
        } catch (MqttException e) {
            e.printStackTrace();
            setConnectState(MqttConstans.MQTT_CONNECT_STATUS.CONNTECT_FALIED);
            return;
        }
        JiotLogger.d("mqtt connect server uri : " + serverURI + " and client id : " + clientId);
        options = new MqttConnectOptions();
        options.setAutomaticReconnect(false);
        options.setConnectionTimeout(MQTT_CONNECT_TIME_OUT);
        options.setKeepAliveInterval(MQTT_KEEP_ALIVE_INTERVAL);
        options.setUserName(deviceInfo.getProductKey());
        options.setPassword(deviceInfo.getDeviceSecret().toCharArray());
        if(protocolType == JiotConstant.SIS_PROTOCOL_TYPE_SSL) {
            options.setSocketFactory(MqttSslUtils.getSocketFactory());
        }
        try {
            JiotLogger.d("Start connecting to " + serverURI);
            setConnectState(MqttConstans.MQTT_CONNECT_STATUS.CONNECTING);
            mqttClient.connect(options,mqttContext,mActionListener);
        } catch (MqttException e) {
            JiotLogger.e("MqttClient connect failed", e);
            setConnectState(MqttConstans.MQTT_CONNECT_STATUS.CONNTECT_FALIED);
        }
    }


    /**
     * 重新连接, 结果通过回调函数通知。
     */
    public synchronized void reconnect() {
        if (mqttClient == null) {
            JiotLogger.e("Reconnect mqttClient = null. Will not do reconnect");
            return;
        }
        if (getConnectStatus().equals(MqttConstans.MQTT_CONNECT_STATUS.CONNECTING)) {
            JiotLogger.d("The client is connecting. Reconnect return directly.");
            return;
        }
        if(null != mqttHandleCallback){
            mqttHandleCallback.onStartReConnecting();
        }
        final IMqttActionListener listenerReconnect = new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken token) {
                setConnectState(MqttConstans.MQTT_CONNECT_STATUS.CONNECTED);
                mqttReconnectTimes = 0;
                if(null != alarmMqttReconnect) {
                    alarmMqttReconnect.stop();
                }
                mqttHandleCallback.onConnectCompleted(Status.OK, false, token.getUserContext(), "connected to " + serverURI);
            }
            @Override
            public void onFailure(IMqttToken token, Throwable exception) {
                JiotLogger.e( "onFailure! ",exception);
                setConnectState(MqttConstans.MQTT_CONNECT_STATUS.CONNTECT_FALIED);
                if(mqttReconnectTimes == 0){
                    if(null != alarmMqttReconnect) {
                        alarmMqttReconnect.stop();
                        alarmMqttReconnect.start();
                        mqttReconnectTimes++;
                    }
                }else if(mqttReconnectTimes < MAX_MQTT_RECONNECT_COUNT){
                    int delayTime = (int) Math.pow(2,mqttReconnectTimes) * 5;
                    mqttReconnectTimes ++;
                    alarmMqttReconnect.scheduleMqttReconnect(delayTime * 1000);
                }else{
                    mqttReconnectTimes = 0;
                    alarmMqttReconnect.stop();
                    mqttHandleCallback.onConnectCompleted(Status.ERROR, true, token.getUserContext(), exception.toString());
                }
            }
        };
        IMqttActionListener listenerDisconnect = new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                setConnectState(MqttConstans.MQTT_CONNECT_STATUS.DISCONNECTED);
                try {
                    alarmMqttReconnect = new JiotAlarmMqttReconnect(context);
                    alarmMqttReconnect.init(mqttClient,options,listenerReconnect);
                    if(null != mqttHandleCallback){
                        mqttHandleCallback.onDisconnectCompleted(Status.OK, asyncActionToken.getUserContext(), "disconnected to " + serverURI);
                    }
                    if(!hasReleased) {
                        if(null != mqttHandleCallback){
                            mqttHandleCallback.onStartReConnecting();
                        }
                        setConnectState(MqttConstans.MQTT_CONNECT_STATUS.CONNECTING);
                        JiotLogger.d("The mqtt client is reconnecting .");
                        mqttClient.connect(options, null, listenerReconnect);
                    }
                } catch (Exception e) {
                    JiotLogger.e("Exception occurred attempting to reconnect: ", e);
                    setConnectState(MqttConstans.MQTT_CONNECT_STATUS.CONNTECT_FALIED);
                }
            }
            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable cause) {
                mqttHandleCallback.onDisconnectCompleted(Status.ERROR, asyncActionToken.getUserContext(), cause.toString());
            }
        };
        if(getConnectStatus().equals(MqttConstans.MQTT_CONNECT_STATUS.CONNECTED)){
            try {
                mqttClient.disconnect(0,null,listenerDisconnect);
            } catch (MqttException ignored) {
                mqttHandleCallback.onDisconnectCompleted(Status.OK, null, "disconnected to " + serverURI);
            }
        }else {
            if(!hasReleased) {
                alarmMqttReconnect = new JiotAlarmMqttReconnect(context);
                alarmMqttReconnect.init(mqttClient, options, listenerReconnect);
                try {
                    setConnectState(MqttConstans.MQTT_CONNECT_STATUS.CONNECTING);
                    JiotLogger.d("The mqtt client is reconnecting .");
                    mqttClient.connect(options, null, listenerReconnect);
                } catch (MqttException ignored) { }
            }
        }
    }


    /**
     * mqtt客户端订阅主题
     * @param mqttContext mqtt操作上下文
     * @param topics topic组
     */
    public void subscribe(@NonNull MqttContext mqttContext, @NonNull final String []topics) {
        for (String topic : topics) {
            if (topic == null || topic.trim().length() == 0) {
                JiotLogger.e("Topic is empty!!!");
                return;
            }
            if (topic.length() > MqttConstans.MAX_SIZE_OF_STRING_TOPIC) {
                JiotLogger.e("Topic length is too long!!!");
                return;
            }
        }

        int []qoss = new int[topics.length];
        for(int i = 0 ; i < topics.length ; i ++){
            qoss[i] = MqttConstans.QOS1;
        }

        if ((mqttClient != null) && (mqttClient.isConnected())) {
            try {
                mqttClient.subscribe(topics,qoss,mqttContext, new SelfMqttActionListener((MqttConstans.SUBSCRIBE)));
            } catch (Exception e) {
                JiotLogger.e("subscribe topics",e);
            }
        } else {
            JiotLogger.e("subscribe topics failed, because mMqttClient not connected.");
        }
    }

    /**
     * 发布消息
     * @param mqttContext mqtt操作的上下文
     * @param topic topic
     * @param message mqtt消息内容
     */
    public void publish(@NonNull MqttContext mqttContext,@NonNull String topic, @NonNull MqttMessage message) {
        if (topic.trim().length() == 0) {
            JiotLogger.e("Topic is empty!!!");
            return;
        }
        if (topic.length() > MqttConstans.MAX_SIZE_OF_STRING_TOPIC) {
            JiotLogger.e( "Topic length is too long!!!");
            return;
        }

        JiotLogger.i("Starting publish topic: " + topic + " Message: " + message.toString());

        if ((mqttClient != null) && (mqttClient.isConnected())) {
            mqttMessageDispather.enqueue(new AsyncPublish(topic, message, mqttContext, new PublishCallback() {
                @Override
                public void onFailure(String topic, MqttMessage message, MqttContext mqttContext, IMqttDeliveryToken token, Throwable throwable) {
                    if(null != mqttHandleCallback){
                        mqttHandleCallback.onPublishCompleted(Status.TIME_OUT,token,mqttContext,throwable.getMessage());
                    }
                }
            }));
        } else if ((mqttClient != null) && (this.bufferOpts != null) && (this.bufferOpts.isBufferEnabled())) {
            //放入缓存
            mqttMessageDispather.enqueue(new AsyncPublish(topic, message, mqttContext, new PublishCallback() {
                @Override
                public void onFailure(String topic, MqttMessage message, MqttContext mqttContext, IMqttDeliveryToken token, Throwable throwable) {
                    if(null != mqttHandleCallback){
                        mqttHandleCallback.onPublishCompleted(Status.TIME_OUT,token,mqttContext,throwable.getMessage());
                    }
                }
            }));
        } else {
            JiotLogger.e("publish topic: " + topic + " failed, mMqttClient not connected and disconnect buffer not enough.");
        }
    }

    /**发布消息线程**/
    final class AsyncPublish implements Runnable{

        private IMqttDeliveryToken sendToken = null;
        private PublishCallback callback;
        private String topic;
        private MqttMessage message;
        private MqttContext mqttContext;
        private AsyncPublish that;
        int tryTimes;
        /**publish重试最大次数**/
        private static final int MAX_MQTT_PUBLISH_TRY_TIMES = 2;
        AsyncPublish(String topic, MqttMessage message, MqttContext mqttContext, PublishCallback callback){
            this.topic = topic;
            this.message = message;
            this.mqttContext = mqttContext;
            this.callback = callback;
            this.that = this;
            this.tryTimes = 0;
        }

        @Override
        public void run() {

            while(tryTimes < MAX_MQTT_PUBLISH_TRY_TIMES) {
                try {
                    doPublishAction(tryTimes);
                    return;
                } catch (MqttException e) {
                    if (tryTimes == 0) {
                        JiotLogger.d("The mqtt client publish message timeout first time,and try again.");
                        //第一次超时
                        tryTimes++;
                    } else {
                        JiotLogger.d("The mqtt client publish message timeout second time,callback to out.");
                        if (null != callback) {
                            callback.onFailure(topic, message, mqttContext, sendToken, e);
                        }
                        if (null != mqttMessageDispather) {
                            mqttMessageDispather.pause(this);
                        }
                        break;
                    }
                }
            }
        }
        void doPublishAction(int times) throws MqttException {
            sendToken =  mqttClient.publish(topic, message, mqttContext, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    if(null != mqttHandleCallback){
                        mqttHandleCallback.onPublishCompleted(Status.OK, asyncActionToken, asyncActionToken.getUserContext(), MqttConstans.PUBLISH_SUCCESS);
                    }
                    if(null != mqttMessageDispather){
                        mqttMessageDispather.finished(that);
                    }
                }
                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    if(null != mqttHandleCallback){
                        mqttHandleCallback.onPublishCompleted(Status.ERROR, asyncActionToken, asyncActionToken.getUserContext(), exception.toString());
                    }
                }
            });
            sendToken.waitForCompletion((times + 1) * MQTT_PUBLISH_TIME_OUT);
        }
    }

    /**
     * MQTT断连，结果通过回调函数通知。
     * @param mqttContext 用户上下文（这个参数在回调函数时透传给用户）
     */
    public void disConnect(MqttContext mqttContext) {

        mLastReceivedMessageId = INVALID_MESSAGE_ID;

        IMqttActionListener mActionListener = new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                setConnectState(MqttConstans.MQTT_CONNECT_STATUS.DISCONNECTED);
                mqttHandleCallback.onDisconnectCompleted(Status.OK, asyncActionToken.getUserContext(), "disconnected to " + serverURI);
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable cause) {
                mqttHandleCallback.onDisconnectCompleted(Status.ERROR, asyncActionToken.getUserContext(), cause.toString());
            }
        };
        if(null != mqttClient) {
            try {
                mqttClient.disconnect(0,mqttContext, mActionListener);
            } catch (MqttException ignored) {
                mqttHandleCallback.onDisconnectCompleted(Status.OK, mqttContext, "disconnected to " + serverURI);
            }
        }else{
            mqttHandleCallback.onDisconnectCompleted(Status.OK, mqttContext, "disconnected to " + serverURI);
        }
    }

    /**
     * 设置当前连接状态
     * @param connectStatus 当前连接状态
     */
    private synchronized void setConnectState(MqttConstans.MQTT_CONNECT_STATUS connectStatus) {
        this.clientConnectStatus = connectStatus;
    }

    /**
     * @return 当前连接状态
     */
    private MqttConstans.MQTT_CONNECT_STATUS getConnectStatus() {
        return this.clientConnectStatus;
    }


    @Override
    public void connectionLost(Throwable cause) {
        JiotLogger.e("connection lost because of: "+ cause.toString());
        setConnectState(MqttConstans.MQTT_CONNECT_STATUS.DISCONNECTED);
        if(null != mqttHandleCallback){
            mqttHandleCallback.onConnectionLost(cause);
        }
        mLastReceivedMessageId = INVALID_MESSAGE_ID;
    }

    @Override
    public void messageArrived(String topic, MqttMessage message){


        if (message.getQos() > 0 && message.getId() == mLastReceivedMessageId) {
            JiotLogger.e("Received topic: " + topic + ", seq_no: " + message.getId() + ", message: " + message + ", discard repeated message!!!");
            return;
        }
        mLastReceivedMessageId = message.getId();


        if (mqttHandleCallback != null) {
            mqttHandleCallback.onMessageReceived(topic, message);
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
    }


    /**
     * 事件回调
     */
    private class SelfMqttActionListener implements IMqttActionListener {
        private int command;

        SelfMqttActionListener(int command) {
            this.command = command;
        }

        @Override
        public void onSuccess(IMqttToken token) {

            MqttWireMessage mqttWireMessage = token.getResponse();

            switch (command) {
                case MqttConstans.PUBLISH:

                    break;

                case MqttConstans.SUBSCRIBE:
                    int[] qos = ((MqttSuback) mqttWireMessage).getGrantedQos();
                    if (null != qos && qos.length >= 1 && qos[0] == INVALID_QOS_VALUE) {
                        mqttHandleCallback.onSubscribeCompleted(Status.ERROR, token, token.getUserContext(), MqttConstans.SUBSCRIBE_FAIL);
                    } else {
                        mqttHandleCallback.onSubscribeCompleted(Status.OK, token, token.getUserContext(), MqttConstans.SUBSCRIBE_SUCCESS);
                    }
                    break;

                case MqttConstans.UNSUBSCRIBE:
                    mqttHandleCallback.onUnSubscribeCompleted(Status.OK, token, token.getUserContext(), MqttConstans.UNSUBSCRIBE_SUCCESS);
                    break;

                default:
                    JiotLogger.e("Unknown message on Success:" + token);
                    break;
            }
        }

        @Override
        public void onFailure(IMqttToken token, Throwable exception) {
            switch (command) {
                case MqttConstans.PUBLISH:

                    break;
                case MqttConstans.SUBSCRIBE:
                    mqttHandleCallback.onSubscribeCompleted(Status.ERROR, token, token.getUserContext(), exception.toString());
                    break;
                case MqttConstans.UNSUBSCRIBE:
                    mqttHandleCallback.onUnSubscribeCompleted(Status.ERROR, token, token.getUserContext(), exception.toString());
                    break;
                default:
                    JiotLogger.e("Unknown message on onFailure:" + token);
                    break;
            }
        }
    }
}
