package com.soundcloud.android.playlists;

import com.soundcloud.android.associations.RepostOperations;
import com.soundcloud.android.rx.observers.DefaultSingleObserver;

import android.content.Context;

public class RepostResultSingleObserver extends DefaultSingleObserver<RepostOperations.RepostResult> {
    private final RepostResultSubscriber repostResultSubscriber;

    public RepostResultSingleObserver(Context context) {
        this.repostResultSubscriber = new RepostResultSubscriber(context);
    }

    @Override
    public void onSuccess(RepostOperations.RepostResult result) {
        repostResultSubscriber.onNext(result);
    }

    @Override
    public void onError(Throwable e) {
        repostResultSubscriber.onError(e);
    }

    @Override
    protected void onStart() {
        repostResultSubscriber.onStart();
    }
}
