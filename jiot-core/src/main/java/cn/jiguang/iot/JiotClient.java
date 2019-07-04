package cn.jiguang.iot;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;

import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import cn.jiguang.iot.api.JiotClientApi;
import cn.jiguang.iot.bean.DeviceInfo;
import cn.jiguang.iot.bean.EventReportReq;
import cn.jiguang.iot.bean.EventReportRsp;
import cn.jiguang.iot.bean.JiotResult;
import cn.jiguang.iot.bean.MsgDeliverReq;
import cn.jiguang.iot.bean.Property;
import cn.jiguang.iot.bean.PropertyReportReq;
import cn.jiguang.iot.bean.PropertyReportRsp;
import cn.jiguang.iot.bean.PropertySetReq;
import cn.jiguang.iot.bean.VersionReportRsp;
import cn.jiguang.iot.mqtt.JiotAlarmHeartBeat;
import cn.jiguang.iot.mqtt.MqttConstans;
import cn.jiguang.iot.mqtt.MqttContext;
import cn.jiguang.iot.mqtt.AbstractMqttHandleCallback;
import cn.jiguang.iot.mqtt.Status;
import cn.jiguang.iot.sis.JiotAlarmHttpRetry;
import cn.jiguang.iot.sis.SisContants;
import cn.jiguang.iot.sis.SisRequestOptions;
import cn.jiguang.iot.bean.VersionReportReq;
import cn.jiguang.iot.callback.JclientHandleCallback;
import cn.jiguang.iot.callback.JclientMessageCallback;
import cn.jiguang.iot.mqtt.MqttConnection;
import cn.jiguang.iot.sis.SisConnection;
import cn.jiguang.iot.sis.AbstractSisHandleCallback;
import cn.jiguang.iot.util.JiotCode;
import cn.jiguang.iot.util.JiotConstant;
import cn.jiguang.iot.util.JiotConstant.CLIENT_STATUS;
import cn.jiguang.iot.util.JiotLogger;

import static cn.jiguang.iot.util.JiotConstant.INIT_LOG;

/**
 * @author : ouyangshengduo
 * e-mail : ouysd@jiguang.cn
 * date  : 2019/5/7 17:01
 * desc : SDK 客户端操作对象
 */
public class JiotClient implements JiotClientApi {

    /**sis服务器操作对象**/
    private SisConnection sisConnection;
    /**mqtt服务器操作对象**/
    private MqttConnection mqttConnection;
    /**mqtt客户端操作结果回调**/
    private JclientHandleCallback handleCallback;
    /**mqtt下行消息回调**/
    private JclientMessageCallback messageCallback;
    /**业务定时心跳操作对象**/
    private JiotAlarmHeartBeat jiotAlarmHeartBeat;
    /**sis请求的重试定时器对象**/
    private JiotAlarmHttpRetry jiotAlarmHttpRetry;
    /**设备三元组信息**/
    private DeviceInfo deviceInfo;
    /**标志是否为sis请求的重试请求**/
    private volatile boolean httpRetrying = false;
    /**sis请求的上一次等待的时间间隔，初始化为10，单位为秒**/
    private int lastHttpRetryInteralTime = 10;
    /**客户端的连接状态**/
    private static volatile CLIENT_STATUS clientStatus = CLIENT_STATUS.NONE;
    /**标志用户是否调用了release**/
    private volatile boolean hadReleased = false;
    /**协议类型，有SSL,和无SSL**/
    private int protocolType = JiotConstant.SIS_PROTOCOL_TYPE_SSL;
    /**日志等级**/
    public static int logLevel = 3;
    /**当前尝试的sis服务器的索引**/
    private volatile int currentMqttServerIndex = 0;
    /**最大的mqtt服务端地址的个数**/
    private static final int MAX_TRY_SIS_SERVERS_COUNT = 2;
    /**存储从sis服务器端拿到的mqtt服务地址**/
    private String[] mqttServers = new String[MAX_TRY_SIS_SERVERS_COUNT];
    /**第一个mqtt服务端地址**/
    private static final String SIS_SERVER_HUB_ADDRESS_1 = "hub_addr1";
    /**第二个mqtt服务端地址**/
    private static final String SIS_SERVER_HUB_ADDRESS_2 = "hub_addr2";
    /**最大的间隔时间，270，单位为秒**/
    private static final int MAX_INTERVAL_TIME = 270;

    /********************单例模式（静态内部类）提供实例对象****************************/
    private JiotClient(){
    }
    private static class JiotClientHolder{
        private static final JiotClient INSTANCE = new JiotClient();
    }
    public static JiotClient getInstance(){
        return JiotClientHolder.INSTANCE;
    }
    /********************单例模式（静态内部类）提供实例对象****************************/

    /**主动断开标志**/
    private volatile boolean autoDisconnect = false;


    @SuppressLint("DefaultLocale")
    @Override
    public void jiotInit(Context context,boolean isUseSsl) {
        Context localContext = context.getApplicationContext();
        sisConnection = new SisConnection(new SelfSisHandleCallBack());
        mqttConnection = new MqttConnection(context,new SelfMqttHandleCallback());
        jiotAlarmHeartBeat = new JiotAlarmHeartBeat(localContext);
        jiotAlarmHeartBeat.init(mqttConnection);
        jiotAlarmHttpRetry = new JiotAlarmHttpRetry(localContext);
        jiotAlarmHttpRetry.init(sisConnection);
        if(isUseSsl) {
            protocolType = JiotConstant.SIS_PROTOCOL_TYPE_SSL;
        }else{
            protocolType = JiotConstant.SIS_PROTOCOL_TYPE_TCP;
        }
        hadReleased = false;
        clientStatus = CLIENT_STATUS.CLIENT_INITIALIZED;

        JiotLogger.d(String.format(INIT_LOG,JiotConstant.SDK_VERSION,JiotConstant.SDK_BUILDID));
    }

