package top.leekm.rpcserver;

import top.leekm.rpcserver.utils.Helper;

import java.nio.channels.SelectionKey;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by lkm on 2017/3/11.
 */
public class BizDispatcher extends Run implements RpcThreadPoolExecutor.Rejactable {

    private SelectorWorker.SelectContext context;

    public BizDispatcher(SelectorWorker.SelectContext context) {
        this.context = context;
    }

    @Override
    protected void todo() throws Throwable {
        dispatchBiz();
    }

    public final void dispatchBiz() {
        byte[] rawData = context.rawData.getBytes();
        System.out.println(sha256Digest(rawData));
        context.selectionKey.interestOps(SelectionKey.OP_WRITE);
    }

    private String sha256Digest(byte[] rawData) {
        String sha256 = "sha-256";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            sha256 = Helper.byteToHexFormate(digest.digest(rawData));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return sha256;
    }

    @Override
    public void onReject() {
        context.selectionKey.cancel();
    }
}
