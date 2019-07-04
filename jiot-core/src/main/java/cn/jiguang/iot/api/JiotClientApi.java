package cn.jiguang.iot.api;

import android.content.Context;

import cn.jiguang.iot.bean.DeviceInfo;
import cn.jiguang.iot.bean.EventReportReq;
import cn.jiguang.iot.bean.JiotResult;
import cn.jiguang.iot.bean.PropertyReportReq;
import cn.jiguang.iot.bean.VersionReportReq;
import cn.jiguang.iot.callback.JclientHandleCallback;
import cn.jiguang.iot.callback.JclientMessageCallback;

/**
 * @author : ouyangshengduo
 * e-mail : ouysd@jiguang.cn
 * date  : 2019/5/7 17:02
 * desc : SDK 开放的接口类
 */
public interface JiotClientApi {

    /**
     * 创建JIOT客户端
     * @param context 应用上下文
     * @param isUseSsl 是否使用加密通道SSL
     */
    void jiotInit(Context context,boolean isUseSsl);

    /**
     *
     * 连接MQTT服务器
     * @param deviceInfo 设备信息，三元组
     * @param messageCallback 客户端处理下行报文的回调函数结构体
     * @param handleCallback sdk连接状态回调函数结构体
     * @return 实时连接结果
     */
    int jiotConn(DeviceInfo deviceInfo, JclientMessageCallback messageCallback,
                 JclientHandleCallback handleCallback);

    /**
     * 断开mqtt连接
     */
    void jiotDisConn();

    /**
     * 销毁JIOT客户端
     */
    void jiotRelease();

    /**
     * 查询JIOT客户端的连接状态
     * @return int  jClient的状态
     */
    int jiotGetConnStatus();

    /**
     * JIOT客户端上报设备属性
     * @param properyReport 设备属性
     * @return 客户端实时返回结果
     */
    JiotResult jiotPropertyReportReq(PropertyReportReq properyReport);

    /**
     * JIOT客户端上报事件请求
     * @param eventReportReq 上报事件
     * @return 客户端实时返回结果
     */
    JiotResult jiotEventReportReq(EventReportReq eventReportReq);

    /**
     * JIOT客户端上报设备版本请求
     * @param versionReportReq 设备版本
     * @return 客户端实时返回结果
     */
    JiotResult jiotVersionReportReq(VersionReportReq versionReportReq);

    /**
     * 设置日志级别，默认不输出日志，调试用
     * @param level 日志等级
     */
    void jiotSetLogLevel(int level);
}
