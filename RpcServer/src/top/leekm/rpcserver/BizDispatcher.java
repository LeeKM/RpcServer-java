package top.leekm.rpcserver;

import top.leekm.rpcserver.utils.Helper;

import java.nio.channels.SelectionKey;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

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
        String request = new String(context.rawData.getBytes());
        String response;
        if (request.contains("RSA")) {
            response = handleRSA();
        } else if (request.contains("MD5")) {
            response = handleDigest(request, "MD5");
        } else if (request.contains("SHA1")) {
            response = handleDigest(request, "SHA-1");
        } else if (request.contains("SHA256")) {
            response = handleDigest(request, "SHA-256");
        } else if (request.contains("SHA512")) {
            response = handleDigest(request, "SHA-512");
        } else {
            response = request;
        }
        context.rawData.reset();
        context.rawData.write(response.getBytes());
        context.selectionKey.interestOps(SelectionKey.OP_WRITE);
    }

    private String handleDigest(String request, String algorithm) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            int start = request.indexOf(algorithm) + algorithm.length();
            int end = request.lastIndexOf(algorithm);
            end = end <= start ? request.length() : end;
            return algorithm + ": " + Helper.byteToHexFormate(digest.digest(request.substring(start, end).getBytes()));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return e.getClass().getName();
        }
    }

    private String handleRSA() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.genKeyPair();
            String pubKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
            String priKey = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
            return String.format("{\"rsaPrivate\":\"%s\",\"rsaPublic\":\"%s\"}", priKey, pubKey);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return e.getClass().getName();
        }
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
