package com.soundcloud.android.playback.skippy;

import static org.mockito.Mockito.verify;

import com.soundcloud.android.playback.PlaybackStateTransition;
import com.soundcloud.android.playback.Player;
import com.soundcloud.android.playback.skippy.SkippyAdapter.StateChangeHandler;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import android.os.Looper;
import android.os.Message;

@RunWith(MockitoJUnitRunner.class)
public class StateChangeHandlerTest {

    private StateChangeHandler handler;

    @Mock private Looper looper;
    @Mock private Player.PlayerListener listener;
    @Mock private Message msg;
    @Mock private StateChangeHandler.StateChangeMessage stateChangeMessage;
    @Mock NetworkConnectionHelper connectionHelper;

    @Before
    public void setUp() {
        handler = new StateChangeHandler(looper, connectionHelper);
        handler.setPlayerListener(listener);
    }

    @Test
    public void shouldNotifyListenerOfStateChange() {
        msg.obj = stateChangeMessage;
        handler.handleMessage(msg);
        verify(listener).onPlaystateChanged(stateChangeMessage.stateTransition);
    }
}
