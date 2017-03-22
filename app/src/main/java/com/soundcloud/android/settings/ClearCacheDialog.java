package com.soundcloud.android.settings;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.playback.StreamCacheConfig;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.rx.observers.DefaultDisposableCompletableObserver;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.waveform.WaveformOperations;
import io.reactivex.Completable;
import io.reactivex.CompletableEmitter;
import io.reactivex.CompletableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

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

    @Inject Context appContext;
    @Inject ImageOperations imageOperations;
    @Inject WaveformOperations waveformOperations;
    @Inject StreamCacheConfig.SkippyConfig skippyConfig;
    @Inject StreamCacheConfig.FlipperConfig flipperConfig;

    private Disposable disposable = RxUtils.emptyDisposable();

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

        disposable.dispose();
        disposable = clearCache()
                .subscribeOn(ScSchedulers.RX_LOW_PRIORITY_SCHEDULER)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new ClearCompleteObserver());

        return dialog;
    }

    private Completable clearCache() {
        return Completable.create(new CompletableOnSubscribe() {
            @Override
            public void subscribe(CompletableEmitter emitter) {
                waveformOperations.clearWaveforms();
                imageOperations.clearDiskCache();
                clear(skippyConfig.getStreamCacheDirectory());
                clear(flipperConfig.getStreamCacheDirectory());
                emitter.onComplete();
            }

            private void clear(File directory) {
                if (directory != null) {
                    IOUtils.cleanDirs(directory);
                }
            }
        });
    }

    private class ClearCompleteObserver extends DefaultDisposableCompletableObserver {
        @Override
        public void onComplete() {
            Toast.makeText(appContext, R.string.cache_cleared, Toast.LENGTH_SHORT).show();
            dismiss();
        }
    }
}
