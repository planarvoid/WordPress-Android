package com.soundcloud.android.main;

import com.soundcloud.android.Actions;
import com.soundcloud.lightcycle.DefaultLightCycleActivity;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.NetworkConnectivityListener;
import org.jetbrains.annotations.Nullable;

import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;

public class NetworkConnectivityController extends DefaultLightCycleActivity<AppCompatActivity> {
    private static final int CONNECTIVITY_MSG = 0;
    private final Context context;
    private final Handler connHandler;
    private final NetworkConnectivityListener connectivityListener;
    private Boolean isConnected;

    @Inject
    public NetworkConnectivityController(Context context, NetworkConnectivityListener connectivityListener) {
        this.context = context;
        this.connectivityListener = connectivityListener;
        this.connHandler = new ConnectivityHandler();
    }

    @Override
    public void onCreate(AppCompatActivity activity, @Nullable Bundle bundle) {
        connectivityListener.registerHandler(connHandler, CONNECTIVITY_MSG);
    }

    @Override
    public void onStart(AppCompatActivity activity) {
        connectivityListener.startListening(context);
    }

    @Override
    public void onStop(AppCompatActivity activity) {
        connectivityListener.stopListening();
    }

    @Override
    public void onDestroy(AppCompatActivity activity) {
        connectivityListener.unregisterHandler(connHandler);
    }

    public boolean isConnected() {
        if (isConnected == null) {
            if (connectivityListener == null) {
                isConnected = true;
            } else {
                // isConnected not set yet
                NetworkInfo networkInfo = connectivityListener.getNetworkInfo();
                isConnected = networkInfo == null || networkInfo.isConnectedOrConnecting();
            }
        }
        return isConnected;
    }

    private final class ConnectivityHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case CONNECTIVITY_MSG:
                    if (context != null && msg.obj instanceof NetworkInfo) {
                        NetworkInfo networkInfo = (NetworkInfo) msg.obj;
                        isConnected = networkInfo.isConnectedOrConnecting();
                        if (isConnected) {
                            // announce potential proxy change
                            context.sendBroadcast(new Intent(Actions.CHANGE_PROXY_ACTION)
                                    .putExtra(Actions.EXTRA_PROXY, IOUtils.getProxy(context, networkInfo)));
                        }
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unknown msg.what: " + msg.what);
            }
        }
    }
}
