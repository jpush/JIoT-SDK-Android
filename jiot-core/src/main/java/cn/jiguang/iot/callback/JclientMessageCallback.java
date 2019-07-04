package cn.jiguang.iot.callback;

import cn.jiguang.iot.bean.EventReportRsp;
import cn.jiguang.iot.bean.MsgDeliverReq;
import cn.jiguang.iot.bean.PropertyReportRsp;
import cn.jiguang.iot.bean.PropertySetReq;
import cn.jiguang.iot.bean.VersionReportRsp;

/**
 * @author : ouyangshengduo
 * e-mail : ouysd@jiguang.cn
 * date  : 2019/5/7 10:29
 * desc : 与服务器mqtt消息交互的回调
 */
public interface JclientMessageCallback {

    /**
     * 处理服务端返回JIOT客户端上报设备属性请求回复
     * @param propertyReportRsp 服务器回复的属性信息
     * @param errorCode 错误码
     */
    void jiotPropertyReportRsp(PropertyReportRsp propertyReportRsp, int errorCode);

    /**
     * 处理服务端返回JIOT客户端上报设备事件请求回复
     * @param eventReportRsp 服务器回复的设备事件信息
     * @param errorCode 错误码
     */
    void jiotEventReportRsp(EventReportRsp eventReportRsp, int errorCode);

    /**
     * 处理服务端返回JIOT客户端上报设备版本请求回复
     * @param versionReportRsp 服务器回复的设备版本信息
     * @param errorCode 错误码
     */
    void jiotVersionReportRsp(VersionReportRsp versionReportRsp, int errorCode);

    /**
     * 服务端下发给JIOT客户端设备属性设置请求
     * @param propertySetReq 服务器回复的设置设备属性
     * @param errorCode 错误码
     */
    void jiotPropertySetReq(PropertySetReq propertySetReq, int errorCode);

    /**
     * 服务端下发给JIOT客户端消息下发请求
     * @param msgDeliverReq 客户端接收属性设置消息
     * @param errorCode 错误码
     */
    void jiotMsgDeliverReq(MsgDeliverReq msgDeliverReq, int errorCode);

}
