package com.soundcloud.android.likes;

import com.soundcloud.android.R;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.java.collections.PropertySet;

import android.content.Context;
import android.widget.Toast;

public class LikeToggleSubscriber extends DefaultSubscriber<PropertySet> {
    private final Context context;
    private final boolean likeStatus;

    public LikeToggleSubscriber(Context context, boolean likeStatus) {
        this.context = context;
        this.likeStatus = likeStatus;
    }

    @Override
    public void onNext(PropertySet ignored) {
        if (likeStatus) {
            Toast.makeText(context, R.string.like_toast_overflow_action, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, R.string.unlike_toast_overflow_action, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onError(Throwable e) {
        Toast.makeText(context, R.string.like_error_toast_overflow_action, Toast.LENGTH_SHORT).show();
    }
}
