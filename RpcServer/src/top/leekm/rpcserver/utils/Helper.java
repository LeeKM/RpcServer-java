package top.leekm.rpcserver.utils;

import top.leekm.rpcserver.Protocol;

import java.io.ByteArrayOutputStream;
import java.util.Random;

import static top.leekm.rpcserver.utils.Helper.Status.CH;
import static top.leekm.rpcserver.utils.Helper.Status.EOF;
import static top.leekm.rpcserver.utils.Helper.Status.WRAP;

/**
 * Created by lkm on 2017/3/10.
 */
public class Helper {

    private final static byte WRAP_VALUE = Protocol.WRAP_VALUE;
    private final static byte EOF_VALUE = Protocol.EOF_VALUE;

    private static Random random = new Random();

    public static byte[] nextBytes(int size) {
        byte[] buffer = new byte[size];
        for (int i = 0; i < size; ++i) {
            buffer[i] = (byte) random.nextInt();
        }
        return buffer;
    }

    public static byte[] unWrapBytes(byte[] buffer) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Status status = CH;
        for (byte value : buffer) {
            switch (status) {
                case CH:
                    if (EOF_VALUE == value) {
                        status = EOF;
                    } else if (WRAP_VALUE == value) {
                        status = WRAP;
                    } else {
                        break;
                    }
                    continue;
                case WRAP:
                    status = CH;
            }
            byteArrayOutputStream.write(value);
        }
        return byteArrayOutputStream.toByteArray();
    }

    public static int byteToIntL(byte[] buffer) {
        int value = ((buffer[0] & 0xFF) << 24) +
                ((buffer[1] & 0xFF) << 16) +
                ((buffer[2] & 0xFF) << 8) +
                (buffer[3] & 0xFF);
        return value;
    }

    public static byte[] intToByteL(int value) {
        byte[] buffer = new byte[4];
        buffer[0] = (byte) (value >> 24);
        buffer[1] = (byte) (value >> 16);
        buffer[2] = (byte) (value >> 8);
        buffer[3] = (byte) (value);
        return buffer;
    }

    enum Status {
        CH, WRAP, EOF
    }
}
