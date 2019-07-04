package cn.jiguang.example.ui;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.jiguang.R;
import cn.jiguang.example.ui.base.BaseActivity;
import cn.jiguang.iot.JiotClient;
import cn.jiguang.iot.api.JiotClientApi;
import cn.jiguang.iot.bean.DeviceInfo;
import cn.jiguang.iot.bean.Event;
import cn.jiguang.iot.bean.EventReportReq;
import cn.jiguang.iot.bean.EventReportRsp;
import cn.jiguang.iot.bean.JiotResult;
import cn.jiguang.iot.bean.MsgDeliverReq;
import cn.jiguang.iot.bean.Property;
import cn.jiguang.iot.bean.PropertyReportReq;
import cn.jiguang.iot.bean.PropertyReportRsp;
import cn.jiguang.iot.bean.PropertySetReq;
import cn.jiguang.iot.bean.VersionReportReq;
import cn.jiguang.iot.bean.VersionReportRsp;
import cn.jiguang.iot.callback.JclientHandleCallback;
import cn.jiguang.iot.callback.JclientMessageCallback;
import cn.jiguang.iot.util.JiotLogger;

/**
 * @author: ouyangshengduo
 * e-mail: ouysd@jiguang.cn
 * date  : 2019/4/9 10:29
 * desc  :
 */
public class MainActivity extends BaseActivity implements JclientHandleCallback, JclientMessageCallback, View.OnClickListener {

    private JiotClientApi jiotClientApi;
    /**
     * 运行日志
     */
    private TextView tvLogInfo;
    /**
     * 设备三元组输入之产品key
     */
    private EditText etProductKey;
    /**
     * 设备三元组输入之设备名称
     */
    private EditText etDeviceName;
    /**
     * 设备三元组输入之设备密钥
     */
    private EditText etDeviceSecret;
    /**
     * 设备属性或者事件的名称
     */
    private EditText etPropertyEventName;
    /**
     * 设备属性或者事件的值
     */
    private EditText etValueContent;
    /**
     * 设备上报版本
     */
    private EditText etVersion;
    /**
     * 设备信息布局
     */
    private LinearLayout llDeviceInfo;

    private SharedPreferences preferences = null;

    private  static final String DEVICE_INFO = "DEVICE_INFO";

    private String localProductKey;
    private String localDeviceName;
    private String localDeviceSecret;

    private CallbackHandler mHandler = new CallbackHandler(this);
    private static class CallbackHandler extends Handler {
        private WeakReference<Context> reference;
        private MainActivity mainActivity;

        /** 静态内部类构造方法
         * @param context 上下文
         */
        CallbackHandler(Context context){
            reference = new WeakReference<>(context);
        }

