package top.leekm.rpcserver.utils;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by lkm on 2017/3/10.
 */
public class OutputStreamWrapper extends OutputStream {

    private final static byte WRAP_VALUE = (byte) 32;
    private final static byte EOF_VALUE = (byte) '\r';

    private OutputStream outputStream;

    public OutputStreamWrapper(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    @Override
    public void write(int b) throws IOException {
        if (b == WRAP_VALUE || b == EOF_VALUE) {
            outputStream.write(WRAP_VALUE);
        }
        outputStream.write(b);
    }

    public void writeEOF() throws IOException {
        outputStream.write(EOF_VALUE);
    }

    @Override
    public void flush() throws IOException {
        outputStream.flush();
    }

    @Override
    public void close() throws IOException {
        outputStream.close();
    }
}
