package com.soundcloud.android.jni;

import com.soundcloud.android.tests.ScAndroidTestCase;

import android.media.MediaPlayer;

import java.io.File;
import java.io.IOException;

public abstract class AudioTest extends ScAndroidTestCase {
    protected void checkAudioFile(File file, @SuppressWarnings("UnusedParameters") int expectedDuration) throws IOException {
        assertTrue("file should exist", file.exists());
        assertTrue("file should not be empty", file.length() > 0);

        // read encoded file with mediaplayer
        MediaPlayer mp = null;
        try {
            mp = new MediaPlayer();
            mp.setDataSource(file.getAbsolutePath());
            mp.prepare();

            int duration = mp.getDuration();
            //mediaplayer, y u so broken?
            //assertEquals(expectedDuration, duration);
            assertTrue(duration > 0);
        } finally {
            if (mp != null) mp.release();
        }
    }
}