        @Override
        public void handleMessage(Message msg) {
            mainActivity = (MainActivity) reference.get();
            if(mainActivity != null){
                switch (msg.what){
                    case Constant.CONNECTED_HANDLE:
                        showLogInfo("Client connected. \n");
                        break;
                    case Constant.CONNECT_FAILE_HANDLE:
                        mainActivity.showDisconnectStatusUI();
                        showLogInfo("Client connect fail,error code is " + msg.arg1 + ". \n");
                        break;
                    case Constant.GET_CONN_STATUS:
                        showLogInfo("Client connect status is " + Constant.CONN_STATUS[msg.arg1] + ". \n");
                        break;
                    case Constant.DISCONNECT_HANDLE:
                        mainActivity.showDisconnectStatusUI();
                        showLogInfo("Client disconnect " + " errcode = " + msg.arg1 + ". \n");
                        break;
                    case Constant.SUBSCRIBE_FAIL_HANDLE:
                        showLogInfo("Client subscribe fail and topic = " + msg.obj + ". \n");
                        break;
                    case Constant.MESSAGE_TIME_OUT_HANDLE:
                        showLogInfo("Client send message timeout and seq_no = " + msg.obj + ". \n");
                        break;
                    case Constant.PUBLISH_FAIL_HANDLE:
                        showLogInfo("Client publish fail and seq_no = " + msg.obj + ". \n");
                        break;
                    case Constant.REPORT_VERSION_RESPONSE:
                        VersionReportRsp versionReportRsp = (VersionReportRsp) msg.obj;
                        showLogInfo("Client receive message (about report verion) from server" + " errcode = " + msg.arg1 + " code = " + versionReportRsp.getCode() + " seq_no = " + versionReportRsp.getSeqNo() + ". \n");
                        break;
                    case Constant.REPORT_PROPERTY_RESPONSE:
                        PropertyReportRsp propertyReportRsp = (PropertyReportRsp) msg.obj;
                        showLogInfo("Client receive message (about report property) from server" + " errcode = " + msg.arg1 + " code = " + propertyReportRsp.getCode() + " seq_no = " + propertyReportRsp.getSeqNo() + " version = " + propertyReportRsp.getVerion() + " property_size = " + propertyReportRsp.getProperties().length + ". \n");
                        break;
                    case Constant.REPORT_EVENT_RESPONSE:
                        EventReportRsp eventReportRsp = (EventReportRsp) msg.obj;
                        showLogInfo("Client receive message (about report event) from server" + " errcode = " + msg.arg1 + " code = " + eventReportRsp.getCode() + " seq_no = " + eventReportRsp.getSeqNo() + ". \n");
                        break;
                    case Constant.MSG_DELIVER_REQ:
                        MsgDeliverReq msgDeliverReq = (MsgDeliverReq) msg.obj;
                        showLogInfo("Client receive message (about msg deliver) from server" + " errcode = " + msg.arg1 + " seq_no = " + msgDeliverReq.getSeqNo() + " message = " + msgDeliverReq.getMessage() + " timestamp = " + msgDeliverReq.getTime() + ". \n");
                        break;
                    case Constant.PROPERTY_SET_REQ:
                        PropertySetReq propertySetReq = (PropertySetReq) msg.obj;
                        showLogInfo("Client receive message (about set property) from server" + " errcode = " + msg.arg1 + " seq_no = " + propertySetReq.getSeqNo() + " verion = " + propertySetReq.getVersion() + " property_size = " + propertySetReq.getProperties().length + ". \n");
                        break;
                    case Constant.CONNECTING:
                        showLogInfo("Client connecting... " + " \n");
                        break;
                    case Constant.DISCONNECTING:
                        showLogInfo("Client disconnecting... " + " \n");
                        break;
                    case Constant.CONNECT_RESPONSE:
                        mainActivity.showDisconnectStatusUI();
                        showLogInfo("Client connect response code = " + msg.arg1 + " \n");
                        break;
                    default:
                        break;
                }
            }
        }

        /**
         * 显示日志
         * @param content 日志内容
         */
        private void showLogInfo(String content){
            if(null != mainActivity) {
                mainActivity.tvLogInfo.append(content);
            }
        }
    }

    /**
     * 设备连接失败的UI变化
     */
    private void showDisconnectStatusUI() {
        llDeviceInfo.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        initData();
    }

    private boolean destroyed = false;




    /**
     * 数据对象的初始化
     */
    private void initData() {
        preferences = getSharedPreferences(DEVICE_INFO, Activity.MODE_PRIVATE);
        localProductKey = preferences.getString("productKey",null);
        localDeviceName = preferences.getString("deviceName",null);
        localDeviceSecret = preferences.getString("deviceSecret",null);
        jiotClientApi = JiotClient.getInstance();
        jiotClientApi.jiotInit(this,true);
        etProductKey.setText(localProductKey == null ? Constant.DEFAULT_PRODUCT_KEY : localProductKey);
        etDeviceName.setText(localDeviceName == null ? Constant.DEFAULT_DEVICE_NAME : localDeviceName);
        etDeviceSecret.setText(localDeviceSecret == null ? Constant.DEFAULT_DEVICE_SECRET : localDeviceSecret);
        etVersion.setText(Constant.DEFAULT_REPORT_VERSION);
        etPropertyEventName.setText(Constant.DEFAULT_DEVICE_PROPERTY);
        etValueContent.setText(Constant.DEFAULT_REPORT_CONTENT);
    }

