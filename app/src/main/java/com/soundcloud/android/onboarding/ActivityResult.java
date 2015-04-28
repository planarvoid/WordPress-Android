package com.soundcloud.android.onboarding;

import com.soundcloud.android.Consts;

import android.content.Intent;

class ActivityResult {

    public final int requestCode;
    public final int resultCode;
    public final Intent intent;

    static ActivityResult empty() {
        return new ActivityResult(Consts.NOT_SET, Consts.NOT_SET, new Intent());
    }

    ActivityResult(int requestCode, int resultCode, Intent intent) {
        this.requestCode = requestCode;
        this.resultCode = resultCode;
        this.intent = intent;
    }

}
