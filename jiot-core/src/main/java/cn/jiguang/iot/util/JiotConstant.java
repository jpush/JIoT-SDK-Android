package cn.jiguang.iot.util;

/**
 * @author : ouyangshengduo
 * e-mail : ouysd@jiguang.cn
 * date  : 2019/4/19 9:16
 * desc : SDK所需要的常量
 */
public interface JiotConstant {

    /**
     * SDK支持TCP与SSL两种方式
     * 0代表没有加密的通道，相应的sis走http请求
     * 1代表ssl加密通道，相应的sis走https请求
     */
    int SIS_PROTOCOL_TYPE_TCP = 0;
    int SIS_PROTOCOL_TYPE_SSL = 1;

    /**
     * 标志当前SDK的版本
     */
    String SDK_VERSION = "1.0.2";
    /**
     * 标志当前版本的SDK的编译序号
     */
    int SDK_BUILDID = 8;

    /**
     * api调用的日志所需要
     */
    String INIT_LOG = "jiotInit()|SDK_VERSION[%s] SDK_BUILDID[%d]";
    String CONN_LOG = "jiotConn()| enter method .";
    String DISCONN_LOG = "jiotDisConn()| enter method .";
    String RELEASE_LOG = "jiotRelease()| enter method .";
    String GET_CONN_STATUS_LOG = "jiotGetConnStatus()| enter method .";
    String REPORT_PROPERTY_LOG = "jiotPropertyReportReq()| enter method .";
    String REPORT_EVENT_LOG = "jiotEventReportReq()| enter method .";
    String REPORT_VERSION_LOG = "jiotVersionReportReq()| enter method .";
    String SET_LOG_LEVEL_LOG = "jiotSetLogLevel()| enter method . log level[%d]";

    /**
     * 定义产品Key字符串最大长度  24
     * 定义设备名称字符串最大长度 24
     * 定义设备密钥字符串最大长度 24
     * 定义事件名称字符串的最大长度 32
     * 定义事件内容字符串的最大长度2048
     */
    int PRODUCT_KEY_MAX_LEN = 24;
    int DEVICE_NAME_MAX_LEN = 24;
    int DEVICE_SECRET_MAX_LEN = 24;
    int EVENT_NAME_MAX_LEN = 32;
    int EVENT_CONTENT_MAX_LEN = 2048;
    int VERSION_MAX_LEN = 64;

    /**
     * 客户端的所有状态
     */
    enum CLIENT_STATUS{
        /**
         * 对象未实例化
         */
        NONE,
        /**
         * 已经初始化完成
         */
        CLIENT_INITIALIZED,
        /**
         * 连接中
         */
        CLIENT_CONNECTING,
        /**
         * 已连接
         */
        CLIENT_CONNECTED,
        /**
         * 断开中
         */
        CLIENT_DISCONNECTING,
        /**
         * 已断开
         */
        CLIENT_DISCONNECTED,
        /**
         * 重连中
         */
        CLIENT_RECONNECTING
    }

}
