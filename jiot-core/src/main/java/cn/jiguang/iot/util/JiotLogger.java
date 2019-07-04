package cn.jiguang.iot.util;

import android.util.Log;
import cn.jiguang.iot.JiotClient;

/**
 * @author : ouyangshengduo
 * e-mail : ouysd@jiguang.cn
 * date  : 2019/4/19 9:34
 * desc : 日志工具类
 */
public class JiotLogger {

    private static final String TAG = "JIGUANG_IOT";

    /**
     * 日志开关
     */
    private static boolean DEBUG = true;

    public static void d(String msg) {
        trace(Log.DEBUG ,msg);
    }

    public static void i(String msg) {
        trace(Log.INFO , msg);
    }

    public static void e(String msg) {
        trace(Log.ERROR,msg);
    }

    public static void e(String msg, Throwable tr) {
        trace(msg, tr);
    }


    /**
     * 根据设置的日志级别，判断是否打印日志
     * @param type 日志类型
     * @param msg 日志内容
     */
    private static void trace(final int type, final String msg) {
        if(DEBUG) {
            switch (type) {
                case Log.DEBUG:
                    if (JiotClient.logLevel >= Log.DEBUG) {
                        Log.d(TAG, msg);
                    }
                    break;
                case Log.INFO:
                    if (JiotClient.logLevel >= Log.VERBOSE) {
                        Log.i(TAG, msg);
                    }
                    break;
                case Log.ERROR:
                    if (JiotClient.logLevel >= 1) {
                        Log.e(TAG, msg);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * 根据设置的日志级别，判断是否打印日志，支持异常打印
     * @param msg 日志内容
     * @param tr 异常信息
     */
    private static void trace(final String msg, final Throwable tr) {
        if (DEBUG) {
            if(JiotClient.logLevel >= 1) {
                Log.e(TAG, msg + tr.getMessage());
            }
        }
    }

}
