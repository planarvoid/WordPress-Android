package com.soundcloud.android.playlists;

import com.soundcloud.android.R;
import com.soundcloud.android.associations.RepostOperations;
import com.soundcloud.android.rx.observers.DefaultSingleObserver;

import android.content.Context;
import android.widget.Toast;

public class RepostResultSingleObserver extends DefaultSingleObserver<RepostOperations.RepostResult> {
    private final Context context;

    public RepostResultSingleObserver(Context context) {
        this.context = context;
    }

    @Override
    public void onSuccess(RepostOperations.RepostResult result) {
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
        super.onSuccess(result);
    }

    @Override
    public void onError(Throwable throwable) {
        super.onError(throwable);
        Toast.makeText(context, R.string.repost_error_toast_overflow_action, Toast.LENGTH_SHORT).show();
    }
}
