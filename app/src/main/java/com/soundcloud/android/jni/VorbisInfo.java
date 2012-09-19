package com.soundcloud.android.jni;


/**
 * Information about a vorbis file.
 *
 * @see <a href="http://xiph.org/vorbis/doc/libvorbis/vorbis_info.html">vorbis_info</a>
 */
public class VorbisInfo {
    /** number of pcm samples */
    public long numSamples;

    /** number of audio channels */
    public int  channels;

    /** usually 44100 */
    public int  sampleRate;

    /** average bitrate */
    public long bitrate;

    /**
     * duration in secs
     * @see <a href="http://xiph.org/vorbis/doc/vorbisfile/ov_time_total.html">ov_time_total</a>
     */
    public double duration;

    @Override
    public String toString() {
        return "Info{" +
                "numSamples=" + numSamples +
                ", channels=" + channels +
                ", sampleRate=" + sampleRate +
                ", bitrate=" + bitrate +
                ", duration=" + duration +
                '}';
    }
}
