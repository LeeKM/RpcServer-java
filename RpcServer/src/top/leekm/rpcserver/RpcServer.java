package top.leekm.rpcserver;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by lkm on 2017/3/5.
 */
public class RpcServer {

    private final AtomicInteger COUNTER = new AtomicInteger(1);
    private boolean start = false;
    private ServerSocketChannel serverChannel;
    private Thread socketThread;
    private SelectorWorker worker;
    private ExecutorService bizExecutor;

    public synchronized void start(int port) throws IOException {
        if (start) {
            throw new IllegalStateException("server has been started");
        }
        serverChannel = ServerSocketChannel.open();
        serverChannel.socket().setSoTimeout(5000);
        serverChannel.socket().bind(new InetSocketAddress(port));

//        int processor = Runtime.getRuntime().availableProcessors();
//        int processor = 1;
//        selectorPool = SelectorPool.open(processor);

        bizExecutor = RpcThreadPoolExecutor.defaultConfigExecutor();
        worker = new SelectorWorker(bizExecutor).start();

        socketThread = new Thread(new Run() {
            @Override
            protected void todo() throws Throwable {
                loop();
            }
        }, "server-thread: " + COUNTER.getAndIncrement());
        socketThread.start();
        start = true;
    }

    private void loop() {
        while (start) {
            try {
                SocketChannel channel = serverChannel.socket().accept().getChannel();
                worker.addConnection(channel);
            } catch (SocketTimeoutException timeoutException) {
                // ignore
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        worker.stop();
        try {
            serverChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            worker = null;
            serverChannel = null;
            socketThread = null;
        }
    }

    public synchronized void stop() {
        if (!start) {
            throw new IllegalStateException("server is not started");
        }
        worker.setBizExecutor(null);
        bizExecutor.shutdown();
        bizExecutor = null;
        start = false;
    }
}
