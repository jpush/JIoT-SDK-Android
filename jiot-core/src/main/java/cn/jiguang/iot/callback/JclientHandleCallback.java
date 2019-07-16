package cn.jiguang.iot.callback;

/**
 * @author : ouyangshengduo
 * e-mail : ouysd@jiguang.cn
 * date  : 2019/5/7 10:29
 * desc : 客户端连接状态回调
 */
public interface JclientHandleCallback {

    /**
     * 客户端mqtt连接成功
     */
    void jiotConnectedHandle();

    /**
     * 客户端mqtt连接失败
     * @param errorCode JIOT客户端异常错误码
     */
    void jiotConnectFailHandle(int errorCode);

    /**
     * 客户端mqtt连接中断，在此之前连接成功过
     * @param errorCode JIOT客户端异常错误码
     * @param msg 断开的原因
     */
    void jiotDisconnectHandle(int errorCode,String msg);

    /**
     * 客户端subscirbe失败，ACL不通过
     * @param topicFilter JIOT订阅的topic
     */
    void jiotSubscribeFailHandle(String topicFilter);

    /**
     * 客户端publish失败，ACL不通过
     * @param seqNo 消息序号
     */
    void jiotPublishFailHandle(long seqNo);

    /**
     * 客户端消息发送超时
     * @param seqNo 消息序号
     */
    void jiotMessageTimeoutHandle(long seqNo);
}
