package com.soundcloud.android.activity.create;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.service.upload.UploadService.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.service.upload.UploadService;
import com.xtremelabs.robolectric.annotation.DisableStrictI18n;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Intent;
import android.content.res.Resources;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.ProgressBar;
import android.widget.TextView;

@RunWith(DefaultTestRunner.class)
@DisableStrictI18n
public class UploadMonitorTest {
    UploadMonitor um;
    LocalBroadcastManager lbm;
    ProgressBar process, upload;
    TextView title, message;

    @Before public void before() {
        um = new UploadMonitor();
        um.onCreate(null);

        upload = (ProgressBar) um.findViewById(R.id.progress_bar_uploading);
        process = (ProgressBar) um.findViewById(R.id.progress_bar_processing);
        title = (TextView) um.findViewById(R.id.track);
        message = (TextView) um.findViewById(R.id.result_message);

        lbm = LocalBroadcastManager.getInstance(DefaultTestRunner.application);
    }

    @Test
    public void testSuccessfulUpload() throws Exception {
        Recording r = mock(Recording.class);
        when(r.isUploading()).thenReturn(false);
        when(r.isError()).thenReturn(false);
        when(r.sharingNote(any(Resources.class))).thenReturn("Foo");

        um.setRecording(r);

        expect(title.getText()).toEqual("Foo");
        expect(process.isIndeterminate()).toBeFalse();
        expect(upload.isIndeterminate()).toBeFalse();

        send(r, PROCESSING_STARTED, PROCESSING_PROGRESS);
        expect(process.isIndeterminate()).toBeFalse();

        send(r, PROCESSING_SUCCESS);
        expect(process.isIndeterminate()).toBeFalse();
        expect(process.getProgress()).toEqual(100);

        send(r, TRANSFER_STARTED, TRANSFER_PROGRESS);
        expect(upload.isIndeterminate()).toBeFalse();

        send(r, TRANSFER_SUCCESS);
        expect(upload.getProgress()).toEqual(100);
        expect(process.getProgress()).toEqual(100);

        expect(message.getText()).toEqual("Yay, that worked!");
    }

    @Test
    public void testFailedUpload() {
        Recording r = mock(Recording.class);
        when(r.isUploading()).thenReturn(false);
        when(r.isError()).thenReturn(false);
        um.setRecording(r);
        send(r, PROCESSING_STARTED, PROCESSING_PROGRESS, PROCESSING_ERROR);
        expect(message.getText()).toEqual("Ok, that went wrong.");
    }

    @Test
    public void shouldFinishActvityProcessCancel() throws Exception {
        Recording r = mock(Recording.class);
        when(r.isUploading()).thenReturn(false);
        when(r.isError()).thenReturn(false);
        um.setRecording(r);
        send(r, PROCESSING_CANCELED);
        expect(um.isFinishing()).toBeTrue();
    }

    @Test
    public void shouldFinishActvityOnUploadCancel() throws Exception {
        Recording r = mock(Recording.class);
        when(r.isUploading()).thenReturn(false);
        when(r.isError()).thenReturn(false);
        um.setRecording(r);
        send(r, TRANSFER_CANCELLED);
        expect(um.isFinishing()).toBeTrue();
    }

    private void send(Recording r, String... action) {
        for (String a : action) lbm.sendBroadcast(new Intent(a).putExtra(UploadService.EXTRA_RECORDING, r));
    }
}
