package com.soundcloud.android.settings;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.IOUtils;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.Subscriptions;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.os.Bundle;

import javax.inject.Inject;

public class ClearCacheDialog extends DialogFragment {

    private static final String TAG = "clear_cache";

    private Subscription subscription = Subscriptions.empty();

    @Inject ImageOperations imageOperations;

    public static void show(FragmentManager fragmentManager) {
        new ClearCacheDialog().show(fragmentManager, TAG);
    }

    public ClearCacheDialog() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        ProgressDialog dialog = new ProgressDialog(getActivity());
        dialog.setTitle(R.string.cache_clearing);
        dialog.setMessage(getString(R.string.cache_clearing_message));
        dialog.setIndeterminate(true);
        dialog.setCancelable(false);

        subscription.unsubscribe();
        subscription = clearCache()
                .subscribeOn(ScSchedulers.LOW_PRIO_SCHEDULER)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new ClearCompleteSubscriber());
        return dialog;
    }

    private Observable<Void> clearCache() {
        return Observable.create(new Observable.OnSubscribe<Void>() {
            @Override
            public void call(Subscriber<? super Void> subscriber) {
                imageOperations.clearDiskCache();
                IOUtils.cleanDirs(Consts.EXTERNAL_MEDIAPLAYER_STREAM_DIRECTORY, Consts.EXTERNAL_SKIPPY_STREAM_DIRECTORY);
                subscriber.onNext(null);
                subscriber.onCompleted();
            }
        });
    }

    private class ClearCompleteSubscriber extends DefaultSubscriber<Void> {
        @Override
        public void onNext(Void args) {
            dismiss();
        }
    }

}
