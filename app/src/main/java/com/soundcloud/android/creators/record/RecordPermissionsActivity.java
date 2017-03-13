package com.soundcloud.android.creators.record;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.legacy.model.Recording;
import com.soundcloud.android.main.LoggedInActivity;
import com.soundcloud.android.main.Screen;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;

import javax.inject.Inject;

public class RecordPermissionsActivity extends LoggedInActivity {

    private static final int REQUEST_CODE = R.string.record_permission_rationale % 0xffff;

    @Inject Navigator navigator;

    public RecordPermissionsActivity() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this)
                        .setTitle(R.string.side_menu_record)
                        .setMessage(R.string.record_permission_rationale)
                        .setPositiveButton(R.string.ok_got_it, (dialogInterface, i) -> requestMicrophonePermission());
                builder.show();
            } else {
                requestMicrophonePermission();
            }
        }
    }

    private void requestMicrophonePermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_CODE);
    }

    @Override
    public Screen getScreen() {
        return Screen.UNKNOWN;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    navigator.openRecord(this, getRecording(), Screen.fromIntent(getIntent()));
                }
                finish();
            }
        }
    }

    @Nullable
    private Recording getRecording() {
        return getIntent().hasExtra(Recording.EXTRA) ?
        (Recording) getIntent().getParcelableExtra(Recording.EXTRA) : null;
    }
}
