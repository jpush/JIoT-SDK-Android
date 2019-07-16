package cn.jiguang.example.ui;

/**
 * @author: ouyangshengduo
 * e-mail: ouysd@jiguang.cn
 * date  : 2019/4/16 13:35
 * desc  :
 */
class Constant {

    /**
     * 连接状态枚举值
     */
    static final String []CONN_STATUS = {"NONE",
            "CLIENT_INITIALIZED",
            "CLIENT_CONNECTING",
            "CLIENT_CONNECTED",
            "CLIENT_DISCONNECTING",
            "CLIENT_DISCONNECTED",
            "CLIENT_RECONNECTING"};
    /**
     * 连接成功处理
     */
    static final int CONNECTED_HANDLE = 1000;
    /**
     * 连接失败处理
     */
    static final int CONNECT_FAILE_HANDLE = 1001;
    /**
     * 获取连接状态处理
     */
    static final int GET_CONN_STATUS = 1002;
    /**
     * 客户端收到消息后的处理
     */
    static final int MSG_DELIVER_REQ = 1003;
    /**
     * 客户端端口处理
     */
    static final int DISCONNECT_HANDLE = 1004;
    /**
     * 订阅失败处理
     */
    static final int SUBSCRIBE_FAIL_HANDLE = 1005;
    /**
     * 消息超时处理
     */
    static final int MESSAGE_TIME_OUT_HANDLE = 1006;
    /**
     * 发布失败处理
     */
    static final int PUBLISH_FAIL_HANDLE = 1007;
    /**
     * 上报版本后回复的处理
     */
    static final int REPORT_VERSION_RESPONSE = 1008;
    /**
     * 上报属性后回复的处理
     */
    static final int REPORT_PROPERTY_RESPONSE = 1009;
    /**
     * 上报事件回复的处理
     */
    static final int REPORT_EVENT_RESPONSE = 1010;
    /**
     * 客户端收到设置设备属性的处理
     */
    static final int PROPERTY_SET_REQ = 1011;
    /**
     * 客户端连接中
     */
    static final int CONNECTING = 1013;
    /**
     * 客户端断开连接中
     */
    static final int DISCONNECTING = 1014;
    /**
     * 实时连接返回状态处理
     */
    static final int CONNECT_RESPONSE = 1015;

    /**
     * 默认产品key
     */
    static final String DEFAULT_PRODUCT_KEY = "";

    /**
     * 默认产品名称
     */
    static final String DEFAULT_DEVICE_NAME = "";
    /**
     * 默认设备密钥
     */
    static final String DEFAULT_DEVICE_SECRET = "";
    /**
     * 默认设备属性
     */
    static final String DEFAULT_DEVICE_PROPERTY = "";
    /**
     * 设备上传的内容
     */
    static final String DEFAULT_REPORT_CONTENT = "jiguang test content";

    static final String DEFAULT_REPORT_VERSION = "V1.0.2";
}