    @Override
    public int jiotConn(@NonNull DeviceInfo deviceInfo, @NonNull JclientMessageCallback messageCallback, @NonNull JclientHandleCallback handleCallback) {

        JiotLogger.d(JiotConstant.CONN_LOG);

        if(clientStatus.equals(CLIENT_STATUS.NONE)){
            JiotLogger.e("invalid disconnect action,client connect status error");
            return JiotCode.JIOT_ERR_MQTT_STATE_ERROR;
        }
        autoDisconnect = false;
        if(deviceInfo.getProductKey() != null && deviceInfo.getProductKey().length() > JiotConstant.PRODUCT_KEY_MAX_LEN){
            return JiotCode.JIOT_ERR_PRODUCTKEY_OVERLONG;
        }

        if(deviceInfo.getDeviceName() != null && deviceInfo.getDeviceName().length() > JiotConstant.DEVICE_NAME_MAX_LEN){
            return JiotCode.JIOT_ERR_DEVICENAME_OVERLONG;
        }

        if(deviceInfo.getDeviceSecret() != null && deviceInfo.getDeviceSecret().length() > JiotConstant.DEVICE_SECRET_MAX_LEN){
            return JiotCode.JIOT_ERR_DEVICESECRET_OVERLONG;
        }

        if(clientStatus.equals(CLIENT_STATUS.CLIENT_CONNECTING)
            || clientStatus.equals(CLIENT_STATUS.CLIENT_RECONNECTING)
            || clientStatus.equals(CLIENT_STATUS.CLIENT_CONNECTED)
            || clientStatus.equals(CLIENT_STATUS.CLIENT_DISCONNECTING)
            || clientStatus.equals(CLIENT_STATUS.NONE)){

            return JiotCode.JIOT_ERR_MQTT_CONNECT_ERROR;
        }

        this.deviceInfo = deviceInfo;
        this.messageCallback = messageCallback;
        this.handleCallback = handleCallback;
        doConnAction(false);
        return 0;
    }

    /**
     * 执行连接客户端操作
     * @param isReconnect 是否为重连的标志
     */
    private void doConnAction(boolean isReconnect){

        httpRetrying = false;
        if(isReconnect) {
            clientStatus = CLIENT_STATUS.CLIENT_RECONNECTING;
        }else{
            clientStatus = CLIENT_STATUS.CLIENT_CONNECTING;
        }
        SisRequestOptions sisRequestOptions = new SisRequestOptions();
        sisRequestOptions.setProductKey(deviceInfo.getProductKey());
        sisRequestOptions.setShouldRetry(true);
        sisRequestOptions.setRequestProtocolType(protocolType);
        JiotLogger.d("Start requesting sis server");
        sisConnection.request(sisRequestOptions);
    }

    @Override
    public void jiotDisConn() {
        JiotLogger.d(JiotConstant.DISCONN_LOG);

        if(clientStatus.equals(CLIENT_STATUS.NONE)){
            JiotLogger.e("invalid disconnect action,client connect status error");
            return;
        }
        autoDisconnect = true;
        if(!clientStatus.equals(CLIENT_STATUS.CLIENT_DISCONNECTING)) {
            clientStatus = CLIENT_STATUS.CLIENT_DISCONNECTING;
            if(null != jiotAlarmHttpRetry){
                jiotAlarmHttpRetry.stop();
            }
            if(null != jiotAlarmHeartBeat){
                jiotAlarmHeartBeat.stop();
            }
            if (null != mqttConnection) {
                mqttConnection.releaseAlarm();
                MqttContext mqttContext = new MqttContext("disconnect", System.currentTimeMillis());
                mqttConnection.disConnect(mqttContext);
            }
        }else{
            JiotLogger.e("invalid disconnect action,client connect status error");
        }
    }

    @Override
    public void jiotRelease() {
        JiotLogger.d(JiotConstant.RELEASE_LOG);

        if(null != jiotAlarmHttpRetry){
            jiotAlarmHttpRetry.stop();
        }

        if(null != jiotAlarmHeartBeat){
            jiotAlarmHeartBeat.stop();
        }

        if(null != mqttConnection){
            mqttConnection.releaseAlarm();
        }

        hadReleased = true;

        if(null != mqttConnection){
            mqttConnection.setHasReleased(true);
        }
        //如果客户端还处于连接状态或者正在连接状态，则首先断开
        if(clientStatus.equals(CLIENT_STATUS.CLIENT_CONNECTED)
            || clientStatus.equals(CLIENT_STATUS.CLIENT_CONNECTING)
            || clientStatus.equals(CLIENT_STATUS.CLIENT_RECONNECTING)) {
            jiotDisConn();
        }else{
            doReleaseAction();
        }
    }

    @Override
    public int jiotGetConnStatus() {
        JiotLogger.d(JiotConstant.GET_CONN_STATUS_LOG);
        if(clientStatus.equals(CLIENT_STATUS.NONE)) {
            return 0;
        } else if(clientStatus.equals(CLIENT_STATUS.CLIENT_INITIALIZED)) {
            return 1;
        } else if(clientStatus.equals(CLIENT_STATUS.CLIENT_CONNECTING)) {
            return 2;
        } else if(clientStatus.equals(CLIENT_STATUS.CLIENT_CONNECTED)) {
            return 3;
        } else if(clientStatus.equals(CLIENT_STATUS.CLIENT_DISCONNECTING)) {
            return 4;
        } else if(clientStatus.equals(CLIENT_STATUS.CLIENT_DISCONNECTED)) {
            return 5;
        } else if(clientStatus.equals(CLIENT_STATUS.CLIENT_RECONNECTING)) {
            return 6;
        } else {
            return 0;
        }
    }

