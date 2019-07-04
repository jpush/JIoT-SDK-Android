package cn.jiguang.iot.bean;

/**
 * @author : ouyangshengduo
 * e-mail : ouysd@jiguang.cn
 * date  : 2019/5/7 10:29
 * desc : 版本上报后的回复类
 */
public class VersionReportRsp {

    /**上报版本回复后的序号**/
    private long seqNo;
    /**回复的错误码**/
    private int code;

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
}
