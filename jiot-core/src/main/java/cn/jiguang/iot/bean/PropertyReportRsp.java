package cn.jiguang.iot.bean;

/**
 * @author : ouyangshengduo
 * e-mail : ouysd@jiguang.cn
 * date  : 2019/5/7 10:29
 * desc :  属性上报后回复接收类
 */
public class PropertyReportRsp {

    /**上报后的错误码**/
    private int code;
    /**上报回复的属性版本**/
    private long verion;
    /**回复的序号**/
    private long seqNo;
    /**回复的属性**/
    private Property[] properties;

    public Property[] getProperties() {
        return properties;
    }

    public void setProperties(Property[] properties) {
        this.properties = properties;
    }

    public long getSeqNo() {
        return seqNo;
    }

    public void setSeqNo(long seqNo) {
        this.seqNo = seqNo;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public long getVerion() {
        return verion;
    }

    public void setVerion(long verion) {
        this.verion = verion;
    }
}
