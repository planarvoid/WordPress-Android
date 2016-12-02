package com.soundcloud.android.settings;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.playback.StreamCacheConfig;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.waveform.WaveformOperations;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.Toast;

import javax.inject.Inject;
import java.io.File;

public class ClearCacheDialog extends DialogFragment {

    private static final String TAG = "clear_cache";

    private Subscription subscription = RxUtils.invalidSubscription();

    @Inject Context appContext;
    @Inject ImageOperations imageOperations;
    @Inject WaveformOperations waveformOperations;
    @Inject StreamCacheConfig.SkippyConfig skippyConfig;
    @Inject StreamCacheConfig.FlipperConfig flipperConfig;

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
                waveformOperations.clearWaveforms();
                imageOperations.clearDiskCache();
                clear(skippyConfig.getStreamCacheDirectory());
                clear(flipperConfig.getStreamCacheDirectory());
                subscriber.onCompleted();
            }

            private void clear(File directory) {
                if (directory != null) {
                    IOUtils.cleanDirs(directory);
                }
            }
        });
    }

    private class ClearCompleteSubscriber extends DefaultSubscriber<Void> {
        @Override
        public void onCompleted() {
            Toast.makeText(appContext, R.string.cache_cleared, Toast.LENGTH_SHORT).show();
            dismiss();
        }
    }

}
