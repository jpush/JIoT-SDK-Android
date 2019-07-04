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

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;
import cn.jiguang.iot.util.JiotLogger;

/**
 * @author : ouyangshengduo
 * e-mail : ouysd@jiguang.cn
 * date  : 2019/5/9 16:36
 * desc : 业务心跳类
 */
public class JiotAlarmHeartBeat{

    /**应用上下文**/
    private Context context;
    /**mqtt操作对象**/
    private MqttConnection mqttConnection;
    /**定时所需要的闹钟接收广播**/
    private BroadcastReceiver jiotAlarmHeartBeatReceiver;
    /**声明将来要执行的intent对象**/
    private PendingIntent pendingIntent;
    /**标志当前定时任务是否已经开启**/
    private volatile boolean hasStarted = false;
    /**广播接收器所需要注册的一个唯一action**/
    private static final String ACTION_SENDING_HEARTBEAT = "CN.JIGUANG.IOT.HEARTBEAT";
    /**业务心跳时间间隔为270 秒**/
    public static final int HEARTBEAT_INTERVAL = 270 * 1000;

    /**
     * 构造方法
     * @param context 需要传入应用上下文
     */
    public JiotAlarmHeartBeat(Context context){
        this.context = context;
    }

    /**
     * 定时器初始化
     * @param mqttConnection 传入mqtt操作对象
     */
    public void init(MqttConnection mqttConnection){
        this.mqttConnection = mqttConnection;
        this.jiotAlarmHeartBeatReceiver = new JiotAlarmHeartBeat.JiotAlarmHeartBeatReceiver();
    }

    /**
     * 启动当前定时任务
     */
    public void start() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_SENDING_HEARTBEAT);
        JiotLogger.d("Register JiotAlarmHeartBeatReceiver to Context " + mqttConnection.getClientId());
        if (context != null && jiotAlarmHeartBeatReceiver != null) {
            context.registerReceiver(jiotAlarmHeartBeatReceiver, intentFilter);
        }
        pendingIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_SENDING_HEARTBEAT), PendingIntent.FLAG_UPDATE_CURRENT);
        //执行AlarmManager系统服务
        scheduleHeartbeat(HEARTBEAT_INTERVAL);

    }

    /**
     * 停止当前定时任务
     */
    public void stop() {
        if(hasStarted){
            JiotLogger.d("Unregister JiotAlarmHeartBeatReceiver to Context " + mqttConnection.getClientId());
            if(pendingIntent != null){
                AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                if (alarmManager != null) {
                    alarmManager.cancel(pendingIntent);
                }
            }
            hasStarted = false;
            try{
                context.unregisterReceiver(jiotAlarmHeartBeatReceiver);
            }catch(IllegalArgumentException e){
                //Ignore unregister errors.
            }
        }
    }

    /**
     * 将定时任务设置给系统
     * @param delayInMilliseconds 下一次任务运行的时间间隔
     */
    public void scheduleHeartbeat(long delayInMilliseconds) {
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
    class JiotAlarmHeartBeatReceiver extends BroadcastReceiver {

        private PowerManager.WakeLock wakelock;

        private final String wakeLockTag = "Jiot.HeartBeat.Receiver." + mqttConnection.getClientId();

        @Override
        @SuppressLint({"Wakelock", "WakelockTimeout"})
        public void onReceive(Context context, Intent intent) {

            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                wakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, wakeLockTag);
                wakelock.acquire();
                try{
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
                    mqttConnection.publish(mqttContext,String.format(MqttConstans.JMQTT_TOPIC_IOT_PING_REQ,mqttConnection.getDeviceInfo().getProductKey(),mqttConnection.getDeviceInfo().getDeviceName()),message);
                }finally {
                    wakelock.release();
                }
            }




        }
    }

}
