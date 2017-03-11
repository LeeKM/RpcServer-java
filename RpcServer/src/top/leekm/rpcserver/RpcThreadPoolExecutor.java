package top.leekm.rpcserver;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by lkm on 2017/3/11.
 */
public class RpcThreadPoolExecutor extends ThreadPoolExecutor {

    public static RpcThreadPoolExecutor defaultConfigExecutor() {
        return new RpcThreadPoolExecutor(64, 64, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(), new DefaultThreadFactory(), new RpcRejectedExecutionHandler());
    }

    public RpcThreadPoolExecutor(int corePoolSize, int maximumPoolSize,
                                 long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    public RpcThreadPoolExecutor(int corePoolSize, int maximumPoolSize,
                                 long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue,
                                 ThreadFactory threadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
    }

    public RpcThreadPoolExecutor(int corePoolSize, int maximumPoolSize,
                                 long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue,
                                 RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
    }

    public RpcThreadPoolExecutor(int corePoolSize, int maximumPoolSize,
                                 long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue,
                                 ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        System.out.println(r + " -before");
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        System.out.println(r + " -after");
    }

    private static class DefaultThreadFactory implements ThreadFactory {
        private static AtomicInteger factoryCount = new AtomicInteger(0);
        private AtomicInteger threadCount = new AtomicInteger(0);
        private int factoryIndex = factoryCount.incrementAndGet();

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, "RpcThread-" +
                    factoryIndex + "-" +
                    threadCount.incrementAndGet());
            return thread;
        }
    }

    private static class RpcRejectedExecutionHandler implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            if (r instanceof  Rejactable) {
                ((Rejactable) r).onReject();
            }
        }
    }

    public interface Rejactable {
        void onReject();
    }
}
