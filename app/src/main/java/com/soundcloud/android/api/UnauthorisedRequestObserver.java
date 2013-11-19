package com.soundcloud.android.api;

import com.soundcloud.android.Consts;
import com.soundcloud.android.rx.observers.DefaultObserver;

import android.content.Context;
import android.content.Intent;

public class UnauthorisedRequestObserver extends DefaultObserver<Void> {

    private Context mContext;

    public UnauthorisedRequestObserver(Context context) {
        if(context != null){
            mContext = context.getApplicationContext();
        }
    }

    @Override
    public void onCompleted() {
        if(mContext != null){
            mContext.sendBroadcast(new Intent(Consts.GeneralIntents.UNAUTHORIZED));
        }
    }
}