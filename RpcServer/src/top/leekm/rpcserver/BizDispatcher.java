package top.leekm.rpcserver;

import java.nio.channels.SelectionKey;

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
        System.out.println(new String(rawData));
        context.selectionKey.interestOps(SelectionKey.OP_WRITE);
    }

    @Override
    public void onReject() {
        context.selectionKey.cancel();
    }
}
