package com.soundcloud.android.jni;

public class Info {
    /** number of pcm samples */
    public long numSamples;
    /** number of audio channels */
    public int  channels;
    /** usually 44100 */
    public int  sampleRate;
    /** average bitrate */
    public long bitrate;
    /** duration in msecs */
    public double duration;
}
