package com.soundcloud.android.rx.observers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.soundcloud.android.fragment.behavior.EmptyViewAware;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.view.EmptyListView;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observer;

import android.support.v4.app.Fragment;

@RunWith(SoundCloudTestRunner.class)
public class ListFragmentObserverTest {

    private ListFragmentObserver observer;
    private Fragment fragment;

    @Mock
    private Observer wrappedObserver;

    @Before
    public void setUp() throws Exception {
        fragment = mock(Fragment.class, withSettings().extraInterfaces(EmptyViewAware.class));
        when(fragment.isAdded()).thenReturn(true);

        observer = new ListFragmentObserver(fragment);
    }

    @Test
    public void testShowsErrorState() {
        final Exception error = new Exception();
        observer.onError(fragment, error);
        verify((EmptyViewAware) fragment).setEmptyViewStatus(EmptyListView.Status.ERROR);
    }

    @Test
    public void testShowsEmptyState() {
        observer.onCompleted(fragment);
        verify((EmptyViewAware) fragment).setEmptyViewStatus(EmptyListView.Status.OK);
    }

    @Test
    public void testShowsContent() {
        observer.onNext(fragment, 1);
        observer.onCompleted(fragment);

        verify((EmptyViewAware) fragment).setEmptyViewStatus(EmptyListView.Status.OK);
    }
}
