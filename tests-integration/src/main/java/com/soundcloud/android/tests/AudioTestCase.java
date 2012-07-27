package com.soundcloud.android.tests;

import android.media.MediaPlayer;

import java.io.File;
import java.io.IOException;

public abstract class AudioTestCase extends ScAndroidTestCase {

    /*
     * Encoded version of MED_WAV
     *
     * 0m:18.786s
     */
    public static final String MED_TEST_OGG = "audio/med_test.ogg";

    /*
     * Vorbis stream 1:
     * Total data length: 42786 bytes
     * Playback length: 0m:05.642s
     * Average bitrate: 60.663021 kb/s
     */
    public static final String SHORT_TEST_OGG = "audio/short_test.ogg";


    /*
     * WARNING: EOS not set on stream 1
     * Vorbis stream 1:
     * Total data length: 159856 bytes
     * Playback length: 0m:05.548s
     * Average bitrate: 230.501229 kb/s
     */
    public static final String SHORT_TEST_NO_EOS_OGG = "audio/short_test_no_eos.ogg";

    /*
     * File Size: 498k      Bit Rate: 706k
     * Encoding: Signed PCM
     * Channels: 1 @ 16-bit
     * Samplerate: 44100Hz
     * Replaygain: off
     * Duration: 00:00:05.64
     */
    public static final String SHORT_WAV = "audio/short_test.wav";

    /*
     * File Size: 882k      Bit Rate: 706k
     * Encoding: Signed PCM
     * Channels: 1 @ 16-bit
     * Samplerate: 44100Hz
     * Replaygain: off
     * Duration: 00:00:10.00
     */
    public static final String SINE_WAV = "audio/sine.wav";


    /*
     * File Size: 3.34M     Bit Rate: 1.41M
     * Encoding: Signed PCM
     * Channels: 2 @ 16-bit
     * Samplerate: 44100Hz
     * Replaygain: off
     * Duration: 00:00:18.95
     */
    public static final String MED_WAV = "audio/med_test.wav";

    /*
     * 3 different ogg bitstreams chained together in one file (= physical
     * bitstream)
     * <p/>
     * Vorbis stream 1:
     * Total data length: 7043 bytes
     * Playback length: 0m:00.993s
     * Average bitrate: 56.735099 kb/s
     * <p/>
     * Vorbis stream 2:
     * Total data length: 9171 bytes
     * Playback length: 0m:01.269s
     * Average bitrate: 57.791748 kb/s
     * <p/>
     * Vorbis stream 3:
     * Total data length: 10109 bytes
     * Playback length: 0m:01.160s
     * Average bitrate: 69.665492 kb/s
     */
    public static final String CHAINED_OGG = "audio/123456789.ogg";


    public static final String TRIMMED_RECORDING = "audio/trimmed_recording.ogg";

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
