package com.soundcloud.android.record;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.audio.AudioConfig;
import com.soundcloud.android.audio.PlaybackStream;
import com.soundcloud.android.audio.reader.WavReader;
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

import java.io.File;
import java.io.IOException;

@RunWith(DefaultTestRunner.class)
public class SoundRecorderTest {
    SoundRecorder recorder;
    private File wavFile = new File(getClass().getResource(WavHeaderTest.MONO_TEST_WAV).getFile());

    @Before
    public void before() {
        recorder = new SoundRecorder(DefaultTestRunner.application, AudioConfig.DEFAULT);
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
        recorder.mReaderThread.join(); // wait for failure
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

        recorder.setPlaybackStream(new PlaybackStream(new WavReader(wavFile)));

        // change settings
        recorder.toggleFade();
        recorder.toggleOptimize();
        recorder.onNewStartPosition(0.1d, 100);
        recorder.onNewEndPosition(0.9d, 100);

        // and persist
        Recording saved = recorder.saveState();
        expect(saved).not.toBeNull();
        expect(saved.isSaved()).toBeTrue();

        Recording r2 = SoundCloudDB.getRecordingByUri(DefaultTestRunner.application.getContentResolver(),
                saved.toUri());

        assert r2 != null;
        expect(r2).not.toBeNull();
        PlaybackStream ps = r2.getPlaybackStream();
        expect(ps).not.toBeNull();
        expect(ps.isFading()).toBeTrue();
        expect(ps.isOptimized()).toBeTrue();
        expect(ps.getStartPos()).toEqual(564L);
        expect(ps.getEndPos()).toEqual(5077L);
    }
}
