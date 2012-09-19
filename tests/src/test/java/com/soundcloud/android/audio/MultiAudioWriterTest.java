package com.soundcloud.android.audio;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.audio.writer.MultiAudioWriter;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.nio.ByteBuffer;

@RunWith(DefaultTestRunner.class)
public class MultiAudioWriterTest {
    @Mock private AudioWriter w1, w2;
    private MultiAudioWriter mw;

    @Before public void before() {
        mw = new MultiAudioWriter(w1, w2);
    }

    @Test
    public void shouldWriteToSeveralOutputs() throws Exception {
        final ByteBuffer buffer = ByteBuffer.allocate(100);

        when(w1.write(buffer, 100)).thenReturn(100);
        when(w2.write(buffer, 100)).thenReturn(100);

        expect(mw.write(buffer, 100)).toEqual(100);

        verify(w1).write(buffer, 100);
        verify(w2).write(buffer, 100);
    }

    @Test
    public void shouldFinalizeAllStreams() throws Exception {
        mw.finalizeStream();
        verify(w1).finalizeStream();
        verify(w2).finalizeStream();
    }

    @Test
    public void shouldSetNewPositionOnAllWriters() throws Exception {
        when(w1.setNewPosition(200)).thenReturn(true);
        when(w2.setNewPosition(200)).thenReturn(true);

        expect(mw.setNewPosition(200)).toBeTrue();

        verify(w1).setNewPosition(200);
        verify(w2).setNewPosition(200);
    }
}