    /**
     * 界面UI控件的初始化
     */
    private void initView() {
        //连接服务器
        Button btnConnect = (Button) findViewById(R.id.btn_connect);
        //断开连接
        Button btnDisconnect = (Button) findViewById(R.id.btn_disconnect);
        //获取连接状态
        Button btnGetConnStatus = (Button) findViewById(R.id.btn_get_conn_status);
        //上报设备属性
        Button btnReportProperty = (Button) findViewById(R.id.btn_report_property);
        //上报设备事件
        Button btnReportEvent = (Button) findViewById(R.id.btn_report_event);
        //上报设备版本
        Button btnReportVersion = (Button) findViewById(R.id.btn_report_version);

        tvLogInfo = (TextView) findViewById(R.id.tv_log_info);
        //logcat运行日志等级
        Spinner spinner = (Spinner) findViewById(R.id.spinner);
        etProductKey = (EditText) findViewById(R.id.et_product_key);
        etDeviceName = (EditText) findViewById(R.id.et_device_name);
        etDeviceSecret = (EditText) findViewById(R.id.et_device_secret);
        llDeviceInfo = (LinearLayout) findViewById(R.id.ll_device_info);
        etPropertyEventName = (EditText) findViewById(R.id.et_property_event_name);
        etValueContent = (EditText) findViewById(R.id.et_value_content);
        etVersion = (EditText) findViewById(R.id.et_version);

        llDeviceInfo.setVisibility(View.VISIBLE);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                              @Override
                                              //当AdapterView中的item被选中的时候执行的方法。
                                              public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                                                  if(null != jiotClientApi){
                                                      jiotClientApi.jiotSetLogLevel(position);
                                                  }
                                              }

