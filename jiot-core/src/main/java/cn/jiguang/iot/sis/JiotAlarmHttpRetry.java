package cn.jiguang.iot.sis;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.PowerManager;
import cn.jiguang.iot.util.JiotLogger;

/**
 * @author : ouyangshengduo
 * e-mail : ouysd@jiguang.cn
 * date  : 2019/5/9 16:36
 * desc : Sis重试定时器
 */
public class JiotAlarmHttpRetry {

    /**应用的上下文对象**/
    private Context context;
    /**sis请求操作对象**/
    private SisConnection sisConnection;
    /**定时所需要的闹钟接收广播**/
    private BroadcastReceiver jiotAlarmHttpRetryReceiver;
    /**声明将来要执行的intent对象**/
    private PendingIntent pendingIntent;
    /**标志当前定时任务是否已经开启**/
    private volatile boolean hasStarted = false;
    /**初始http重试时间间隔为10秒**/
    private static final int DEFALIT_HTTPRETRY_INTERVAL = 10 * 1000;
    /**广播接收器所需要注册的一个唯一action**/
    private static final String ACTION_HTTP_RETRY = "CN.JIGUANG.IOT.HTTPRETRY";
    /**
     * 判断当前定时器是否已经启动
     * @return true代表已经启动，false代表没有启动
     */
    public boolean isHasStarted() {
        return hasStarted;
    }

    /**
     * 构造方法
     * @param context 需要传入应用上下文
     */
    public JiotAlarmHttpRetry(Context context){
        this.context = context;
    }

    /**
     * 定时器初始化
     * @param sisConnection 传入sis操作对象
     */
    public void init(SisConnection sisConnection){
        this.sisConnection = sisConnection;
        this.jiotAlarmHttpRetryReceiver = new JiotAlarmHttpRetry.JiotAlarmHttpRetryReceiver();
    }

    /**
     * 启动当前定时任务
     */
    public void start() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_HTTP_RETRY);
        JiotLogger.d("Register JiotAlarmHttpRetryReceiver to Context ");
        if (context != null && jiotAlarmHttpRetryReceiver != null) {
            context.registerReceiver(jiotAlarmHttpRetryReceiver, intentFilter);
        }
        pendingIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_HTTP_RETRY), PendingIntent.FLAG_UPDATE_CURRENT);
        scheduleHttpRetry(DEFALIT_HTTPRETRY_INTERVAL);

    }

    /**
     * 停止当前定时任务
     */
    public void stop() {
        if(hasStarted){
            JiotLogger.d("Unregister JiotAlarmHttpRetryReceiver to Context ");
            if(pendingIntent != null){
                AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                if (alarmManager != null) {
                    alarmManager.cancel(pendingIntent);
                }
            }
            hasStarted = false;
            try{
                context.unregisterReceiver(jiotAlarmHttpRetryReceiver);
            }catch(IllegalArgumentException e){
                //忽略注销的异常
            }
        }
    }

    /**
     * 将定时任务设置给系统
     * @param delayInMilliseconds 下一次任务运行的时间间隔
     */
    public void scheduleHttpRetry(long delayInMilliseconds) {
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
    class JiotAlarmHttpRetryReceiver extends BroadcastReceiver {

        private PowerManager.WakeLock wakelock;

        private final String wakeLockTag = "Jiot.HttpRetry.Receiver." + System.currentTimeMillis();

        @Override
        @SuppressLint({"Wakelock", "WakelockTimeout"})
        public void onReceive(Context context, Intent intent) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                wakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, wakeLockTag);
                wakelock.acquire();
                try{
                    SisRequestOptions sisRequestOptions = new SisRequestOptions();
                    sisRequestOptions.setHost(SisContants.HOST);
                    sisRequestOptions.setProductKey(sisConnection.getProductKey());
                    sisRequestOptions.setShouldRetry(true);
                    sisRequestOptions.setRequestProtocolType(sisConnection.getSisRequestProtocolType());
                    sisConnection.request(sisRequestOptions);
                }finally {
                    wakelock.release();
                }
            }
        }
    }

}
