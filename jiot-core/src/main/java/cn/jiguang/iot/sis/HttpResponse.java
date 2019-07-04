package cn.jiguang.iot.sis;

/**
 * @author : ouyangshengduo
 * e-mail : ouysd@jiguang.cn
 * date  : 2019/6/25 10:06
 * desc : http回复封装
 */
class HttpResponse {

    /**状态码**/
    private boolean success;
    /**包的内容**/
    private String body;
    /**异常内容**/
    private Exception exception;

    HttpResponse(){
    }

    HttpResponse(boolean success, String body) {
        this.success = success;
        this.body = body;
    }

    HttpResponse(boolean success, Exception e) {
        this.success = success;
        this.exception = e;
    }

    boolean isSuccess() {
        return success;
    }

    String getBody() {
        return body;
    }

    Exception getException() {
        return exception;
    }
}
