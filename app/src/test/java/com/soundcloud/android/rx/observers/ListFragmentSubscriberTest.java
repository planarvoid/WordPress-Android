package com.soundcloud.android.rx.observers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.view.EmptyListView;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observer;

@RunWith(SoundCloudTestRunner.class)
public class ListFragmentSubscriberTest {

    private ListFragmentSubscriber observer;
    private EmptyViewAware emptyViewHolder;

    @Mock
    private Observer wrappedObserver;

    @Before
    public void setUp() throws Exception {
        emptyViewHolder = mock(EmptyViewAware.class);
        observer = new ListFragmentSubscriber(emptyViewHolder);
    }

    @Test
    public void testShowsErrorState() {
        final Exception error = new Exception();
        observer.onError(error);
        verify(emptyViewHolder).setEmptyViewStatus(EmptyListView.Status.ERROR);
    }

    @Test
    public void testShowsEmptyState() {
        observer.onCompleted();
        verify(emptyViewHolder).setEmptyViewStatus(EmptyListView.Status.OK);
    }

    @Test
    public void testShowsContent() {
        observer.onNext(1);
        observer.onCompleted();

        verify(emptyViewHolder).setEmptyViewStatus(EmptyListView.Status.OK);
    }
}
