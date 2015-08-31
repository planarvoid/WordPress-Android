package com.soundcloud.android.playback.skippy;

import static org.mockito.Mockito.verify;

import com.soundcloud.android.playback.Player;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.utils.NetworkConnectionHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.os.Looper;
import android.os.Message;

@RunWith(SoundCloudTestRunner.class)
public class StateChangeHandlerTest {
    private SkippyAdapter.StateChangeHandler handler;
    @Mock private Looper looper;
    @Mock private Player.PlayerListener listener;
    @Mock private Message msg;
    @Mock private Player.StateTransition stateTransition;
    @Mock NetworkConnectionHelper connectionHelper;


    @Before
    public void setUp(){
        handler = new SkippyAdapter.StateChangeHandler(looper, connectionHelper);
        handler.setPlayerListener(listener);
    }
    @Test
    public void shouldNotifyListenerOfStateChange(){
        msg.obj = stateTransition;
        handler.handleMessage(msg);
        verify(listener).onPlaystateChanged(stateTransition);
    }

}
