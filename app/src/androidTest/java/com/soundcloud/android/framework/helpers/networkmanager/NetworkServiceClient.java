package com.soundcloud.android.framework.helpers.networkmanager;

import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import java.util.Random;

public class NetworkServiceClient {

    private final Waiter waiter;
    private final Messenger service;
    private final ResponseHandler responseHandler;
    private final Random random;

    public NetworkServiceClient(Messenger service, ResponseHandler responseHandler) {
        waiter = new Waiter();
        this.service = service;
        this.responseHandler = responseHandler;
        this.random = new Random();
    }

    //TODO: Use DVO instead of creating message here
    public Response send(String command) {
        int id = random.nextInt();
        Message message = createMessage(getMsgBundle(command, id));
        sendMessage(message);
        if (!waiter.waitForMessage(responseHandler, id)) {
            throw new IllegalStateException("Did not receive a message back from the network manager");
        }
        return responseHandler.getResponse(id);
    }

    private void sendMessage(Message msg) {
        if (null != service) {
            try {
                service.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private Message createMessage(Bundle msgBundle) {
        Message msg = Message.obtain(null, 0);
        msg.replyTo = new Messenger(responseHandler);
        msg.setData(msgBundle);
        return msg;
    }

    private Bundle getMsgBundle(String command, int id) {
        Bundle msgBundle = new Bundle();
        msgBundle.putInt("Id", id);
        msgBundle.putString("Command", command);
        return msgBundle;
    }
}