    @Override
    public JiotResult jiotPropertyReportReq(@NonNull PropertyReportReq properyReport) {
        JiotLogger.d(JiotConstant.REPORT_PROPERTY_LOG);

        if(clientStatus.equals(CLIENT_STATUS.NONE)){
            JiotLogger.e("invalid disconnect action,client connect status error");
            return new JiotResult(JiotCode.JIOT_ERR_MQTT_STATE_ERROR,properyReport.getSeqNo());
        }

        if(!clientStatus.equals(CLIENT_STATUS.CLIENT_CONNECTED)){
            JiotLogger.e("invalid report action,client connect status error");
            return new JiotResult(JiotCode.JIOT_ERR_MQTT_STATE_ERROR,properyReport.getSeqNo());
        }
        // 校验参数基本格式
        if(properyReport.getSeqNo() < 0
                || properyReport.getVersion() <= 0
                || properyReport.getProperties().length == 0){

            if(properyReport.getSeqNo() < 0){
                //序号错误
                return new JiotResult(JiotCode.JIOT_ERR_SEQNO_ERROR,properyReport.getSeqNo());
            }

            if(properyReport.getVersion() <= 0){
                //序号错误
                return new JiotResult(JiotCode.JIOT_ERR_VERSION_FORMAT_ERROR,properyReport.getSeqNo());
            }
            //参数异常
            return new JiotResult(JiotCode.JIOT_ERR_ARGU_FORMAT_ERROR,properyReport.getSeqNo());
        }
        //校验属性内容
        for(int i = 0 ; i < properyReport.getProperties().length ; i ++){
            try {
                if (properyReport.getProperties()[i].getName().isEmpty()
                        || properyReport.getProperties()[i].getName().length() > 32){
                    return new JiotResult(JiotCode.JIOT_ERR_PROPERTY_NAME_FORMAT_ERROR,properyReport.getSeqNo());
                }
            }catch (Exception e){
                return new JiotResult(JiotCode.JIOT_ERR_PROPERTY_VALUE_FORMAT_ERROR,properyReport.getSeqNo());
            }
        }

        try {
            Map<String, Object> data = new HashMap<>(4);
            long currentMills = System.currentTimeMillis();
            if(properyReport.getSeqNo() == 0) {
                data.put("seq_no", currentMills);
            }else{
                data.put("seq_no", properyReport.getSeqNo());
            }
            data.put("time",currentMills);
            data.put("version",properyReport.getVersion());
            JSONArray jsonProperties = new JSONArray();
            for(int i = 0 ; i < properyReport.getProperties().length ; i ++){
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("name",properyReport.getProperties()[i].getName());
                jsonObject.put("time",properyReport.getProperties()[i].getTime());
                jsonObject.put("value",properyReport.getProperties()[i].getValue());
                jsonProperties.put(jsonObject);
            }
            data.put("property_list",jsonProperties);
            // MQTT消息
            MqttMessage message = new MqttMessage();
            JSONObject jsonObject = new JSONObject();
            for (Map.Entry<String, Object> entrys : data.entrySet()) {
                jsonObject.put(entrys.getKey(), entrys.getValue());
            }
            message.setQos(MqttConstans.QOS1);
            message.setPayload(jsonObject.toString().getBytes());
            // 用户上下文（请求实例）
            MqttContext mqttContext = new MqttContext("publishTopic", currentMills);
            mqttConnection.publish(mqttContext, String.format(MqttConstans.JMQTT_TOPIC_PROPERTY_REPORT_REQ, mqttConnection.getDeviceInfo().getProductKey(), mqttConnection.getDeviceInfo().getDeviceName()), message);
        }catch (Exception e){
            return new JiotResult(JiotCode.JIOT_ERR_PROPERTY_FORMAT_ERROR,properyReport.getSeqNo());
        }
        return new JiotResult(JiotCode.JIOT_SUCCESS,properyReport.getSeqNo());
    }

