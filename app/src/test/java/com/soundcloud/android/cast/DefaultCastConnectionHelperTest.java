package com.soundcloud.android.cast;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.sample.castcompanionlibrary.cast.VideoCastManager;
import com.google.sample.castcompanionlibrary.cast.callbacks.IVideoCastConsumer;
import com.soundcloud.android.cast.CastConnectionHelper.CastConnectionListener;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import android.content.Context;
import android.view.Menu;

@RunWith(SoundCloudTestRunner.class)
public class DefaultCastConnectionHelperTest {

    private DefaultCastConnectionHelper defaultCastConnectionHelper;

    @Mock private Context context;
    @Mock private VideoCastManager videoCastmanager;
    @Mock private CastConnectionListener castConnectionListener;
    @Captor private ArgumentCaptor<IVideoCastConsumer> castConsumerCaptor;

    @Before
    public void setUp() throws Exception {
        defaultCastConnectionHelper = new DefaultCastConnectionHelper(context, videoCastmanager);
    }

    @Test
    public void addMediRouterButtonAddsMediaRouterButtonThroughVideoCastManager() {
        final Menu menu = mock(Menu.class);
        defaultCastConnectionHelper.addMediaRouterButton(menu, 123);

        verify(videoCastmanager).addMediaRouterButton(menu, 123);
    }

    @Test
    public void reconnectSessionIfPossibleReconnectsThroughVideoCastManager() {
        defaultCastConnectionHelper.reconnectSessionIfPossible();

        verify(videoCastmanager).reconnectSessionIfPossible(context, false);
    }

    @Test
    public void onApplicationConnectedIsPassedToTheConnectionListener() throws Exception {
        defaultCastConnectionHelper.addConnectionListener(castConnectionListener);

        captureVideoCastConsumer().onApplicationConnected(null, null, false);

        verify(castConnectionListener).onConnectedToReceiverApp();
    }

    @Test
    public void onApplicationConnectedIsNotPassedToARemovedConnectionListener() throws Exception {
        defaultCastConnectionHelper.addConnectionListener(castConnectionListener);
        defaultCastConnectionHelper.removeConnectionListener(castConnectionListener);

        captureVideoCastConsumer().onApplicationConnected(null, null, false);

        verify(castConnectionListener, never()).onConnectedToReceiverApp();
    }

    @Test
    public void isConnectedReturnsIsConnectedFromVideoCastManager() throws Exception {
        when(videoCastmanager.isConnected()).thenReturn(true);
        expect(defaultCastConnectionHelper.isConnected()).toBeTrue();
    }

    private IVideoCastConsumer captureVideoCastConsumer() {
        verify(videoCastmanager).addVideoCastConsumer(castConsumerCaptor.capture());
        return castConsumerCaptor.getValue();
    }
}