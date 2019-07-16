package cn.jiguang.iot.mqtt;

/**
 * @author : ouyangshengduo
 * e-mail : ouysd@jiguang.cn
 * date  : 2019/5/10 9:38
 * desc : mqtt操作上下文
 */
public class MqttContext {

    /**
     * 请求类型
     */
    private String requestType;

    /**
     * 请求序号
     */
    private long seqNo;

    public MqttContext(String requestType,long seqNo){
        this.requestType = requestType;
        this.seqNo = seqNo;
    }
    public long getSeqNo() {
        return seqNo;
    }

    public void setSeqNo(long seqNo) {
        this.seqNo = seqNo;
    }

    @Override
    public String toString() {
        return "MqttContext{" +
                "requestType='" + requestType + '\'' +
                ", seqNo=" + seqNo +
                '}';
    }
}
