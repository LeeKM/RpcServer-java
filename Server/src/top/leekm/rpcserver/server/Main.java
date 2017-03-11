package top.leekm.rpcserver.server;

import top.leekm.rpcserver.RpcServer;
import top.leekm.rpcserver.RpcThreadPoolExecutor;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

/**
 * Created by lkm on 2017/3/5.
 */
public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {

        System.out.println((int) ' ');

        RpcServer server = new RpcServer();

        server.start(8088);

        Thread.sleep(10000);

        server.stop();

        System.out.println("finish");
    }

}

class Runner implements Runnable, RpcThreadPoolExecutor.Rejactable {

    @Override
    public void run() {

    }

    @Override
    public void onReject() {
        System.out.println(Thread.currentThread() + " -reject");
    }
}