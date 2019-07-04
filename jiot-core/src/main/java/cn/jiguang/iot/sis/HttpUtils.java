package cn.jiguang.iot.sis;

import android.accounts.NetworkErrorException;
import android.annotation.SuppressLint;
import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import cn.jiguang.iot.util.JiotLogger;

/**
 * @author : ouyangshengduo
 * e-mail : ouysd@jiguang.cn
 * date  : 2019/6/25 10:06
 * desc : http工具类
 */
public class HttpUtils {

    /**回调**/
    private IHttpCallback httpCallback;
    /**请求主url**/
    private String url;
    /**请求参数**/
    private String stringParams;

    public enum HTTP_TYPE {
        /**get请求标志**/
        GET,
        /**post请求标志**/
        POST
    }

    public enum PROTOCOL_TYPE {
        /**http协议**/
        HTTP,
        /**https协议**/
        HTTPS
    }

    /**http,https协议标志**/
    private static final String HTTPS_STRING = "https";
    private static final String HTTP_STRING = "http";

    /**
     * url 和 参数的分隔符
     **/
    private static final String URL_AND_PARA_SEPARATOR = "?";

    /**尝试次数**/
    private static final int TRY_TWO_TIMES = 2;
    private static final int TRY_ONE_TIME = 1;

    private static HTTP_TYPE httpType;
    private static PROTOCOL_TYPE protocolType;

    /**
     * 获取http操作对象
     * @param url url
     * @param stringParams 参数
     * @return 返回操作对象
     */
    static HttpUtils getHttpUtil(final String url, final String stringParams) {
        return new HttpUtils(url, stringParams);
    }

    /**
     * 构造方法
     * @param url url
     * @param stringParams 参数
     */
    private HttpUtils(final String url, final String stringParams) {
        this.url = url;
        this.stringParams = stringParams;
        // 判断是http请求还是https请求
        try {
            URL httpUrl = new URL(url);
            if (httpUrl.getProtocol().toLowerCase().equals(HTTPS_STRING)) {
                protocolType = PROTOCOL_TYPE.HTTPS;
            } else if (httpUrl.getProtocol().toLowerCase().equals(HTTP_STRING)) {
                protocolType = PROTOCOL_TYPE.HTTP;
            }
        }
        catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 更新请求url以及参数
     * @param url url
     * @param paramsString 参数
     */
    void updateHttpUrlAndParams(String url, String paramsString){
        this.url = url;
        this.stringParams = paramsString;
    }

    /**
     *  执行get方法
     * @param callback 回调
     * @param shouldRetry 是否需要重试
     */
    void httpGet(IHttpCallback callback, boolean shouldRetry) {
        this.httpCallback = callback;
        httpType = HTTP_TYPE.GET;
        if (!url.contains(URL_AND_PARA_SEPARATOR)) {
            url = url + URL_AND_PARA_SEPARATOR + stringParams;
        } else if (url.substring(url.length() - 1).equals(URL_AND_PARA_SEPARATOR)) {
            url = url + stringParams;
        }
        if(shouldRetry) {
            httpAccess(TRY_TWO_TIMES);
        }else{
            httpAccess(TRY_ONE_TIME);
        }
    }

    /**
     * 执行post方法
     * @param shouldRetry 是否需要重试
     */
    public void httpPost(boolean shouldRetry) {
        httpType = HTTP_TYPE.POST;
        if(shouldRetry) {
            httpAccess(TRY_TWO_TIMES);
        }else{
            httpAccess(TRY_ONE_TIME);
        }
    }

    /**
     * 执行http请求
     * @param tryTimes 尝试次数
     */
    private void httpAccess(int tryTimes) {

        try {
            new HttpTask(httpCallback, httpType, protocolType, stringParams,tryTimes)
                    .execute(url);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

}

/**
 * 默认证书工具
 */
class DefaultTrustManager implements X509TrustManager {

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return null;
    }

    @SuppressLint("TrustAllX509TrustManager")
    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) { }

    @SuppressLint("TrustAllX509TrustManager")
    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) { }
}


/**
 * http同步任务
 */
@SuppressLint("NewApi")
class HttpTask extends AsyncTask<String, Void, HttpResponse> {
    /**回调**/
    private IHttpCallback httpCallback;
    /**http请求类型，get/post**/
    private HttpUtils.HTTP_TYPE httpType;
    /**协议类型，http/https**/
    private HttpUtils.PROTOCOL_TYPE protocolType;
    /**
     * 建立连接超时时间 10s
     */
    private static final int CONNECTION_TIMEOUT = 10000;
    /**
     * 数据传输超时时间 20s
     */
    private static final int READ_TIMEOUT = 20000;
    /**请求参数**/
    private String stringParams;
    /**编码方式**/
    private static final String ENCODING_UTF_8 = "UTF-8";
    /**尝试次数**/
    private int retryTimes;


