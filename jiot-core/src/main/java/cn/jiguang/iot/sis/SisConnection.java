package cn.jiguang.iot.sis;
import cn.jiguang.iot.util.JiotLogger;

/**
 * @author : ouyangshengduo
 * e-mail : ouysd@jiguang.cn
 * date  : 2019/5/7 17:25
 * desc : sis服务连接
 */
public class SisConnection {

    /**sis请求结果回调**/
    private AbstractSisHandleCallback sisHandleCallback;
    /**设备三元组中的产品Key**/
    private String productKey;
    /**sis请求的协议类型，有http和https**/
    private int sisRequestProtocolType = 1;

    private HttpUtils httpUtils;

    /**
     * 构造方法
     * @param sisHandleCallback 传入外界的回调对象
     */
    public SisConnection(AbstractSisHandleCallback sisHandleCallback){
        this.sisHandleCallback = sisHandleCallback;
    }

    /**
     * 获取三元组中的产品Key
     * @return 返回产品Key字符串
     */
    String getProductKey() {
        return productKey;
    }

    /**
     * 获取请求的协议类型，http还是https
     * @return 0代表http,1代表https
     */
    int getSisRequestProtocolType() {
        return sisRequestProtocolType;
    }

    /**
     * 开放调用的请求方法
     * @param options 请求所需要的参数
     */
    public void request(final SisRequestOptions options){
        this.productKey = options.getProductKey();
        this.sisRequestProtocolType = options.getRequestProtocolType();

        JiotLogger.d("requesting url [ " + options.getHttpUrl() + "?" + options.getHttpParams() + " ]");
        if(null == httpUtils) {
            httpUtils = HttpUtils.getHttpUtil(options.getHttpUrl(), options.getHttpParams());
        }else {
            httpUtils.updateHttpUrlAndParams(options.getHttpUrl(),options.getHttpParams());
        }
        httpUtils.httpGet( new IHttpCallback() {
            @Override
            public void onFinish(String response) {
                //请求成功
                if (null != sisHandleCallback) {
                    JiotLogger.d("sis response : " + response);
                    sisHandleCallback.onResponse(options.getHost(), response);
                }
            }

            @Override
            public void onError(Exception throwable) {
                //请求失败
                if (null != sisHandleCallback) {
                    sisHandleCallback.onFailure(options.getHost(), throwable);
                }
            }
        },options.isShouldRetry());
    }

}
