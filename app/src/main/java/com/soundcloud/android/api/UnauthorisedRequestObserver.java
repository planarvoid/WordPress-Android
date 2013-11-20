package com.soundcloud.android.api;

import com.soundcloud.android.Consts;
import com.soundcloud.android.rx.observers.DefaultObserver;

import android.content.Context;
import android.content.Intent;

class UnauthorisedRequestObserver extends DefaultObserver {

    private Context mContext;

    public UnauthorisedRequestObserver(Context context) {
        mContext = context.getApplicationContext();
    }

    @Override
    public void onCompleted() {
        mContext.sendBroadcast(new Intent(Consts.GeneralIntents.UNAUTHORIZED));
    }
}