package cn.jiguang.iot.sis;

/**
 * @author : ouyangshengduo
 * e-mail : ouysd@jiguang.cn
 * date  : 2019/6/25 11:05
 * desc : 网络请求回调
 */
public interface IHttpCallback {

    // 网络请求成功

    /**
     * 网络请求成功
     * @param response 请求结果
     */
    void onFinish(String response);

    /**
     * 网络请求失败
     * @param e 异常
     */
    void onError(Exception e);
}
