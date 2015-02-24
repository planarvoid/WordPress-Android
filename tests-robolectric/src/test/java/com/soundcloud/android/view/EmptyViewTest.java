package com.soundcloud.android.view;

import static org.mockito.Mockito.verify;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(SoundCloudTestRunner.class)
public class EmptyViewTest {

    EmptyView subject;

    @Mock
    ErrorView errorView;

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
    public void shouldSetUnexpectedResponseStateClientErrorCode() throws Exception {
        subject.setStatus(HttpStatus.SC_NOT_FOUND);
        verify(errorView).setUnexpectedResponseState();
    }

    @Test
    public void shouldSetUnexpectedResponseStateWithServerErrorCode() throws Exception {
        subject.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        verify(errorView).setUnexpectedResponseState();
    }

    @Test
    public void shouldSetConnectionErrorState() throws Exception {
        subject.setStatus(EmptyView.Status.ERROR);
        verify(errorView).setConnectionErrorState();
    }
}
