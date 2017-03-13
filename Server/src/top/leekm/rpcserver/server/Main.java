package top.leekm.rpcserver.server;

import top.leekm.rpcserver.RpcServer;

import java.io.IOException;

/**
 * Created by lkm on 2017/3/5.
 */
public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {

        System.out.println((int) ' ');

        RpcServer server = new RpcServer();

        server.start(8088);

        Thread.sleep(24 * 3600 * 1000);

        server.stop();

        System.out.println("finish");

    }

}