                                              @Override
                                              //未选中时的时候执行的方法
                                              public void onNothingSelected(AdapterView<?> parent) {

                                              }
                                          });
        btnConnect.setOnClickListener(this);
        btnDisconnect.setOnClickListener(this);
        btnGetConnStatus.setOnClickListener(this);
        btnReportProperty.setOnClickListener(this);
        btnReportEvent.setOnClickListener(this);
        btnReportVersion.setOnClickListener(this);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        jiotClientApi.jiotRelease();
        destroyed = true;
    }


    @Override
    public void onClick(View v) {

        switch (v.getId()){
            //连接服务器
            case R.id.btn_connect:
                doConnect();
                break;
            //断开与服务器的连接
            case R.id.btn_disconnect:
                doDisconnect();
                break;
            //获取客户端的连接状态
            case R.id.btn_get_conn_status:
                doGetConnStatus();
                break;
            //上报客户端的设备版本
            case R.id.btn_report_version:
                doReportVersion();
                break;
            //上报客户端的设备属性
            case R.id.btn_report_property:
                doReportProperty();
                break;
            //上报客户端的设备事件
            case R.id.btn_report_event:
                doReportEvent();
                break;
            default:
                break;
        }
    }

    /**
     * 响应按钮点击上报设备事件
     */
    private void doReportEvent() {
        if(checkReportPropertyOrEventInput()) {
            EventReportReq eventReportReq = new EventReportReq();
            eventReportReq.setSeqNo(0);
            Event event = new Event();
            event.setName(etPropertyEventName.getText().toString().trim());
            event.setContent(etValueContent.getText().toString().trim());
            event.setTime(System.currentTimeMillis());
            eventReportReq.setEvent(event);
            JiotResult res = jiotClientApi.jiotEventReportReq(eventReportReq);
            if(res.getErrorCode() != 0){
                tvLogInfo.append("Client report local error" + " errcode = " +res.getErrorCode() + ". \n");
            }
        }else{
            Toast.makeText(this,"Input Invalid",Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 响应按钮点击上报设备属性
     */
    private void doReportProperty() {
        if(checkReportPropertyOrEventInput()) {
            PropertyReportReq propertyReportReq = new PropertyReportReq();
            propertyReportReq.setSeqNo(0);
            propertyReportReq.setVersion(2);
            //属性的数量，多属性测试时需要根据具体的值进行修改
            Property[] properties = new Property[1];
            Property property = new Property();
            property.setName(etPropertyEventName.getText().toString().trim());
            property.setTime(System.currentTimeMillis());
            property.setValue(etValueContent.getText().toString().trim());
            properties[0] = property;

            //测试多属性上报代码
            /**Property property1 = new Property();
            property1.setName(etPropertyEventName.getText().toString().trim() + "3");
            property1.setTime(System.currentTimeMillis());
            property1.setValue(etValueContent.getText().toString().trim());
            properties[1] = property1;**/
            propertyReportReq.setProperties(properties);
            JiotResult res = jiotClientApi.jiotPropertyReportReq(propertyReportReq);
            if(res.getErrorCode() != 0){
                tvLogInfo.append("Client report local error" + " errcode = " +res.getErrorCode() + ". \n");
            }
        }else{
            Toast.makeText(this,"Input Invalid",Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 校验上报设备属性或者事件输入数据的合法性
     * @return true 合法，false 非法
     */
    private boolean checkReportPropertyOrEventInput() {
        return !etPropertyEventName.getText().toString().trim().isEmpty() &&
                !etValueContent.getText().toString().trim().isEmpty();
    }

    /**
     * 响应按钮点击上报设备版本
     */
    private void doReportVersion() {

        if(checkReportVersionInput()) {
            VersionReportReq versionReportReq = new VersionReportReq();
            versionReportReq.setSeqNo(0);
            versionReportReq.setVersion(etVersion.getText().toString().trim());
            JiotResult res = jiotClientApi.jiotVersionReportReq(versionReportReq);
            if(res.getErrorCode() != 0){
                tvLogInfo.append("Client report local error" + " errcode = " +res.getErrorCode() + ". \n");
            }
        }else{
            Toast.makeText(this,"Input Invalid",Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 校验上报版本信息的输入数据合法性
     * @return true 合法，false 非法
     */
    private boolean checkReportVersionInput() {
        return !etVersion.getText().toString().trim().isEmpty();

    }

    /**
     * 响应按钮点击获取连接状态
     */
    private void doGetConnStatus() {
        int status = jiotClientApi.jiotGetConnStatus();

        Message message = mHandler.obtainMessage();
        message.what = Constant.GET_CONN_STATUS;
        message.arg1 = status;
        mHandler.sendMessage(message);
    }

    /**
     * 响应按钮点击断开连接
     */
    private void doDisconnect() {

        mHandler.sendEmptyMessage(Constant.DISCONNECTING);
        synchronized (JiotClientApi.class) {
            jiotClientApi.jiotDisConn();
        }
    }

    /**
     * 响应按钮点击连接
     */
    private void doConnect() {

        //检查输入合法性
        if(checkConnectInput()) {
            //屏蔽输入
            llDeviceInfo.setVisibility(View.GONE);
            mHandler.sendEmptyMessage(Constant.CONNECTING);

            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("productKey", etProductKey.getText().toString().trim());
            editor.putString("deviceName", etDeviceName.getText().toString().trim());
            editor.putString("deviceSecret", etDeviceSecret.getText().toString().trim());
            editor.commit();

            synchronized (JiotClientApi.class) {
                DeviceInfo deviceInfo = new DeviceInfo();
                deviceInfo.setProductKey(etProductKey.getText().toString().trim());
                deviceInfo.setDeviceName(etDeviceName.getText().toString().trim());
                deviceInfo.setDeviceSecret(etDeviceSecret.getText().toString().trim());
                int ret = jiotClientApi.jiotConn(deviceInfo, MainActivity.this, MainActivity.this);
                if (ret != 0) {
                    Message message = mHandler.obtainMessage();
                    message.arg1 = ret;
                    message.what = Constant.CONNECT_RESPONSE;
                    mHandler.sendMessage(message);
                }
            }
        }else{
            Toast.makeText(this,"Input Invalid",Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 检查输入连接设备信息的合法信息
     * @return true合法，false 非法
     */
    private boolean checkConnectInput() {
        return !etProductKey.getText().toString().trim().isEmpty() &&
                !etDeviceName.getText().toString().trim().isEmpty() &&
                !etDeviceSecret.getText().toString().trim().isEmpty();
    }


    /******************************连接状态回调************************************/
    @Override
    public void jiotConnectedHandle() {
        if(null != mHandler){
            mHandler.sendEmptyMessage(Constant.CONNECTED_HANDLE);
        }
    }

    @Override
    public void jiotConnectFailHandle(int errorCode) {
        if(null != mHandler){
            Message message = mHandler.obtainMessage();
            message.what = Constant.CONNECT_FAILE_HANDLE;
            message.arg1 = errorCode;
            mHandler.sendMessage(message);
        }
    }

    @Override
    public void jiotDisconnectHandle(int errorCode,String msg) {

        if(null != mHandler){
            Message message = mHandler.obtainMessage();
            message.what = Constant.DISCONNECT_HANDLE;
            message.arg1 = errorCode;
            message.obj = msg;
            mHandler.sendMessage(message);
        }
    }

    @Override
    public void jiotSubscribeFailHandle(String topicFilter) {
        if(null != mHandler){
            Message message = mHandler.obtainMessage();
            message.what = Constant.SUBSCRIBE_FAIL_HANDLE;
            message.obj = topicFilter;
            mHandler.sendMessage(message);
        }
    }

    @Override
    public void jiotPublishFailHandle(long seqNo) {
        if(null != mHandler){
            Message message = mHandler.obtainMessage();
            message.what = Constant.PUBLISH_FAIL_HANDLE;
            message.obj = seqNo;
            mHandler.sendMessage(message);
        }
    }

    @Override
    public void jiotMessageTimeoutHandle(long seqNo) {

        JiotLogger.e("jiotMessageTimeoutHandle callback jiotClientApi.jiotGetConnStatus() = " + jiotClientApi.jiotGetConnStatus());
        if(null != mHandler){
            Message message = mHandler.obtainMessage();
            message.what = Constant.MESSAGE_TIME_OUT_HANDLE;
            message.obj = seqNo;
            mHandler.sendMessage(message);
        }
    }


    /******************************消息接收回调************************************/
    @Override
    public void jiotPropertyReportRsp(PropertyReportRsp propertyReportRsp, int errorCode) {
        if(null != mHandler){
            Message message = mHandler.obtainMessage();
            message.obj = propertyReportRsp;
            message.arg1 = errorCode;
            message.what = Constant.REPORT_PROPERTY_RESPONSE;
            mHandler.sendMessage(message);
        }
    }

    @Override
    public void jiotEventReportRsp(EventReportRsp eventReportRsp, int errorCode) {
        if(null != mHandler){
            Message message = mHandler.obtainMessage();
            message.obj = eventReportRsp;
            message.arg1 = errorCode;
            message.what = Constant.REPORT_EVENT_RESPONSE;
            mHandler.sendMessage(message);
        }
    }

    @Override
    public void jiotVersionReportRsp(VersionReportRsp versionReportRsp, int errorCode) {
        if(null != mHandler){
            Message message = mHandler.obtainMessage();
            message.obj = versionReportRsp;
            message.arg1 = errorCode;
            message.what = Constant.REPORT_VERSION_RESPONSE;
            mHandler.sendMessage(message);
        }
    }

    @Override
    public void jiotPropertySetReq(PropertySetReq propertySetReq, int errorCode) {
        if(null != mHandler){
            Message message = mHandler.obtainMessage();
            message.obj = propertySetReq;
            message.arg1 = errorCode;
            message.what = Constant.PROPERTY_SET_REQ;
            mHandler.sendMessage(message);
        }
    }


    @Override
    public void jiotMsgDeliverReq(MsgDeliverReq msgDeliverReq, int errorCode) {
        if(null != mHandler){
            Message message = mHandler.obtainMessage();
            message.obj = msgDeliverReq;
            message.arg1 = errorCode;
            message.what = Constant.MSG_DELIVER_REQ;
            mHandler.sendMessage(message);
        }
    }


}
