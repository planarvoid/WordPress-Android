package com.soundcloud.android.creators.record;

import com.soundcloud.android.Actions;
import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.api.legacy.model.Recording;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.view.screen.BaseLayoutHelper;
import org.jetbrains.annotations.NotNull;
import rx.Subscriber;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import javax.inject.Inject;
import java.io.File;

public class UploadActivity extends ScActivity {

    @Inject BaseLayoutHelper baseLayoutHelper;
    @Inject RecordingOperations operations;
    @Inject Navigator navigator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.resolve);

        if (!isUploadIntent()) {
            finish();
            return;
        }

        Intent intent = getIntent();
        Uri stream = intent.getParcelableExtra(Intent.EXTRA_STREAM);

        operations.upload(SoundRecorder.UPLOAD_DIR, stream, intent.getType(), getContentResolver())
                .subscribe(uploadSubscriber(intent));
    }

    @Override
    protected void setActivityContentView() {
        baseLayoutHelper.setContainerLayout(this);
    }

    @NotNull
    private Subscriber<Recording> uploadSubscriber(final Intent intent) {
        return new DefaultSubscriber<Recording>() {
            @Override
            public void onNext(Recording recording) {
                recording.title = intent.getStringExtra(Actions.EXTRA_TITLE);
                recording.is_private = !intent.getBooleanExtra(Actions.EXTRA_PUBLIC, true);
                recording.tags = intent.getStringArrayExtra(Actions.EXTRA_TAGS);
                recording.description = intent.getStringExtra(Actions.EXTRA_DESCRIPTION);
                recording.genre = intent.getStringExtra(Actions.EXTRA_GENRE);

                Uri artwork = intent.getParcelableExtra(Actions.EXTRA_ARTWORK);

                if (artwork != null && "file".equals(artwork.getScheme())) {
                    recording.artwork_path = new File(artwork.getPath());
                }

                navigator.openRecord(UploadActivity.this, recording);
            }

            @Override
            public void onCompleted() {
                finish();
            }
        };
    }

    private boolean isUploadIntent() {
        final Intent intent = getIntent();
        final String action = intent.getAction();

        return intent.hasExtra(Intent.EXTRA_STREAM) &&
                (Intent.ACTION_SEND.equals(action) ||
                        Actions.SHARE.equals(action) ||
                        Actions.EDIT.equals(action));
    }
}
