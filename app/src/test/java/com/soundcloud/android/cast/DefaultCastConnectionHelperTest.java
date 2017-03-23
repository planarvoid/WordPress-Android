package com.soundcloud.android.cast;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.framework.CastSession;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class DefaultCastConnectionHelperTest extends AndroidUnitTest {

    @Mock private CastContextWrapper castContextWrapper;
    @Mock private DefaultCastButtonInstaller castButtonInstaller;

    @Mock private CastConnectionHelper.OnConnectionChangeListener listener;

    private DefaultCastConnectionHelper castConnectionHelper;

    @Before
    public void setUp() {
        castConnectionHelper = new DefaultCastConnectionHelper(castContextWrapper, castButtonInstaller);
    }

    @Test
    public void isCastingReturnsCorrectStateAfterConnectionStateChanged() {
        castConnectionHelper.notifyConnectionChange(true, true);

        assertThat(castConnectionHelper.isCasting()).isTrue();
    }

    @Test
    public void allListenersAreNotifiedAfterConnectionStateChanged() {
        CastConnectionHelper.OnConnectionChangeListener listener1 = mock(CastConnectionHelper.OnConnectionChangeListener.class);
        CastConnectionHelper.OnConnectionChangeListener listener2 = mock(CastConnectionHelper.OnConnectionChangeListener.class);
        castConnectionHelper.addOnConnectionChangeListener(listener1);
        castConnectionHelper.addOnConnectionChangeListener(listener2);
        reset(listener1);
        reset(listener2);

        castConnectionHelper.notifyConnectionChange(true, true);

        verify(listener1).onCastAvailable();
        verify(listener2).onCastAvailable();
    }

    @Test
    public void listenerIsNotifiedOfCurrentStateAsSoonAsItIsAdded() {
        castConnectionHelper.addOnConnectionChangeListener(listener);

        verify(listener).onCastUnavailable();
    }

    @Test
    public void removingListenerActuallyStopsForwardingCallsToIt() {
        castConnectionHelper.addOnConnectionChangeListener(listener);
        reset(listener);

        castConnectionHelper.removeOnConnectionChangeListener(listener);
        castConnectionHelper.notifyConnectionChange(true, true);

        verifyZeroInteractions(listener);
    }

    @Test
    public void deviceNameIsReturnedFromCastContext() {
        String expectedDeviceName = "device123";
        CastSession castSession = mock(CastSession.class);
        CastDevice castDevice = mock(CastDevice.class);
        when(castContextWrapper.getCurrentCastSession()).thenReturn(Optional.of(castSession));
        when(castSession.getCastDevice()).thenReturn(castDevice);
        when(castDevice.getFriendlyName()).thenReturn(expectedDeviceName);

        String actualDeviceName = castConnectionHelper.getDeviceName();

        assertThat(actualDeviceName).isEqualTo(expectedDeviceName);
    }

    @Test
    public void deviceNameIsReturnedAsEmptyStringIfThereIsNoDeviceConnected() {
        CastSession castSession = mock(CastSession.class);
        when(castContextWrapper.getCurrentCastSession()).thenReturn(Optional.of(castSession));
        when(castSession.getCastDevice()).thenReturn(null);

        String actualDeviceName = castConnectionHelper.getDeviceName();

        assertThat(actualDeviceName).isEqualTo("");
    }
}
