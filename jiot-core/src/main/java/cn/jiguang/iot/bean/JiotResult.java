package cn.jiguang.iot.bean;

/**
 * @author : ouyangshengduo
 * e-mail : ouysd@jiguang.cn
 * date  : 2019/5/7 18:02
 * desc : 返回结果类
 */
public class JiotResult {
    /**
     * 序号
     */
    private long seqNo;
    /**
     * 错误码
     */
    private int errorCode;

    public JiotResult(int errorCode,long seqNo){
        this.seqNo = seqNo;
        this.errorCode = errorCode;
    }
    public long getSeqNo() {
        return seqNo;
    }

    public void setSeqNo(long seqNo) {
        this.seqNo = seqNo;
    }

    public int getErrorCode() {
        return errorCode;
    }
}
