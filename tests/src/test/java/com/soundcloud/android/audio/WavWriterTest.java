package com.soundcloud.android.audio;


import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.audio.writer.WavWriter;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.nio.ByteBuffer;

@RunWith(DefaultTestRunner.class)
public class WavWriterTest {

    @Test
    public void shouldCreateAValidWavFile() throws Exception {
        File out = File.createTempFile("test", "wav");
        WavWriter writer = new WavWriter(out, AudioConfig.PCM16_44100_1);

        ByteBuffer buffer = ByteBuffer.allocate(44100 * 2);
        writer.write(buffer, buffer.capacity());
        writer.close();

        WavHeader header = WavHeader.fromFile(out);
        expect(header.getAudioConfig()).toBe(AudioConfig.PCM16_44100_1);
        expect(header.getNumBytes()).toEqual(44100 * 2);
        expect(out.length()).toEqual(WavHeader.LENGTH + 44100 * 2l);
    }

    @Test
    public void shouldSetNewRecordingPosition() throws Exception {
        File out = File.createTempFile("test", "wav");
        WavWriter writer = new WavWriter(out, AudioConfig.PCM16_44100_1);

        // one sec of audio data
        ByteBuffer buffer = ByteBuffer.allocate(44100 * 2);
        writer.write(buffer, buffer.capacity());

        expect(writer.setNewPosition(500)).toBeTrue();
        writer.close();

        WavHeader header = WavHeader.fromFile(out);
        expect(header.getNumBytes()).toEqual(44100);
        expect(out.length()).toEqual(WavHeader.LENGTH + 44100l);
    }

    @Test
    public void shouldSetNewRecordingPositionAndAppendToFile() throws Exception {
        File out = File.createTempFile("test", "wav");
        WavWriter writer = new WavWriter(out, AudioConfig.PCM16_44100_1);
        // one sec of audio data
        ByteBuffer buffer = ByteBuffer.allocate(44100 * 2);
        writer.write(buffer, buffer.capacity());

        expect(writer.setNewPosition(500)).toBeTrue();

        buffer.rewind();
        writer.write(buffer, buffer.capacity());

        writer.close();

        WavHeader header = WavHeader.fromFile(out);
        expect(out.length()).toEqual(WavHeader.LENGTH + 3 * 44100l);
        expect(header.getNumBytes()).toEqual(3 * 44100);
    }

    @Test
    public void testNewRecordingPositionWithInvalidPosition() throws Exception {
        File out = File.createTempFile("test", "wav");
        WavWriter writer = new WavWriter(out, AudioConfig.PCM16_44100_1);
        expect(writer.setNewPosition(500)).toBeFalse();
        expect(writer.setNewPosition(-1)).toBeFalse();
        writer.write(ByteBuffer.allocate(44100 * 2), 44100 * 2);
        expect(writer.setNewPosition(1500)).toBeFalse();
    }
}
