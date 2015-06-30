package com.soundcloud.android.creators.record;

import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.api.legacy.model.Recording;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import org.jetbrains.annotations.NotNull;
import rx.Subscriber;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import javax.inject.Inject;

public class UploadActivity extends ScActivity {
    @Inject RecordingOperations operations;

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
                .subscribe(uploadSubscriber());
    }

    @NotNull
    private Subscriber<Recording> uploadSubscriber() {
        return new DefaultSubscriber<Recording>() {
            @Override
            public void onNext(Recording recording) {
                startRecordingIntent(recording);
            }

            @Override
            public void onCompleted() {
                finish();
            }
        };
    }

    private void startRecordingIntent(Recording recording) {
        Intent recordIntent = new Intent(this, RecordActivity.class);
        recordIntent.putExtra(Recording.EXTRA, recording);
        recordIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(recordIntent);
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
