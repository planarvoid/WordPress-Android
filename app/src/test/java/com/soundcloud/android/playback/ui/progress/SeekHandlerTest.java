package com.soundcloud.android.playback.ui.progress;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.os.Message;

public class SeekHandlerTest extends AndroidUnitTest {

    private static final float SEEK_POS = .5f;

    @Mock ScrubController scrubController;

    private SeekHandler seekHandler;
    private Message message;

    @Before
    public void setUp() throws Exception {
        seekHandler = new SeekHandler(scrubController);
        message = Message.obtain();
        message.obj = SEEK_POS;
    }

    @Test
    public void handleMessageFinishesSeek() {
        seekHandler.handleMessage(message);
        verify(scrubController).finishSeek(SEEK_POS);
    }

    @Test
    public void handleMessageSetsPendingSeekIfDragging() {
        when(scrubController.isDragging()).thenReturn(true);
        seekHandler.handleMessage(message);
        verify(scrubController).setPendingSeek(SEEK_POS);
    }
}
