package cn.jiguang.iot.mqtt;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.PowerManager;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;

import cn.jiguang.iot.util.JiotLogger;

/**
 * @author : ouyangshengduo
 * e-mail : ouysd@jiguang.cn
 * date  : 2019/5/9 16:36
 * desc : mqtt重连定时类
 */
class JiotAlarmMqttReconnect {

    /**应用上下文**/
    private Context context;
    /**定时所需要的闹钟接收广播**/
    private BroadcastReceiver jiotAlarmMqttReconnectReceiver;
    /**声明将来要执行的intent对象**/
    private PendingIntent pendingIntent;
    /**标志当前定时任务是否已经开启**/
    private volatile boolean hasStarted = false;
    /**广播接收器所需要注册的一个唯一action**/
    private static final String ACTION_MQTT_RECONNECT = "CN.JIGUANG.IOT.MQTT.RECONNECT";
    /**mqtt操作对象**/
    private MqttAsyncClient mqttClient;
    /**mqtt重连的参数配置**/
    private MqttConnectOptions options;
    /**连接状态回调**/
    private IMqttActionListener listener;
    /**默认定时器下一个操作执行时间 5秒**/
    private static final int DEFALUT_INTERVAL_TIME = 5000;
    /**
     * 构造方法
     * @param context 需要传入应用上下文
     */
    JiotAlarmMqttReconnect(Context context){
        this.context = context;
    }


    /**
     * 定时器初始化
     * @param mqttClient mqtt客户端
     * @param options mqtt连接参数
     * @param listener mqtt连接状态回调
     */
    void init(MqttAsyncClient mqttClient, MqttConnectOptions options, IMqttActionListener listener){
        this.listener = listener;
        this.mqttClient = mqttClient;
        this.options = options;
        this.jiotAlarmMqttReconnectReceiver = new JiotAlarmMqttReconnect.JiotAlarmMqttReconnectReceiver();
    }

    /**
     * 启动当前定时任务
     */
    void start() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_MQTT_RECONNECT);
        JiotLogger.d("Register JiotAlarmMqttReconnectReceiver to Context " + mqttClient.getClientId());
        if (context != null && jiotAlarmMqttReconnectReceiver != null) {
            context.registerReceiver(jiotAlarmMqttReconnectReceiver, intentFilter);
        }
        pendingIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_MQTT_RECONNECT), PendingIntent.FLAG_UPDATE_CURRENT);
        //获取AlarmManager系统服务
        scheduleMqttReconnect(DEFALUT_INTERVAL_TIME);

    }

    /**
     * 停止当前定时任务
     */
    void stop() {

        if(hasStarted){
            JiotLogger.d("Unregister JiotAlarmMqttReconnectReceiver to Context " + mqttClient.getClientId());
            if(pendingIntent != null){
                AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                if (alarmManager != null) {
                    alarmManager.cancel(pendingIntent);
                }
            }
            hasStarted = false;
            try{
                context.unregisterReceiver(jiotAlarmMqttReconnectReceiver);
            }catch(IllegalArgumentException e){
                //Ignore unregister errors.
            }
        }
    }

    /**
     * 将定时任务设置给系统
     * @param delayInMilliseconds 下一次任务运行的时间间隔
     */
    void scheduleMqttReconnect(long delayInMilliseconds) {
        long nextAlarmInMilliseconds = System.currentTimeMillis() + delayInMilliseconds;
        //获取AlarmManager系统服务
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        //兼容不同Android版本的AlarmManager服务
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if (alarmManager != null) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextAlarmInMilliseconds, pendingIntent);
                hasStarted = true;
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (alarmManager != null) {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, nextAlarmInMilliseconds, pendingIntent);
                hasStarted = true;
            }
        } else {
            if (alarmManager != null) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, nextAlarmInMilliseconds, pendingIntent);
                hasStarted = true;
            }
        }
    }

    /**
     * 自定义广播接收器，处理定时任务时间间隔到了后的任务执行
     */
    class JiotAlarmMqttReconnectReceiver extends BroadcastReceiver {

        private PowerManager.WakeLock wakelock;

        private final String wakeLockTag = "Jiot.MqttReconnect.Receiver." + mqttClient.getClientId();

        @Override
        @SuppressLint({"Wakelock", "WakelockTimeout"})
        public void onReceive(Context context, Intent intent) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                wakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, wakeLockTag);
                wakelock.acquire();
                try{
                    // 要发布的数据
                    if(null != mqttClient && mqttClient.isConnected()){
                        try {
                            mqttClient.disconnect(0);
                        } catch (MqttException e) {
                            e.printStackTrace();
                        }
                    }
                    try {
                        if (mqttClient != null) {
                            JiotLogger.d("The mqtt client is reconnecting .");
                            mqttClient.connect(options,null,listener);
                        }
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                }finally {
                    wakelock.release();
                }
            }
        }
    }

}
