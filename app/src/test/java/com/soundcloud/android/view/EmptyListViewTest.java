package com.soundcloud.android.view;

import static org.mockito.Mockito.verify;

import com.soundcloud.android.associations.FriendFinderFragment;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(DefaultTestRunner.class)
public class EmptyListViewTest {

    EmptyListView subject;

    @Mock
    ErrorView errorView;

    @Before
    public void setUp() throws Exception {
        subject = new EmptyListView(Robolectric.application){
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
        subject.setStatus(FriendFinderFragment.Status.ERROR);
        verify(errorView).setConnectionErrorState();
    }
}
