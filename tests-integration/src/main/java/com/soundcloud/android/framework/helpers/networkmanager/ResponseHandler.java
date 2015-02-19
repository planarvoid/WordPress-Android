package com.soundcloud.android.framework.helpers.networkmanager;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.util.ArrayList;


public class ResponseHandler extends Handler {
    private ArrayList<Response> responsesList = new ArrayList<Response>();

    private static final int MESSAGE_TYPE_STRING = 0;
    private final String TAG = "ResponseHandler";

    public ResponseHandler(Looper mainLooper) {
        super(mainLooper);
    }

    public boolean hasMessage(int id) {
        return (getResponse(id) != null);
    }

    public Response getResponse(int id) {
        for(Response response : responsesList) {
            if(response.getId() == id) {
                return response;
            }
        }
        return null;
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case MESSAGE_TYPE_STRING:
                handleCommandResponse(msg);
                break;
            default:
                Log.d(TAG, "Received unexpected message type. Unsure how to parse response");
                super.handleMessage(msg);
        }
    }

    public void handleCommandResponse(Message msg) {
        Response response = new Response(msg);
        responsesList.add(response);
    }
}
