package cn.jiguang.iot.sis;

/**
 * @author : ouyangshengduo
 * e-mail : ouysd@jiguang.cn
 * date  : 2019/5/7 17:29
 * desc : sis请求结果回调
 */
public abstract class AbstractSisHandleCallback {

    /**
     * sis请求失败回调
     * @param host 请求服务器地址
     * @param throwable 请求异常
     */
    public abstract void onFailure(String host, Throwable throwable);

    /**
     * sis请求成功回调
     * @param host 请求服务器地址
     * @param response 返回结果内容
     */
    public abstract void onResponse(String host, String response);
}
