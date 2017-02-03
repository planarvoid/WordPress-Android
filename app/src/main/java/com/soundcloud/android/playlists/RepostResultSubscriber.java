package com.soundcloud.android.playlists;

import com.soundcloud.android.R;
import com.soundcloud.android.associations.RepostOperations;
import com.soundcloud.android.rx.observers.DefaultSubscriber;

import android.content.Context;
import android.widget.Toast;

public class RepostResultSubscriber extends DefaultSubscriber<RepostOperations.RepostResult> {
    private final Context context;

    public RepostResultSubscriber(Context context) {
        this.context = context;
    }

    @Override
    public void onNext(RepostOperations.RepostResult result) {
        switch (result) {
            case REPOST_SUCCEEDED:
                Toast.makeText(context, R.string.reposted_to_followers, Toast.LENGTH_SHORT).show();
                break;
            case REPOST_FAILED:
                Toast.makeText(context, R.string.repost_error_toast_overflow_action, Toast.LENGTH_SHORT).show();
                break;
            case UNREPOST_SUCCEEDED:
                Toast.makeText(context, R.string.unposted_to_followers, Toast.LENGTH_SHORT).show();
                break;
            case UNREPOST_FAILED:
                Toast.makeText(context, R.string.repost_error_toast_overflow_action, Toast.LENGTH_SHORT).show();
                break;
        }
    }

    @Override
    public void onError(Throwable e) {
        Toast.makeText(context, R.string.repost_error_toast_overflow_action, Toast.LENGTH_SHORT).show();
    }

}