    @Override
    public JiotResult jiotEventReportReq(@NonNull EventReportReq eventReportReq) {
        JiotLogger.d(JiotConstant.REPORT_EVENT_LOG);

        if(clientStatus.equals(CLIENT_STATUS.NONE)){
            JiotLogger.e("invalid disconnect action,client connect status error");
            return new JiotResult(JiotCode.JIOT_ERR_MQTT_STATE_ERROR,eventReportReq.getSeqNo());
        }

        if(!clientStatus.equals(CLIENT_STATUS.CLIENT_CONNECTED)){
            JiotLogger.e("invalid report action,client connect status error");
            return new JiotResult(JiotCode.JIOT_ERR_MQTT_STATE_ERROR,eventReportReq.getSeqNo());
        }

        // 校验参数基本格式
        if(eventReportReq.getSeqNo() < 0){
            //参数异常
            return new JiotResult(JiotCode.JIOT_ERR_SEQNO_ERROR,eventReportReq.getSeqNo());
        }

        if(null == eventReportReq.getEvent()
                || null == eventReportReq.getEvent().getName()
                || eventReportReq.getEvent().getName().isEmpty()
                || eventReportReq.getEvent().getName().length() > JiotConstant.EVENT_NAME_MAX_LEN){
            //事件名称格式异常
            return new JiotResult(JiotCode.JIOT_ERR_EVENT_NAME_FORMAT_ERROR,eventReportReq.getSeqNo());
        }

        if(null == eventReportReq.getEvent().getContent()
                || eventReportReq.getEvent().getContent().isEmpty()
                || eventReportReq.getEvent().getContent().length() > JiotConstant.EVENT_CONTENT_MAX_LEN){
            //事件内容格式异常
            return new JiotResult(JiotCode.JIOT_ERR_EVENT_CONTENT_FORMAT_ERROR,eventReportReq.getSeqNo());
        }

        try {
            Map<String, Object> data = new HashMap<>(3);
            long currentMills = System.currentTimeMillis();
            if(eventReportReq.getSeqNo() == 0) {
                data.put("seq_no", currentMills);
            }else{
                data.put("seq_no", eventReportReq.getSeqNo());
            }
            data.put("time",currentMills);
            JSONObject jsonEvent = new JSONObject();
            jsonEvent.put("name",eventReportReq.getEvent().getName());
            jsonEvent.put("time",eventReportReq.getEvent().getTime());
            jsonEvent.put("content",eventReportReq.getEvent().getContent());
            data.put("event",jsonEvent);
            // MQTT消息
            MqttMessage message = new MqttMessage();
            JSONObject jsonObject = new JSONObject();
            for (Map.Entry<String, Object> entrys : data.entrySet()) {
                jsonObject.put(entrys.getKey(), entrys.getValue());
            }
            message.setQos(MqttConstans.QOS1);
            message.setPayload(jsonObject.toString().getBytes());
            // 用户上下文（请求实例）
            MqttContext mqttContext = new MqttContext("publishTopic", currentMills);
            mqttConnection.publish(mqttContext, String.format(MqttConstans.JMQTT_TOPIC_EVENT_REPORT_REQ, mqttConnection.getDeviceInfo().getProductKey(), mqttConnection.getDeviceInfo().getDeviceName()), message);
        }catch (Exception e){
            return new JiotResult(JiotCode.JIOT_ERR_EVENT_FORMAT_ERROR,eventReportReq.getSeqNo());
        }
        return new JiotResult(JiotCode.JIOT_SUCCESS,eventReportReq.getSeqNo());
    }

