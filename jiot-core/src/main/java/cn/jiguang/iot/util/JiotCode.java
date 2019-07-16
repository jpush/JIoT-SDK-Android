package cn.jiguang.iot.util;

/**
 * @author : ouyangshengduo
 * e-mail : ouysd@jiguang.cn
 * date  : 2019/5/17 9:55
 * desc : 错误码归集
 */
public class JiotCode {

    /**
     * 成功
     */
    public static final int JIOT_SUCCESS = 0;

    /**
     * mqtt 连接错误
     */
    public static final int JIOT_ERR_MQTT_CONNECT_ERROR = 11028;

    /**
     * mqtt 网络连接错误
     */
    public static final int JIOT_ERR_MQTT_NETWORK_CONNECT_ERROR = 11027;

    /**
     * sis的http请求错误，一般是由于客户端设备网络异常
     */
    public static final int JIOT_ERR_SIS_HTTP_FAIL = 14001;

    /**
     * sis的http请求返回的数据内容有误
     */
    public static final int JIOT_ERR_SIS_CONTENT_ERROR = 14002;

    /**
     * sis的http请求返回的数据json格式有误，导致解析异常
     */
    public static final int JIOT_ERR_SIS_JSON_PARSE_FAIL = 14003;

    /**
     * mqtt数据交互中，获取的json格式有误，导致解析异常
     */
    public static final int  JIOT_ERR_JCLI_JSON_PARSE_ERR = 12002;

    /**
     * product_key超过长度
     */
    public static final int JIOT_ERR_PRODUCTKEY_OVERLONG = 10002;

    /**
     * deviceName超过长度
     */
    public static final int JIOT_ERR_DEVICENAME_OVERLONG = 10003;

    /**
     * productKey超过长度
     */
    public static final int JIOT_ERR_DEVICESECRET_OVERLONG = 10004;

    /**
     * 序号错误
     */
    public static final int JIOT_ERR_SEQNO_ERROR = 10006;

    /**
     * 参数异常
     */
    public static final int JIOT_ERR_ARGU_FORMAT_ERROR = 10009;

    /**
     * 版本格式异常
     */
    public static final int JIOT_ERR_VERSION_FORMAT_ERROR = 10010;

    /**
     * 属性格式异常
     */
    public static final int JIOT_ERR_PROPERTY_FORMAT_ERROR = 10011;

    /**
     * 属性名称格式异常
     */
    public static final int JIOT_ERR_PROPERTY_NAME_FORMAT_ERROR = 10012;

    /**
     * 属性值格式异常
     */
    public static final int JIOT_ERR_PROPERTY_VALUE_FORMAT_ERROR = 10013;

    /**
     * 事件格式异常
     */
    public static final int JIOT_ERR_EVENT_FORMAT_ERROR = 10014;

    /**
     * 事件名异常
     */
    public static final int JIOT_ERR_EVENT_NAME_FORMAT_ERROR = 10015;

    /**
     * 事件内容异常
     */
    public static final int JIOT_ERR_EVENT_CONTENT_FORMAT_ERROR = 10016;

    /**
     * 版本格式异常
     */
    public static final int JIOT_ERR_VERSION_APP_VAR_FORMAT_ERROR = 10018;

    /**
     * mqtt状态异常
     */
    public static final int JIOT_ERR_MQTT_STATE_ERROR = 11013;

    /**
     * MQTT断开连接
     */
    public static final int JIOT_ERR_DISCONNECT_ERROR = 11033;

}
