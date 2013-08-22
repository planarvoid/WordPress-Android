package com.soundcloud.android.activity.create;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.service.upload.UploadService.PROCESSING_CANCELED;
import static com.soundcloud.android.service.upload.UploadService.PROCESSING_ERROR;
import static com.soundcloud.android.service.upload.UploadService.PROCESSING_PROGRESS;
import static com.soundcloud.android.service.upload.UploadService.PROCESSING_STARTED;
import static com.soundcloud.android.service.upload.UploadService.PROCESSING_SUCCESS;
import static com.soundcloud.android.service.upload.UploadService.TRANSFER_CANCELLED;
import static com.soundcloud.android.service.upload.UploadService.TRANSFER_PROGRESS;
import static com.soundcloud.android.service.upload.UploadService.TRANSFER_STARTED;
import static com.soundcloud.android.service.upload.UploadService.TRANSFER_SUCCESS;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.service.upload.UploadService;
import com.xtremelabs.robolectric.annotation.DisableStrictI18n;
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


    private UploadMonitor setupMonitor(Recording r) {
        um = new UploadMonitor();
        um.setIntent(new Intent().putExtra(Recording.EXTRA, r));
        um.onCreate(null);

        upload = (ProgressBar) um.findViewById(R.id.progress_bar_uploading);
        process = (ProgressBar) um.findViewById(R.id.progress_bar_processing);
        title = (TextView) um.findViewById(R.id.track);
        message = (TextView) um.findViewById(R.id.result_message);
        lbm = LocalBroadcastManager.getInstance(DefaultTestRunner.application);

        return um;
    }

    @Test
    public void testSuccessfulUpload() throws Exception {
        Recording r = mock(Recording.class);
        when(r.isUploading()).thenReturn(false);
        when(r.isError()).thenReturn(false);
        when(r.sharingNote(any(Resources.class))).thenReturn("Foo");

        setupMonitor(r);

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
        setupMonitor(r);
        send(r, PROCESSING_STARTED, PROCESSING_PROGRESS, PROCESSING_ERROR);
        expect(message.getText()).toEqual("OK, that went wrong.");
    }

    @Test
    public void shouldFinishActivityProcessCancel() throws Exception {
        Recording r = mock(Recording.class);
        when(r.isUploading()).thenReturn(false);
        when(r.isError()).thenReturn(false);
        setupMonitor(r);

        send(r, PROCESSING_CANCELED);
        expect(um.isFinishing()).toBeTrue();
    }

    @Test
    public void shouldFinishActivityOnUploadCancel() throws Exception {
        Recording r = mock(Recording.class);
        when(r.isUploading()).thenReturn(false);
        when(r.isError()).thenReturn(false);
        setupMonitor(r);
        send(r, TRANSFER_CANCELLED);
        expect(um.isFinishing()).toBeTrue();
    }

    @Test
    public void shouldHandleNullRecordingAndFinish() throws Exception {
        UploadMonitor monitor = new UploadMonitor();
        monitor.setIntent(new Intent().setData(Content.RECORDINGS.forId(1234)));
        monitor.onCreate(null);
        expect(monitor.isFinishing()).toBeTrue();
    }

    private void send(Recording r, String... action) {
        for (String a : action) lbm.sendBroadcast(new Intent(a).putExtra(UploadService.EXTRA_RECORDING, r));
    }
}
