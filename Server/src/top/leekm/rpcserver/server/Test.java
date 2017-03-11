package top.leekm.rpcserver.server;

import top.leekm.rpcserver.RpcThreadPoolExecutor;
import top.leekm.rpcserver.Run;
import top.leekm.rpcserver.utils.Helper;
import top.leekm.rpcserver.utils.OutputStreamWrapper;

import java.net.Socket;
import java.util.Base64;

/**
 * Created by lkm on 2017/3/11.
 */
public class Test {

    public static void main(String[] args) throws Throwable {
        for (int i = 0; i < 1024 * 1024; ++i) {
            if (executor.getQueue().size() >= 10240) {
                Thread.sleep(1000);
            }
            executor.execute(new Run() {
                @Override
                protected void todo() throws Throwable {
                    Socket socket = new Socket("120.25.219.34", 8088);
                    socket.setKeepAlive(false);
                    OutputStreamWrapper wrapper = new OutputStreamWrapper(socket.getOutputStream());
                    wrapper.write(Base64.getEncoder().encode(Helper.nextBytes(10240)));
                    wrapper.writeEOF();

                    byte[] buffer = new byte[4096];
                    int len = 0;
                    int lastLen = len;
                    while ((len = socket.getInputStream().read(buffer)) > 0) {
                        lastLen = len;
                    }
                    System.out.println(new String(buffer, 0, 64));

                    socket.close();
                }
            });
        }
        executor.shutdown();
    }

    static RpcThreadPoolExecutor executor = RpcThreadPoolExecutor.defaultConfigExecutor();
}
