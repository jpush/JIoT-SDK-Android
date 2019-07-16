package cn.jiguang.iot.sis;
import cn.jiguang.iot.util.JiotConstant;

/**
 * @author : ouyangshengduo
 * e-mail : ouysd@jiguang.cn
 * date  : 2019/5/8 9:44
 * desc : sis请求参数
 */
public class SisRequestOptions {

    /**请求协议类型，0代表http,1代表https**/
    private int requestProtocolType;
    /**请求的服务器地址**/
    private String host;
    /**请求url中的参数productKey的值**/
    private String productKey;
    /**标志当前请求是否需要失败再重试一次然后再回调**/
    private boolean shouldRetry;

    boolean isShouldRetry() {
        return shouldRetry;
    }

    public void setShouldRetry(boolean shouldRetry) {
        this.shouldRetry = shouldRetry;
    }

    int getRequestProtocolType() {
        return requestProtocolType;
    }

    public void setRequestProtocolType(int requestProtocolType) {
        this.requestProtocolType = requestProtocolType;
    }

    String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    String getProductKey() {
        return productKey;
    }

    public void setProductKey(String productKey) {
        this.productKey = productKey;
    }

    /**
     *
     * @return 返回完整的url
     */
    String getHttpUrl() {

        if(null == host) {
            host = SisContants.HOST;
        }
        //样例：https://113.31.131.59/v1/addrget?product_key=''&protocol_type=1
        if(requestProtocolType == JiotConstant.SIS_PROTOCOL_TYPE_TCP){
            return "http://" + getHost() + SisContants.URI;
        }else{
            return "https://" + getHost() + SisContants.URI;
        }
    }

    String getHttpParams(){
        return "product_key=" + productKey + "&protocol_type=" + requestProtocolType;
    }
}
