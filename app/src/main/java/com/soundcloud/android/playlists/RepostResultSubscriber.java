package com.soundcloud.android.playlists;

import com.soundcloud.android.R;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.java.collections.PropertySet;

import android.content.Context;
import android.widget.Toast;

public class RepostResultSubscriber extends DefaultSubscriber<PropertySet> {
    private final Context context;
    private final boolean repostStatus;

    public RepostResultSubscriber(Context context, boolean repostStatus) {
        this.context = context;
        this.repostStatus = repostStatus;
    }

    @Override
    public void onNext(PropertySet ignored) {
        if (repostStatus) {
            Toast.makeText(context, R.string.reposted_to_followers, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, R.string.unposted_to_followers, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onError(Throwable e) {
        Toast.makeText(context, R.string.repost_error_toast_overflow_action, Toast.LENGTH_SHORT).show();
    }
}
