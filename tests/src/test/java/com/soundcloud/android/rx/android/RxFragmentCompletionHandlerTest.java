package com.soundcloud.android.rx.android;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.support.v4.app.Fragment;

@RunWith(SoundCloudTestRunner.class)
public class RxFragmentCompletionHandlerTest {

    private TestCompletedHandler handler;
    private @Mock Fragment mockFragment;

    private class TestCompletedHandler extends RxFragmentCompletionHandler<Fragment> {

        public TestCompletedHandler(Fragment fragment) {
            super(fragment);
        }

        @Override
        protected void onCompleted(Fragment fragment) {
        }
    }

    @Before
    public void setup() {
        initMocks(this);
        handler = spy(new TestCompletedHandler(mockFragment));
    }

    @Test
    public void shouldCallBackIfFragmentExistsAndAttachedToActivity() {
        when(mockFragment.isAdded()).thenReturn(true);

        handler.call();

        verify(handler).onCompleted(mockFragment);
    }

    @Test
    public void shouldCallBackIfFragmentNotAttachedAndActivityNotRequired() {
        when(mockFragment.isAdded()).thenReturn(false);

        handler.setRequireActivity(false);
        handler.call();

        verify(handler).onCompleted(mockFragment);
    }

    @Test
    public void shouldNotCallBackIfActivityRequiredAndFragmentNotAttached() {
        when(mockFragment.isAdded()).thenReturn(false);

        handler.setRequireActivity(true);
        handler.call();

        verify(handler, never()).onCompleted(mockFragment);
    }

    @Test
    public void shouldNotCallBackIfFragmentIsNull() {
        handler = spy(new TestCompletedHandler(null));

        handler.call();

        verifyZeroInteractions(handler);
    }
}
