package com.soundcloud.android.playlists;

import com.soundcloud.android.R;
import com.soundcloud.android.events.RepostsStatusEvent;
import com.soundcloud.android.rx.observers.DefaultSubscriber;

import android.content.Context;
import android.widget.Toast;

public class RepostResultSubscriber extends DefaultSubscriber<RepostsStatusEvent.RepostStatus> {
    private final Context context;
    private final boolean isReposted;

    public RepostResultSubscriber(Context context, boolean isReposted) {
        this.context = context;
        this.isReposted = isReposted;
    }

    @Override
    public void onNext(RepostsStatusEvent.RepostStatus repostStatus) {
        // do not show toast when repost failed (repost status will be reversed)
        if (repostStatus.isReposted() == this.isReposted) {
            if (this.isReposted) {
                Toast.makeText(context, R.string.reposted_to_followers, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, R.string.unposted_to_followers, Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(context, R.string.repost_error_toast_overflow_action, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onError(Throwable e) {
        Toast.makeText(context, R.string.repost_error_toast_overflow_action, Toast.LENGTH_SHORT).show();
    }

}
