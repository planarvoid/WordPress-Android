package com.soundcloud.android.view;

import static org.mockito.Mockito.verify;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(SoundCloudTestRunner.class)
public class EmptyViewTest {

    private EmptyView subject;

    @Mock ErrorView errorView;

    @Before
    public void setUp() throws Exception {
        subject = new EmptyView(Robolectric.application){
            @Override
            protected ErrorView addErrorView() {
                return errorView;
            }
        };
    }

    @Test
    public void shouldSetServerErrorState() throws Exception {
        subject.setStatus(EmptyView.Status.SERVER_ERROR);
        verify(errorView).setServerErrorState();
    }

    @Test
    public void shouldSetConnectionErrorState() throws Exception {
        subject.setStatus(EmptyView.Status.CONNECTION_ERROR);
        verify(errorView).setConnectionErrorState();
    }

    @Test // clarify with product
    public void shouldSetServerErrorStateByDefault() {
        subject.setStatus(EmptyView.Status.ERROR);
        verify(errorView).setServerErrorState();
    }
}
