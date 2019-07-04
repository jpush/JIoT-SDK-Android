package cn.jiguang.iot.mqtt;

import android.support.annotation.NonNull;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author : ouyangshengduo
 * e-mail : ouysd@jiguang.cn
 * date  : 2019/6/20 10:48
 * desc : publish消息池分发器
 */
class MqttMessageDispather {

    /**最多同时请求的数量**/
    private int maxRequests;

    MqttMessageDispather(){
        this(1);
    }

    /**
     * 构造方法
     * @param maxRequests 最大请求数量
     */
    private MqttMessageDispather(int maxRequests){
        this.maxRequests = maxRequests;
    }

    /**声明一个线程池**/
    private ExecutorService executorService;

    /**等待双端队列，双端比较适合增加与删除**/
    private final Deque<MqttConnection.AsyncPublish> readyAsyncPublishs = new ArrayDeque<>();

    /**运行中的双端队列**/
    private final Deque<MqttConnection.AsyncPublish> runningAsyncPublishs = new ArrayDeque<>();


    /**
     * 线程池的初始化
     * @return 返回线程池
     */
    private synchronized ExecutorService initExecutorService(){

        if(null == executorService){
            //这里只是给这个线程起一个名字
            ThreadFactory threadFactory = new ThreadFactory() {
                @Override
                public Thread newThread(@NonNull Runnable runnable) {
                    return new Thread(runnable,"Mqtt Client Publish Thread");
                }
            };
            //这里按照OkHttp的线程池样式来创建，单个线程在闲置的时候保留60秒
            executorService = new ThreadPoolExecutor(0,Integer.MAX_VALUE,60L, TimeUnit.SECONDS,new SynchronousQueue<Runnable>(),threadFactory);
        }
        return executorService;
    }

    /**
     * 将线程加入到线程池队列
     * @param asyncPublish 传入具体的子线程
     */
    void enqueue(MqttConnection.AsyncPublish asyncPublish){
        // 首先判断正在运行的队列是否已经满了，而且同一个host请求的是否已经超过规定的数量
        if(runningAsyncPublishs.size() < maxRequests){
            runningAsyncPublishs.add(asyncPublish);
            initExecutorService().execute(asyncPublish);
        }else{
            //不满足条件，就加到等待队列
            readyAsyncPublishs.add(asyncPublish);
        }
    }

    /**
     * 结束某一个子线程
     * @param asyncPublish 子任务
     */
    void finished(MqttConnection.AsyncPublish asyncPublish){
        synchronized (this){
            runningAsyncPublishs.remove(asyncPublish);
            checkReadyPublishs();
        }
    }

    /**
     * 暂停下一个子任务的执行
     * @param asyncPublish 子任务
     */
    void pause(MqttConnection.AsyncPublish asyncPublish){
        synchronized (this){
            runningAsyncPublishs.remove(asyncPublish);
        }
    }

    /**
     * 检查是否可以运行等待中的请求
     */
    private void checkReadyPublishs() {
        //达到了同时请求最大数
        if(runningAsyncPublishs.size() >= maxRequests){
            return;
        }
        //没有等待执行的任务
        if(readyAsyncPublishs.isEmpty()){
            return;
        }
        Iterator<MqttConnection.AsyncPublish> asyncCallIterator = readyAsyncPublishs.iterator();
        while(asyncCallIterator.hasNext()){
            MqttConnection.AsyncPublish asyncPublish = asyncCallIterator.next();
            asyncCallIterator.remove();
            runningAsyncPublishs.add(asyncPublish);
            executorService.execute(asyncPublish);

            if(runningAsyncPublishs.size() >= maxRequests){
                return;
            }
        }
    }
}
