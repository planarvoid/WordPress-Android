package com.soundcloud.android.framework.helpers.networkmanager;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Messenger;

public class NetworkManager {
    private static final String ANDROIDNETWORKMANAGER_PACKAGE = "com.soundcloud.androidnetworkmanager";
    private static final String ANDROIDNETWORKMANAGER_SERVICE = "com.soundcloud.androidnetworkmanager.NetworkManagerService";
    private NetworkServiceClient networkServiceClient;
    private Context context;
    private final ResponseHandler responseHandler;
    private static final String TURN_ON_WIFI = "TURN_ON_WIFI";
    private static final String TURN_OFF_WIFI = "TURN_OFF_WIFI";
    private static final String IS_WIFI_ENABLED = "IS_WIFI_ENABLED";

    private ServiceConnection connection = new ServiceConnection() {
        private Messenger service;

        public void onServiceConnected(ComponentName className, IBinder service) {
            this.service = new Messenger(service);
            networkServiceClient = new NetworkServiceClient(this.service, responseHandler);
        }

        public void onServiceDisconnected(ComponentName className) {
            this.service = null;
            networkServiceClient = null;
        }
    };

    public NetworkManager(Context context) {
        this.context = context;
        this.responseHandler = new ResponseHandler(context.getMainLooper());
    }

    public boolean bind() {
        return context.bindService(getIntent(), connection, Context.BIND_AUTO_CREATE);
    }

    public void unbind() {
        context.unbindService(connection);
    }

    public Response switchWifiOn() {
        return sendIfConnected(TURN_ON_WIFI);
    }

    public Response switchWifiOff() {
        return sendIfConnected(TURN_OFF_WIFI);
    }

    public boolean isWifiEnabled() {
        return Boolean.parseBoolean(sendIfConnected(IS_WIFI_ENABLED).getResponse());
    }

    private Response sendIfConnected(String command) {
        return networkServiceClient == null
                ? Response.EMPTY
                : networkServiceClient.send(command);
    }

    private Intent getIntent() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(ANDROIDNETWORKMANAGER_PACKAGE,
                ANDROIDNETWORKMANAGER_SERVICE));
        return intent;
    }
}
