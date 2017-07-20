package com.soundcloud.android.playback.common;

import static org.mockito.Mockito.verify;

import com.soundcloud.android.playback.Player;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import android.os.Looper;
import android.os.Message;

@RunWith(MockitoJUnitRunner.class)
public class StateChangeHandlerTest {

    private StateChangeHandler handler;

    @Mock private Looper looper;
    @Mock private Player.PlayerListener listener;
    @Mock private Message msg;
    @Mock private StateChangeHandler.StateChangeMessage stateChangeMessage;

    @Before
    public void setUp() {
        handler = new StateChangeHandler(looper);
        handler.setPlayerListener(listener);
    }

    @Test
    public void shouldNotifyListenerOfStateChange() {
        msg.obj = stateChangeMessage;
        handler.handleMessage(msg);
        verify(listener).onPlaystateChanged(stateChangeMessage.stateTransition);
    }
}
