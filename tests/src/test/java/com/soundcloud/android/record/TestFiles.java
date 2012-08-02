package com.soundcloud.android.record;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.InputStream;
import java.net.URISyntaxException;

public enum TestFiles {
    /** 44100 16bit signed, 1 channel, 00:00:05.64, 497708 bytes  */
    MONO_TEST_WAV("mono_16bit_44khz.wav"),

    /** 44100 16bit signed, 2 channels, 00:00:00.27, 47148 bytes  */
    STEREO_TEST_WAV("stereo_16bit_44khz.wav"),

    /** 8000 16bit signed, 1 channel, 00:00:05.55, 88844 bytes  */
    PCM16_8000_1_WAV("mono_16bit_8khz.wav");

    public final String name;

    public File asFile() {
        try {
            return new File(getClass().getResource(name).toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public @NotNull InputStream asStream() {
        InputStream stream = getClass().getResourceAsStream(name);
        if (stream == null) throw new RuntimeException("file "+name+" not found");
        return stream;
    }

    TestFiles(String s) {
        this.name = s;
    }
}