    @Override
    public JiotResult jiotVersionReportReq(VersionReportReq versionReportReq) {
        JiotLogger.d(JiotConstant.REPORT_VERSION_LOG);

        if(clientStatus.equals(CLIENT_STATUS.NONE)){
            JiotLogger.e("invalid disconnect action,client connect status error");
            return new JiotResult(JiotCode.JIOT_ERR_MQTT_STATE_ERROR,versionReportReq.getSeqNo());
        }

        if(!clientStatus.equals(CLIENT_STATUS.CLIENT_CONNECTED)){
            JiotLogger.e("invalid report action,client connect status error");
            return new JiotResult(JiotCode.JIOT_ERR_MQTT_STATE_ERROR,versionReportReq.getSeqNo());
        }
        // 校验参数基本格式
        if(versionReportReq.getSeqNo() < 0){
            //序号错误
            return new JiotResult(JiotCode.JIOT_ERR_SEQNO_ERROR,versionReportReq.getSeqNo());
        }

        if(null == versionReportReq.getVersion()
                || versionReportReq.getVersion().isEmpty()
                || versionReportReq.getVersion().length() > JiotConstant.VERSION_MAX_LEN){
            //版本异常
            return new JiotResult(JiotCode.JIOT_ERR_VERSION_APP_VAR_FORMAT_ERROR,versionReportReq.getSeqNo());
        }


        try {
            Map<String, Object> data = new HashMap<>(4);
            long currentMills = System.currentTimeMillis();
            if(versionReportReq.getSeqNo() == 0) {
                data.put("seq_no", currentMills);
            }else{
                data.put("seq_no", versionReportReq.getSeqNo());
            }
            data.put("time",currentMills);
            data.put("app_ver",versionReportReq.getVersion());
            data.put("sdk_ver",JiotConstant.SDK_VERSION);
            // MQTT消息
            MqttMessage message = new MqttMessage();

            JSONObject jsonObject = new JSONObject();
            for (Map.Entry<String, Object> entrys : data.entrySet()) {
                jsonObject.put(entrys.getKey(), entrys.getValue());
            }
            message.setQos(MqttConstans.QOS1);
            message.setPayload(jsonObject.toString().getBytes());
            // 用户上下文（请求实例）
            MqttContext mqttContext = new MqttContext("publishTopic", currentMills);
            mqttConnection.publish(mqttContext, String.format(MqttConstans.JMQTT_TOPIC_VERSION_REPORT_REQ, mqttConnection.getDeviceInfo().getProductKey(), mqttConnection.getDeviceInfo().getDeviceName()), message);
        }catch (Exception e){
            return new JiotResult(JiotCode.JIOT_ERR_VERSION_FORMAT_ERROR,versionReportReq.getSeqNo());
        }
        return new JiotResult(JiotCode.JIOT_SUCCESS,versionReportReq.getSeqNo());
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void jiotSetLogLevel(int level) {
        JiotLogger.d(String.format(JiotConstant.SET_LOG_LEVEL_LOG,level));
        logLevel = level;
    }

    /**
     * 实现SisHandleCallback回调接口
     */
    private class SelfSisHandleCallBack extends AbstractSisHandleCallback {
        @Override
        public void onFailure(String host, Throwable throwable) {
            JiotLogger.e("sis request failed ,caused by : " + throwable.getMessage());

            if(autoDisconnect || hadReleased){
                return;
            }
            if(SisContants.HOST.equals(host)){
                //尝试默认HOST失败，再使用隐藏HOST进行尝试
                doReRequestWithHiddenHost();
            }else {
                clientStatus = CLIENT_STATUS.CLIENT_DISCONNECTED;
                handleCallback.jiotConnectFailHandle(JiotCode.JIOT_ERR_SIS_HTTP_FAIL);
                doLoopRequest();
            }
        }

        /**
         * 使用隐藏IP再次请求
         */
        private void doReRequestWithHiddenHost() {

            SisRequestOptions sisRequestOptions = new SisRequestOptions();
            sisRequestOptions.setHost(SisContants.HIDDEN_HOST);
            sisRequestOptions.setProductKey(deviceInfo.getProductKey());
            sisRequestOptions.setShouldRetry(false);
            sisRequestOptions.setRequestProtocolType(protocolType);
            sisConnection.request(sisRequestOptions);
        }


        @Override
        public void onResponse(String host, final String response) {


            if(autoDisconnect || hadReleased){
                return;
            }
            //拿到sis返回的mqtt服务器列表
            if(null == response){
                JiotLogger.d("mqtt servers list is null");
                if(SisContants.HOST.equals(host)){
                    //尝试默认HOST失败，再使用隐藏HOST进行尝试
                    doReRequestWithHiddenHost();
                }else {
                    clientStatus = CLIENT_STATUS.CLIENT_DISCONNECTED;
                    handleCallback.jiotConnectFailHandle(JiotCode.JIOT_ERR_SIS_HTTP_FAIL);
                    doLoopRequest();
                }
                return;
            }
            try {
                JSONObject sisServersJson = new JSONObject(response);
                int validSisServerCount = 0;

                if(sisServersJson.has(SIS_SERVER_HUB_ADDRESS_1)){
                    mqttServers[validSisServerCount ++] = sisServersJson.getString(SIS_SERVER_HUB_ADDRESS_1);
                }

                if(sisServersJson.has(SIS_SERVER_HUB_ADDRESS_2)){
                    mqttServers[validSisServerCount ++] = sisServersJson.getString(SIS_SERVER_HUB_ADDRESS_2);
                }

                if(validSisServerCount <= 0){
                    if(null != handleCallback){
                        clientStatus = CLIENT_STATUS.CLIENT_DISCONNECTED;
                        handleCallback.jiotConnectFailHandle(JiotCode.JIOT_ERR_SIS_CONTENT_ERROR);
                    }
                    if(SisContants.HOST.equals(host)){
                        //尝试默认HOST失败，再使用隐藏HOST进行尝试
                        doReRequestWithHiddenHost();
                    }else {
                        doLoopRequest();
                    }
                    return;
                }


                currentMqttServerIndex = 0;
                //拿到sis返回的mqtt服务器，就需要进行mqtt的连接逻辑
                if(null != mqttConnection){
                    MqttContext mqttContext = new MqttContext("connect", System.currentTimeMillis());
                    mqttConnection.connect(mqttContext,mqttServers[currentMqttServerIndex],deviceInfo,protocolType);
                }
            } catch (JSONException e) {
                e.printStackTrace();
                //返回数据json格式异常，需要抛出异常，或者返回错误码
                if(null != handleCallback){
                    clientStatus = CLIENT_STATUS.CLIENT_DISCONNECTED;
                    handleCallback.jiotConnectFailHandle(JiotCode.JIOT_ERR_SIS_JSON_PARSE_FAIL);
                }
                if(SisContants.HOST.equals(host)){
                    //尝试默认HOST失败，再使用隐藏HOST进行尝试
                    doReRequestWithHiddenHost();
                }else {
                    doLoopRequest();
                }
            }
        }
    }

    /**
     * 执行循环请求sis服务器
     */
    private void doLoopRequest() {

        clientStatus = CLIENT_STATUS.CLIENT_RECONNECTING;
        if (!httpRetrying) {
            httpRetrying = true;
            if(null != jiotAlarmHttpRetry && jiotAlarmHttpRetry.isHasStarted()){
                jiotAlarmHttpRetry.stop();
            }
            lastHttpRetryInteralTime = 10;
            if (jiotAlarmHttpRetry != null) {
                jiotAlarmHttpRetry.start();
            }
        } else{
            int time = (lastHttpRetryInteralTime + 20) * 2 - 20;
            if (time > MAX_INTERVAL_TIME) {
                time = MAX_INTERVAL_TIME;
            }
            if(null != jiotAlarmHttpRetry) {
                jiotAlarmHttpRetry.scheduleHttpRetry(time * 1000);
            }
            lastHttpRetryInteralTime = time;
        }
    }

    /**
     * 实现MqttHandleCallback回调接口
     */
    private class SelfMqttHandleCallback extends AbstractMqttHandleCallback {

        @Override
        public void onConnectCompleted(Status status, boolean reconnect, Object userContext, String msg) {

            String userContextInfo = "";
            if (userContext instanceof MqttContext) {
                userContextInfo = userContext.toString();
            }


            String logInfo = String.format("onConnectCompleted, status[%s], reconnect[%b], userContext[%s], msg[%s]",
                    status.name(), reconnect, userContextInfo, msg);
            JiotLogger.d(logInfo);
            if(status.equals(Status.OK)){
                //mqtt连接成功
                if(null != mqttConnection){
                    // 用户上下文（请求实例）sub/sys/${ProductKey}/*/msg/deliver
                    String[] topics = {String.format(MqttConstans.JMQTT_TOPIC_CLIENT_REQ_RECEIVE,deviceInfo.getProductKey(),deviceInfo.getDeviceName()),
                            String.format(MqttConstans.JMQTT_TOPIC_CLIENT_RECEIVE_MSG,deviceInfo.getProductKey())};
                    MqttContext mqttContext = new MqttContext("subscribeTopic", System.currentTimeMillis());
                    mqttConnection.subscribe(mqttContext,topics);
                }
            }else {
                //连接失败
                if (reconnect) {
                    doConnAction(true);
                } else {

                    //判断是否需要更换mqtt服务端再连接
                    if (currentMqttServerIndex == 0 && !autoDisconnect) {
                        currentMqttServerIndex = 1;
                        if (null != mqttServers[currentMqttServerIndex] && null != mqttConnection) {
                            clientStatus = CLIENT_STATUS.CLIENT_RECONNECTING;
                            MqttContext mqttContext = new MqttContext("connect", System.currentTimeMillis());
                            mqttConnection.connect(mqttContext, mqttServers[currentMqttServerIndex], deviceInfo,protocolType);
                            return;
                        } else {
                            if (null != handleCallback) {
                                clientStatus = CLIENT_STATUS.CLIENT_DISCONNECTED;
                                if(status.equals(Status.TIME_OUT)) {
                                    handleCallback.jiotConnectFailHandle(JiotCode.JIOT_ERR_MQTT_NETWORK_CONNECT_ERROR);
                                }else{
                                    handleCallback.jiotConnectFailHandle(JiotCode.JIOT_ERR_MQTT_CONNECT_ERROR);
                                }
                            }
                        }
                    } else {
                        if (null != handleCallback) {
                            clientStatus = CLIENT_STATUS.CLIENT_DISCONNECTED;
                            if(status.equals(Status.TIME_OUT)) {
                                handleCallback.jiotConnectFailHandle(JiotCode.JIOT_ERR_MQTT_NETWORK_CONNECT_ERROR);
                            }else{
                                handleCallback.jiotConnectFailHandle(JiotCode.JIOT_ERR_MQTT_CONNECT_ERROR);
                            }
                        }
                    }
                    //重新进行sis请求逻辑

                    if(!autoDisconnect) {
                        doLoopRequest();
                    }
                }
            }
        }

        @Override
        public void onStartReConnecting() {
            clientStatus = CLIENT_STATUS.CLIENT_RECONNECTING;
        }

        @Override
        public void onConnectionLost(Throwable cause) {
            String logInfo = String.format("onConnectionLost, cause[%s]", cause.toString());
            JiotLogger.d(logInfo);
            clientStatus = CLIENT_STATUS.CLIENT_DISCONNECTED;
            if(null != handleCallback){
                handleCallback.jiotDisconnectHandle(JiotCode.JIOT_SUCCESS,cause.getMessage());
            }

            if(!hadReleased && !autoDisconnect && null != mqttConnection){
                mqttConnection.reconnect();
            }
        }

        @Override
        public void onDisconnectCompleted(Status status, Object userContext, String msg) {

            String userContextInfo = "";
            if (userContext instanceof MqttContext) {
                userContextInfo = userContext.toString();
            }
            String logInfo = String.format("onDisconnectCompleted, status[%s], userContext[%s], msg[%s]", status.name(), userContextInfo,msg);
            JiotLogger.d(logInfo);

            if(status.equals(Status.OK)){
                //断开连接成功
                if(null != jiotAlarmHeartBeat){
                    jiotAlarmHeartBeat.stop();
                }

                if(null != jiotAlarmHttpRetry){
                    jiotAlarmHttpRetry.stop();
                }

                clientStatus = CLIENT_STATUS.CLIENT_DISCONNECTED;
                if (null != handleCallback) {
                    handleCallback.jiotDisconnectHandle(JiotCode.JIOT_SUCCESS, null);
                }
                if(hadReleased){
                    //属于用户主动释放资源操作
                    if(null != mqttConnection){
                        mqttConnection.close();
                    }
                    doReleaseAction();
                }
            }else{
                if(null != handleCallback){
                    handleCallback.jiotDisconnectHandle(JiotCode.JIOT_ERR_DISCONNECT_ERROR,msg);
                }
            }
        }

        @Override
        public void onPublishCompleted(Status status, IMqttToken token, Object userContext, String errMsg) {
            String userContextInfo = "";
            long seqNo = 0;
            if (userContext instanceof MqttContext) {
                userContextInfo = userContext.toString();
                seqNo = ((MqttContext) userContext).getSeqNo();
            }

            if(null != token && !status.equals(Status.ERROR)) {
                String logInfo = String.format("onPublishCompleted, status[%s], topics[%s], userContext[%s], errMsg[%s]",
                        status.name(), Arrays.toString(token.getTopics()), userContextInfo, errMsg);
                JiotLogger.d(logInfo);
            }
            if(status.equals(Status.TIME_OUT)){
                //发布消息超时
                clientStatus = CLIENT_STATUS.CLIENT_RECONNECTING;
                if(null != handleCallback){
                    handleCallback.jiotMessageTimeoutHandle(seqNo);
                }

                if(null != mqttConnection){
                    mqttConnection.reconnect();
                }
            }
        }

        @Override
        public void onSubscribeCompleted(Status status, IMqttToken asyncActionToken, Object userContext, String errMsg) {

            String userContextInfo = "";
            if (userContext instanceof MqttContext) {
                userContextInfo = userContext.toString();
            }
            String logInfo = String.format("onSubscribeCompleted, status[%s], topics[%s], userContext[%s],errMsg[%s]",
                    status.name(), Arrays.toString(asyncActionToken.getTopics()), userContextInfo, errMsg);
            JiotLogger.d(logInfo);

            if(status.equals(Status.OK)){
                //订阅topic成功
                JiotLogger.d("Jiot client subscribe topics success.");
                if(null != mqttConnection){
                    // 要发布的数据
                    long currentMills = System.currentTimeMillis();
                    // MQTT消息
                    MqttMessage message = new MqttMessage();
                    JSONObject jsonObject = new JSONObject();
                    try {
                        jsonObject.put("seq_no",currentMills);
                    } catch (JSONException e) {
                        JiotLogger.e("pack json data failed!",e);
                    }
                    message.setQos(MqttConstans.QOS1);
                    message.setPayload(jsonObject.toString().getBytes());
                    // 用户上下文（请求实例）
                    MqttContext mqttContext = new MqttContext("publishTopic", currentMills);
                    mqttConnection.publish(mqttContext,String.format(MqttConstans.JMQTT_TOPIC_IOT_PING_REQ,deviceInfo.getProductKey(),deviceInfo.getDeviceName()),message);
                }
            }else{
                //订阅失败
                if(null != handleCallback){
                    handleCallback.jiotSubscribeFailHandle(Arrays.toString(asyncActionToken.getTopics()));
                }
            }
        }

        @Override
        public void onUnSubscribeCompleted(Status status, IMqttToken asyncActionToken, Object userContext, String errMsg) {
            String userContextInfo = "";
            if (userContext instanceof MqttContext) {
                userContextInfo = userContext.toString();
            }

            String logInfo = String.format("onUnSubscribeCompleted, status[%s], topics[%s], userContext[%s], errMsg[%s]",
                    status.name(), Arrays.toString(asyncActionToken.getTopics()), userContextInfo,errMsg);
            JiotLogger.d(logInfo);
        }

        @Override
        public void onMessageReceived(final String topic, final MqttMessage message) {
            String logInfo = String.format("receive command, topic[%s], message[%s]", topic, message.toString());
            JiotLogger.d(logInfo);

            if(topic.equals(String.format(MqttConstans.JMQTT_TOPIC_IOT_PING_RESP,deviceInfo.getProductKey(),deviceInfo.getDeviceName()))){
                //代表是业务心跳的返回
                //业务心跳成功，代表连接最后一个步骤成功，返回连接成功
                handlePingMessageReceived();

            }else if(topic.equals(String.format(MqttConstans.JMQTT_TOPIC_MSG_DELIVER_REQ,deviceInfo.getProductKey(),deviceInfo.getDeviceName()))){
                //客户端收到服务端下发的传递消息请求
                handleMsgDeleverReceived(message);

            }else if(topic.equals(String.format(MqttConstans.JMQTT_TOPIC_PROPERTY_SET_REQ,deviceInfo.getProductKey(),deviceInfo.getDeviceName()))){
                //客户端收到服务端下发的设置属性请求
                handlePropertySetReceived(message);

            }else if(topic.equals(String.format(MqttConstans.JMQTT_TOPIC_PROPERTY_REPORT_RSP,deviceInfo.getProductKey(),deviceInfo.getDeviceName()))){
                //客户端上报属性后，客户端收到服务端的属性上报确认回复
                handleReportPropertRspReceived(message);

            }else if(topic.equals(String.format(MqttConstans.JMQTT_TOPIC_EVENT_REPORT_RSP,deviceInfo.getProductKey(),deviceInfo.getDeviceName()))){
                //客户端上报事件后，客户端收到服务端的事件上报确认回复
                handleReportEventRspReceived(message);

            }else if(topic.equals(String.format(MqttConstans.JMQTT_TOPIC_VERSION_REPORT_RSP,deviceInfo.getProductKey(),deviceInfo.getDeviceName()))){
                //客户端上报版本后，客户端收到服务端的版本上报确认回复
                handleReportVersionRspReceived(message);
            }
        }

        /**
         * 处理客户端上报设备版本后的回复
         * @param message 消息内容
         */
        private void handleReportVersionRspReceived(MqttMessage message) {
            VersionReportRsp versionReportRsp = new VersionReportRsp();
            int errorCode = 0;
            try {
                JSONObject jsonObject = new JSONObject(new String(message.getPayload()));
                versionReportRsp.setSeqNo(jsonObject.getLong("seq_no"));
                versionReportRsp.setCode(jsonObject.getInt("code"));
            } catch (Exception e) {
                e.printStackTrace();
                errorCode = JiotCode.JIOT_ERR_JCLI_JSON_PARSE_ERR;
            }
            if(null != messageCallback){
                messageCallback.jiotVersionReportRsp(versionReportRsp,errorCode);
            }
        }

        /**
         * 处理客户端上报事件后的回复
         * @param message 消息内容
         */
        private void handleReportEventRspReceived(MqttMessage message) {
            EventReportRsp eventReportRsp = new EventReportRsp();
            int errorCode = 0;
            try {
                JSONObject jsonObject = new JSONObject(new String(message.getPayload()));
                eventReportRsp.setSeqNo(jsonObject.getLong("seq_no"));
                eventReportRsp.setCode(jsonObject.getInt("code"));
            } catch (Exception e) {
                e.printStackTrace();
                errorCode = JiotCode.JIOT_ERR_JCLI_JSON_PARSE_ERR;
            }
            if(null != messageCallback){
                messageCallback.jiotEventReportRsp(eventReportRsp,errorCode);
            }
        }

        /**
         * 处理客户端上报属性后的回复
         * @param message 消息内容
         */
        private void handleReportPropertRspReceived(final MqttMessage message) {
            PropertyReportRsp propertyReportRsp = new PropertyReportRsp();
            int errorCode = 0;
            try {
                JSONObject jsonObject = new JSONObject(new String(message.getPayload()));
                propertyReportRsp.setSeqNo(jsonObject.getLong("seq_no"));
                propertyReportRsp.setVerion(jsonObject.getLong("version"));
                propertyReportRsp.setCode(jsonObject.getInt("code"));
                JSONArray jsonArray = jsonObject.getJSONArray("property_list");
                Property[] properties = new Property[jsonArray.length()];
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject subObject = jsonArray.getJSONObject(i);
                    Property property = new Property();
                    property.setName(subObject.getString("name"));
                    property.setTime(subObject.getLong("time"));
                    property.setValue(subObject.getString("value"));
                    properties[i] = property;
                }
                propertyReportRsp.setProperties(properties);
            } catch (Exception e) {
                e.printStackTrace();
                errorCode = JiotCode.JIOT_ERR_JCLI_JSON_PARSE_ERR;
            }
            if(null != messageCallback){
                messageCallback.jiotPropertyReportRsp(propertyReportRsp,errorCode);
            }
        }

        /**
         * 处理客户端收到服务器下发的设置属性请求
         * @param message 消息内容
         */
        private void handlePropertySetReceived(final MqttMessage message) {
            PropertySetReq propertySetReq = new PropertySetReq();
            int errorCode = 0;
            try {
                JSONObject jsonObject = new JSONObject(new String(message.getPayload()));
                propertySetReq.setSeqNo(jsonObject.getLong("seq_no"));
                propertySetReq.setVersion(jsonObject.getLong("version"));
                propertySetReq.setTime(jsonObject.getLong("time"));
                JSONArray jsonArray = jsonObject.getJSONArray("property_list");
                Property[] properties = new Property[jsonArray.length()];
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject subObject = jsonArray.getJSONObject(i);
                    Property property = new Property();
                    property.setName(subObject.getString("name"));
                    property.setTime(subObject.getLong("time"));
                    property.setValue(subObject.getString("value"));
                    properties[i] = property;
                }
                propertySetReq.setProperties(properties);
                //  需要先进行回复确认
                setPropertyRsp(propertySetReq.getSeqNo(),jsonObject.getJSONArray("property_list"));
            } catch (Exception e) {
                e.printStackTrace();
                errorCode = JiotCode.JIOT_ERR_JCLI_JSON_PARSE_ERR;
            }
            if(null != messageCallback){
                messageCallback.jiotPropertySetReq(propertySetReq,errorCode);
            }
        }

