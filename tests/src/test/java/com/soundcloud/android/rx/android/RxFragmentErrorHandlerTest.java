package com.soundcloud.android.rx.android;

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
public class RxFragmentErrorHandlerTest {

    private TestErrorHandler handler;
    private @Mock Fragment mockFragment;

    private class TestErrorHandler extends RxFragmentErrorHandler<Fragment> {

        public TestErrorHandler(Fragment fragment) {
            super(fragment);
        }

        @Override
        protected void onError(Fragment fragment, Exception error) {
        }
    }

    @Before
    public void setup() {
        initMocks(this);
        handler = spy(new TestErrorHandler(mockFragment));
    }

    @Test
    public void shouldCallBackIfFragmentExistsAndAttachedToActivity() {
        when(mockFragment.isAdded()).thenReturn(true);

        Exception error = new Exception();
        handler.call(error);

        verify(handler).onError(mockFragment, error);
    }

    @Test
    public void shouldNotCallBackIfFragmentNotAttached() {
        when(mockFragment.isAdded()).thenReturn(false);

        Exception error = new Exception();
        handler.call(error);

        verifyZeroInteractions(handler);
    }

    @Test
    public void shouldNotCallBackIfFragmentIsNull() {
        handler = spy(new TestErrorHandler(null));

        handler.call(new Exception());

        verifyZeroInteractions(handler);
    }
}
