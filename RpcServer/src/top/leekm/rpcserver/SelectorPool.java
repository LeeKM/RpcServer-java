package top.leekm.rpcserver;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by lkm on 2017/3/5.
 */
public class SelectorPool {

    private AtomicInteger index = new AtomicInteger(0);
    private SelectorWorker[] workers;

    private SelectorPool(int size) throws IOException {
        this.workers = new SelectorWorker[size];
        for (int i = 0; i < size; ++i) {
            workers[i] = new SelectorWorker();
            workers[i].start();
        }
    }

    public void addConnection(SocketChannel channel) {
        int localIndex = index.getAndIncrement() & 0xFFFFFF;
        localIndex = localIndex % workers.length;
        workers[localIndex].addConnection(channel);
    }

    public static SelectorPool open(int size) throws IOException {
        return new SelectorPool(size);
    }

    public void close() {
        for (SelectorWorker worker : workers) {
            worker.stop();
        }
    }
}
