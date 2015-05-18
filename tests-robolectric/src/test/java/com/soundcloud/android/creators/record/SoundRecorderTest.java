package com.soundcloud.android.creators.record;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.creators.record.RemainingTimeCalculator.KEEP_BLOCKS;

import com.soundcloud.android.api.legacy.model.Recording;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.testsupport.TestHelper;
import com.soundcloud.android.utils.IOUtils;
import com.xtremelabs.robolectric.shadows.ShadowStatFs;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.os.Environment;

import java.io.File;
import java.io.IOException;

@RunWith(DefaultTestRunner.class)
public class SoundRecorderTest {
    SoundRecorder recorder;
    private File wavFile = TestFiles.MONO_TEST_WAV.asFile();

    @Before
    public void before() {
        recorder = new SoundRecorder(DefaultTestRunner.application, AudioConfig.DEFAULT);
        IOUtils.deleteDir(SoundRecorder.RECORD_DIR);
    }

    @Ignore // fails with JNI error on Java 7
    @Test(expected = IOException.class)
    public void shouldThrowIOExceptionWhenNoSDCardIsPresent() throws Exception {
        recorder.startRecording();
    }

    @Ignore // fails with JNI error on Java 7
    @Test(expected = IOException.class)
    public void shouldThrowIOExceptionIfNoSpaceLeft() throws Exception {
        TestHelper.enableSDCard();
        recorder.startRecording();
    }

    @Ignore // fails with JNI error on Java 7
    @Test
    public void shouldStartRecording() throws Exception {
        TestHelper.enableSDCard();
        ShadowStatFs.registerStats(Environment.getExternalStorageDirectory(), 200, KEEP_BLOCKS+1, KEEP_BLOCKS+1);
        Recording r = recorder.startRecording();
        expect(r).not.toBeNull();
        recorder.readerThread.join(); // wait for failure
        expect(recorder.isRecording()).toBeFalse(); // recording not supported w/ Robolectric
    }

    @Ignore // fails with JNI error on Java 7
    @Test
    public void shouldSaveState() throws Exception {
        TestHelper.enableSDCard();
        ShadowStatFs.registerStats(Environment.getExternalStorageDirectory(), 200, KEEP_BLOCKS+1, KEEP_BLOCKS+1);
        Recording r = recorder.startRecording();
        expect(r).not.toBeNull();

        Recording saved = recorder.saveState();
        expect(saved).not.toBeNull();
//        expect(saved.isSaved()).toBeTrue();
    }

}
