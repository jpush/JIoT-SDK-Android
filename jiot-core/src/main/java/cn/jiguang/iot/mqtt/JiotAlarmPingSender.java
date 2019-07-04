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
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttPingSender;
import org.eclipse.paho.client.mqttv3.internal.ClientComms;
import cn.jiguang.iot.util.JiotLogger;

/**
 * @author : ouyangshengduo
 * e-mail : ouysd@jiguang.cn
 * date  : 2019/5/9 16:36
 * desc : mqtt自身的协议栈心跳类
 */
public class JiotAlarmPingSender implements MqttPingSender {

    /**应用的上下文对象**/
    private Context context;
    /**定时所需要的闹钟接收广播**/
    private BroadcastReceiver jiotAlarmPingSenderReceiver;
    /**声明将来要执行的intent对象**/
    private PendingIntent pendingIntent;
    /**标志当前定时任务是否已经开启**/
    private volatile boolean hasStarted = false;
    /**广播接收器所需要注册的一个唯一action**/
    private static final String ACTION_PING_SENDER= "CN.JIGUANG.IOT.PING_SENDER";
    /**mqtt操作对象**/
    private ClientComms mComms;
    /**当前自身对象的引用**/
    private JiotAlarmPingSender that;

    JiotAlarmPingSender(Context context) {
        this.context = context;
        that = this;
    }

    @Override
    public void init(ClientComms comms) {
        this.mComms = comms;
        this.jiotAlarmPingSenderReceiver = new JiotAlarmPingSenderReceiver();
    }

    @Override
    public void start() {
        JiotLogger.d("Register JiotAlarmPingSenderReceiver to Context " + ACTION_PING_SENDER);
        if (context != null && jiotAlarmPingSenderReceiver != null) {
            context.registerReceiver(jiotAlarmPingSenderReceiver, new IntentFilter(ACTION_PING_SENDER));
        }

        pendingIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_PING_SENDER), PendingIntent.FLAG_UPDATE_CURRENT);
        schedule(mComms.getKeepAlive());

    }

    @Override
    public void stop() {
        if(hasStarted){
            JiotLogger.d("Unregister JiotAlarmPingSenderReceiver to Context " + mComms.getClient().getClientId());
            if(pendingIntent != null){
                // Cancel Alarm.
                AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                if (alarmManager != null) {
                    alarmManager.cancel(pendingIntent);
                }
            }

            hasStarted = false;
            try{
                context.unregisterReceiver(jiotAlarmPingSenderReceiver);
            }catch(IllegalArgumentException e){
                //Ignore unregister errors.
            }
        }
    }

    @Override
    public void schedule(long delayInMilliseconds) {
        long nextAlarmInMilliseconds = System.currentTimeMillis() + delayInMilliseconds;
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            // In SDK 23 and above, dosing will prevent setExact, setExactAndAllowWhileIdle will force
            // the device to run this task whilst dosing.
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
     * PingReq发送类
     */
    class JiotAlarmPingSenderReceiver extends BroadcastReceiver {

        private PowerManager.WakeLock wakelock;

        private final String wakeLockTag = "Jiot.client" + that.mComms.getClient().getClientId();

        @Override
        @SuppressLint({"Wakelock", "WakelockTimeout"})
        public void onReceive(Context context, Intent intent) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                wakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, wakeLockTag);
                wakelock.acquire();
                IMqttToken token = mComms.checkForActivity(new IMqttActionListener() {

                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        JiotLogger.d("Success. Release lock(" + wakeLockTag + "):" + System.currentTimeMillis());
                        //Release wakelock when it is done.
                        wakelock.release();
                    }

                    @Override

                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        JiotLogger.d( "Failure. Release lock(" + wakeLockTag + "):" + System.currentTimeMillis());
                        //Release wakelock when it is done.
                        wakelock.release();
                    }
                });


                if (token == null && wakelock.isHeld()) {
                    wakelock.release();
                }
            }
        }
    }

}