        /**
         * 处理客户端收到服务器的消息下发请求
         * @param message 消息内容
         */
        private void handleMsgDeleverReceived(final MqttMessage message) {
            MsgDeliverReq msgDeliverReq = new MsgDeliverReq();
            int errorCode = 0;
            try {
                JSONObject jsonObject = new JSONObject(new String(message.getPayload()));
                msgDeliverReq.setMessage(jsonObject.getString("message"));
                msgDeliverReq.setSeqNo(jsonObject.getLong("seq_no"));
                msgDeliverReq.setTime(jsonObject.getLong("time"));
                //  需要先进行回复确认
                msgDeliverRsp(msgDeliverReq.getSeqNo());
            } catch (JSONException e) {
                e.printStackTrace();
                errorCode = JiotCode.JIOT_ERR_JCLI_JSON_PARSE_ERR;
            }

            if(null != messageCallback){
                messageCallback.jiotMsgDeliverReq(msgDeliverReq,errorCode);
            }
        }

        /**
         * 处理业务心跳的回复之后的逻辑
         */
        private void handlePingMessageReceived() {

            if(null != handleCallback && !clientStatus.equals(CLIENT_STATUS.CLIENT_CONNECTED)){
                clientStatus = CLIENT_STATUS.CLIENT_CONNECTED;
                //执行到这里，代表sis请求到的数据合法
                httpRetrying = false;
                if(null != jiotAlarmHttpRetry){
                    jiotAlarmHttpRetry.stop();
                }
                if(null != jiotAlarmHeartBeat){
                    jiotAlarmHeartBeat.start();
                }
                JiotLogger.d("Jiot client connect success.");
                handleCallback.jiotConnectedHandle();
            }else if(clientStatus.equals(CLIENT_STATUS.CLIENT_CONNECTED)){
                if(null != jiotAlarmHeartBeat){
                    jiotAlarmHeartBeat.scheduleHeartbeat(JiotAlarmHeartBeat.HEARTBEAT_INTERVAL);
                }
            }
        }

