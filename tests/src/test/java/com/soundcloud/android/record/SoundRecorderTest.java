package com.soundcloud.android.record;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.audio.AudioConfig;
import com.soundcloud.android.audio.PlaybackStream;
import com.soundcloud.android.audio.WavFile;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.provider.SoundCloudDB;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.utils.IOUtils;
import com.xtremelabs.robolectric.shadows.ShadowStatFs;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;

import java.io.File;
import java.io.IOException;

@RunWith(DefaultTestRunner.class)
public class SoundRecorderTest {
    SoundRecorder recorder;
    private File wavFile = new File(getClass().getResource(WavHeaderTest.MONO_TEST_WAV).getFile());

    @Before
    public void before() {
        recorder = new SoundRecorder(DefaultTestRunner.application, AudioConfig.DEFAULT);
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(DefaultTestRunner.application);
        IOUtils.deleteDir(SoundRecorder.RECORD_DIR);
    }


    @Test(expected = IOException.class)
    public void shouldThrowIOExceptionWhenNoSDCardIsPresent() throws Exception {
        recorder.startRecording(null);
    }

    @Test(expected = IOException.class)
    public void shouldThrowIOExceptionIfNoSpaceLeft() throws Exception {
        TestHelper.setSDCardMounted();
        recorder.startRecording(null);
    }

    @Test
    public void shouldStartRecording() throws Exception {
        TestHelper.setSDCardMounted();
        ShadowStatFs.registerStats(Environment.getExternalStorageDirectory(), 100, 10, 10);
        Recording r = recorder.startRecording(null);
        expect(r).not.toBeNull();
        expect(recorder.isRecording()).toBeFalse(); // recording not supported w/ Robolectric
    }

    @Test
    public void shouldSaveState() throws Exception {
        TestHelper.setSDCardMounted();
        ShadowStatFs.registerStats(Environment.getExternalStorageDirectory(), 100, 10, 10);
        Recording r = recorder.startRecording(null);
        expect(r).not.toBeNull();

        Recording saved = recorder.saveState();
        expect(saved).not.toBeNull();
        expect(saved.isSaved()).toBeTrue();
    }

    @Test
    public void shouldSaveCurrentPlaybackSettings() throws Exception {
        TestHelper.setSDCardMounted();
        ShadowStatFs.registerStats(Environment.getExternalStorageDirectory(), 100, 10, 10);
        Recording r = recorder.startRecording(null);
        expect(r).not.toBeNull();

        expect(r.getFile().getParentFile().mkdirs()).toBeTrue();
        IOUtils.copy(wavFile, r.getFile());

        recorder.setPlaybackStream(new PlaybackStream(new WavFile(wavFile)));

        // change settings
        recorder.toggleFade();
        recorder.toggleOptimize();
        recorder.onNewStartPosition(0.1d);
        recorder.onNewEndPosition(0.9d);

        // and persist
        Recording saved = recorder.saveState();
        expect(saved).not.toBeNull();
        expect(saved.isSaved()).toBeTrue();

        Recording r2 = SoundCloudDB.getRecordingByUri(DefaultTestRunner.application.getContentResolver(),
                saved.toUri());

        assert r2 != null;
        PlaybackStream ps = r2.getPlaybackStream();
        expect(ps.isFading()).toBeTrue();
        expect(ps.isOptimized()).toBeTrue();
        expect(ps.getStartPos()).toEqual(564L);
        expect(ps.getEndPos()).toEqual(5077L);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldCheckForValidEndPosition() throws Exception {
        recorder.onNewEndPosition(90d);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldCheckForValidStartPosition() throws Exception {
        recorder.onNewStartPosition(-20d);
    }
}
