package cn.jiguang.iot.bean;

/**
 * @author : ouyangshengduo
 * e-mail : ouysd@jiguang.cn
 * date  : 2019/5/7 10:29
 * desc : 属性设置请求类
 */
public class PropertySetReq {

    /**设置属性请求的序号**/
    private long seqNo;
    /**属性组内容**/
    private Property[] properties;
    /**下发设置属性的时间戳**/
    private long time;
    /**下发设置属性的属性版本**/
    private long version;

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public long getSeqNo() {
        return seqNo;
    }

    public void setSeqNo(long seqNo) {
        this.seqNo = seqNo;
    }

    public Property[] getProperties() {
        return properties;
    }

    public void setProperties(Property[] properties) {
        this.properties = properties;
    }
}