    /**
     * 构造方法
     * @param callback 回调
     * @param type 请求类型
     * @param protocolType 协议类型
     * @param params 参数
     * @param retryTimes 重试次数
     */
    HttpTask(IHttpCallback callback, HttpUtils.HTTP_TYPE type, HttpUtils.PROTOCOL_TYPE protocolType, String params, int retryTimes) {
        super();
        httpCallback = callback;
        httpType = type;
        stringParams = params;
        this.protocolType = protocolType;
        this.retryTimes = retryTimes;
    }

    /**默认证书工具**/
    private static TrustManager[] defaultTrustManagers = new DefaultTrustManager[] { new DefaultTrustManager() };

    /**
     * 信任所有主机-对于任何证书都不做检查
     */
    private static void trustAllHosts() {
        try {
            SSLContext sc = SSLContext.getInstance("TLSv1.2");
            sc.init(null, defaultTrustManagers, null);
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    /**服务器名称校验，默认不进行校验**/
    private static HostnameVerifier DO_NOT_VERIFY = new HostnameVerifier() {
        @SuppressLint("BadHostnameVerifier")
        @Override
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    };

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected HttpResponse doInBackground(String... urls) {

        if (urls == null || urls.length == 0) {
            return null;
        }
        int currentTryTime = 1;
        HttpResponse httpResponse = new HttpResponse();
        while(currentTryTime <= retryTimes) {
            StringBuilder result = new StringBuilder();
            HttpURLConnection httpUrlCon = null;
            try {
                URL httpUrl = new URL(urls[0]);
                switch (protocolType) {
                    case HTTP:
                        httpUrlCon = (HttpURLConnection) httpUrl.openConnection();
                        break;
                    case HTTPS:
                        trustAllHosts();
                        httpUrlCon = (HttpsURLConnection) httpUrl.openConnection();
                        // 不进行主机名确认
                        ((HttpsURLConnection) httpUrlCon).setHostnameVerifier(DO_NOT_VERIFY);
                        break;
                    default:
                        break;
                }

                // set  http  configure
                // 建立连接超时时间
                httpUrlCon.setConnectTimeout(CONNECTION_TIMEOUT);
                //数据传输超时时间，很重要，必须设置。

                httpUrlCon.setReadTimeout(READ_TIMEOUT);
                // 向连接中写入数据
                httpUrlCon.setDoInput(true);
                // 禁止缓存
                httpUrlCon.setUseCaches(false);
                httpUrlCon.setInstanceFollowRedirects(true);
                httpUrlCon.setRequestProperty("Charset", "UTF-8");
                httpUrlCon.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                switch (httpType) {
                    case GET:
                        // 设置请求类型为
                        httpUrlCon.setRequestMethod("GET");
                        break;
                    case POST:
                        // 设置请求类型为
                        httpUrlCon.setRequestMethod("POST");
                        // 获取输出流
                        DataOutputStream out = new DataOutputStream(httpUrlCon.getOutputStream());
                        // 将要传递的数据写入数据输出流,不要使用out.writeBytes(param); 否则中文时会出错
                        out.write(stringParams.getBytes(ENCODING_UTF_8));
                        // 输出缓存
                        out.flush();
                        // 关闭数据输出流
                        out.close();
                        break;
                    default:
                        break;

                }

                httpUrlCon.connect();

                //check the result of connection
                int responseCode = httpUrlCon.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // 获得读取的内容
                    InputStreamReader in = new InputStreamReader(httpUrlCon.getInputStream());
                    // 获取输入流对象
                    BufferedReader buffer = new BufferedReader(in);
                    String inputLine;
                    while ((inputLine = buffer.readLine()) != null) {
                        result.append(inputLine).append("\n");
                    }
                    // 关闭字符输入流
                    in.close();
                    httpResponse = new HttpResponse(true, result.toString());
                    return httpResponse;
                } else {
                    httpResponse = new HttpResponse(false, new NetworkErrorException("response code error code : " + responseCode));
                }
            } catch (UnsupportedEncodingException e) {
                httpResponse = new HttpResponse(false, e);
            } catch (ProtocolException e) {
                httpResponse = new HttpResponse(false, e);
            } catch (MalformedURLException e) {
                httpResponse = new HttpResponse(false, e);
            } catch (IOException e) {
                httpResponse = new HttpResponse(false, e);
            } finally {
                if (httpUrlCon != null) {
                    // 断开连接
                    httpUrlCon.disconnect();
                }
            }
            if(currentTryTime == 1 && retryTimes >= 2){
                JiotLogger.d("sis request failed,retry again." );
            }
            currentTryTime ++;
        }
        return httpResponse;
    }

    @Override
    protected void onPostExecute(HttpResponse result) {
        super.onPostExecute(result);

        //回调
        if(result.isSuccess()){
            httpCallback.onFinish(result.getBody());
        }else{
            httpCallback.onError(result.getException());
        }
    }
}
