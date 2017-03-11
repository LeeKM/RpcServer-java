package top.leekm.rpcserver;

import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;

import java.io.IOException;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by lkm on 2017/3/5.
 */
public class SelectorWorker {

    private final static AtomicInteger COUNTER = new AtomicInteger(1);

    volatile WorkerStatus status = WorkerStatus.stop;
    private Selector selector;
    private Thread selectThread;
    private ExecutorService bizExecutor;
    private ConcurrentHashMap<SelectionKey, SelectContext> connections =
            new ConcurrentHashMap<>();
    private CopyOnWriteArrayList<SocketChannel> penddingConnections =
            new CopyOnWriteArrayList<>();

    public SelectorWorker() {
    }

    public SelectorWorker(ExecutorService bizExecutor) {
        this.bizExecutor = bizExecutor;
    }

    public synchronized SelectorWorker start() throws IOException {
        if (status != WorkerStatus.stop) {
            throw new RuntimeException("worker is not ready to start");
        }
        selector = Selector.open();
        selectThread = new Thread(new Run() {
            @Override
            protected void todo() throws Throwable {
                loop();
            }
        }, "select-thread: " + COUNTER.getAndIncrement());
        selectThread.start();
        status = WorkerStatus.start;
        return this;
    }

    public synchronized void setBizExecutor(ExecutorService executor) {
        this.bizExecutor = executor;
    }

    public synchronized void addConnection(SocketChannel channel) {
        if (status != WorkerStatus.start) {
            throw new RuntimeException("worker is not in start status");
        }
        penddingConnections.add(channel);
        selector.wakeup();
    }

    private void loop() throws Throwable {
        while (status == WorkerStatus.start) {

            int valid = selector.select();
            handlePenddingConnections();
            if (valid <= 0) continue;

            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            for (SelectionKey key : selectionKeys) {
                SelectContext context = connections.get(key);
                if (key.isReadable()) {
                    context.onReadable();
                } else if (key.isWritable()) {
                    context.onWritable();
                }

                if (!key.isValid()) {
                    connections.remove(key);
                    key.channel().close();
                }
            }
            selectionKeys.clear();
        }
        for (SelectContext context : connections.values()) {
            context.clear();
        }
        connections.clear();
        for (SocketChannel channel : penddingConnections) {
            if (channel.isOpen()) {
                channel.close();
            }
        }
        selector.close();
        selector = null;
        selectThread = null;
        status = WorkerStatus.stop;
    }

    private void handlePenddingConnections() throws IOException {
        for (SocketChannel channel : penddingConnections) {
            channel.configureBlocking(false);
            onConfigConnection(channel);
            SelectionKey selectionKey = channel.register(selector, SelectionKey.OP_READ);
            SelectContext context = new SelectContext(channel, selectionKey);
            connections.put(selectionKey, context);
            penddingConnections.remove(channel);
        }
    }

    protected void onConfigConnection(SocketChannel socketChannel) throws SocketException {
        socketChannel.socket().setKeepAlive(false);
    }

    public synchronized void stop() {
        if (status == WorkerStatus.stop) {
            throw new RuntimeException("worker is stopped");
        }
        status = WorkerStatus.pending;
        selector.wakeup();
    }

    private enum WorkerStatus {
        stop, start, pending
    }


    private enum IOStatus {
        CH, WRAP, EOF
    }

    class SelectContext {
        private final static byte WRAP_VALUE = Protocol.WRAP_VALUE;
        private final static byte EOF_VALUE = Protocol.EOF_VALUE;
        private final static int MAX_LEN = Protocol.MAX_LEN;

        private SocketChannel socketChannel;
        SelectionKey selectionKey;

        ByteOutputStream rawData = new ByteOutputStream();
        private ByteBuffer buffer = ByteBuffer.allocate(1024);
        private int responseIndex = 0;
        private IOStatus status = IOStatus.CH;

        public SelectContext(SocketChannel channel, SelectionKey key) {
            this.socketChannel = channel;
            this.selectionKey = key;
        }

        private void onReadable() throws IOException {
            buffer.clear();
            if (socketChannel.read(buffer) > 0) {
                buffer.flip();
                while (buffer.hasRemaining()
                        && status != IOStatus.EOF
                        && rawData.size() < MAX_LEN+1) {
                    byte value = buffer.get();
                    switch (status) {
                        case CH:
                            if (EOF_VALUE == value) {
                                status = IOStatus.EOF;
                            } else if (WRAP_VALUE == value) {
                                status = IOStatus.WRAP;
                            } else {
                                break;
                            }
                            continue;
                        case WRAP:
                            status = IOStatus.CH;
                    }
                    rawData.write(value);
                }
            }

            if (rawData.size() > MAX_LEN) {
                onBufferOverLoad();
            } else if (status == IOStatus.EOF) {
                selectionKey.interestOps(SelectionKey.OP_CONNECT);
                final ExecutorService local = bizExecutor;
                if (null == local || local.isShutdown()) {
                    new BizDispatcher(this).dispatchBiz();
                } else {
                    local.execute(new BizDispatcher(this));
                }
            }
        }

        private void onBufferOverLoad() {
            this.rawData = null;
            this.buffer = null;
            this.selectionKey.cancel();
        }

        public void onWritable() throws IOException {
            byte[] raw = rawData.getBytes();
            int endIndex = responseIndex + 1024 < rawData.size() ? responseIndex + 1024 : rawData.size();
            socketChannel.write(ByteBuffer.wrap(raw, responseIndex, endIndex));
            responseIndex = endIndex;
            selectionKey.cancel();
        }

        public void clear() {
            selectionKey.cancel();
            try {
                socketChannel.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }
}