        /**
         * 收到设置属性消息后的回复确认
         * @param seqNo 序号
         * @param properties 属性json数组
         */
        private void setPropertyRsp(long seqNo, JSONArray properties) {
            // MQTT消息
            MqttMessage message = new MqttMessage();
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("seq_no",seqNo);
                jsonObject.put("code",0);
                jsonObject.put("property_list",properties);
            } catch (JSONException e) {
                JiotLogger.e("pack json data failed!",e);
            }
            message.setQos(MqttConstans.QOS0);
            message.setPayload(jsonObject.toString().getBytes());
            // 用户上下文（请求实例）
            MqttContext mqttContext = new MqttContext("publishTopic", seqNo);
            mqttConnection.publish(mqttContext,String.format(MqttConstans.JMQTT_TOPIC_PROPERTY_SET_RSP,deviceInfo.getProductKey(),deviceInfo.getDeviceName()),message);
        }

        /**
         * 收到消息后的回复确认
         * @param seqNo 序号
         */
        private void msgDeliverRsp(long seqNo) {
            // MQTT消息
            MqttMessage message = new MqttMessage();
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("seq_no",seqNo);
                jsonObject.put("code",0);
            } catch (JSONException e) {
                JiotLogger.e("pack json data failed!",e);
            }
            message.setQos(MqttConstans.QOS0);
            message.setPayload(jsonObject.toString().getBytes());
            // 用户上下文（请求实例）
            MqttContext mqttContext = new MqttContext("publishTopic", seqNo);
            mqttConnection.publish(mqttContext,String.format(MqttConstans.JMQTT_TOPIC_MSG_DELIVER_RSP,deviceInfo.getProductKey(),deviceInfo.getDeviceName()),message);
        }
    }

    /**
     * 释放资源操作
     */
    private void doReleaseAction() {
        clientStatus = CLIENT_STATUS.NONE;
        sisConnection = null;
        mqttConnection = null;
        jiotAlarmHttpRetry = null;
        jiotAlarmHeartBeat = null;
        hadReleased = false;
    }

}
