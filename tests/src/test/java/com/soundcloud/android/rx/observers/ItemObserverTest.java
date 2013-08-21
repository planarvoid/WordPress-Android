package com.soundcloud.android.rx.observers;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.soundcloud.android.fragment.behavior.AdapterViewAware;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.view.EmptyListView;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observer;

import android.support.v4.app.Fragment;

@RunWith(SoundCloudTestRunner.class)
public class ItemObserverTest {

    private ItemObserver observer;
    private Fragment fragment;

    @Mock
    private Observer wrappedObserver;

    @Before
    public void setUp() throws Exception {
        fragment = mock(Fragment.class, withSettings().extraInterfaces(AdapterViewAware.class));
        when(fragment.isAdded()).thenReturn(true);
        when(((AdapterViewAware) fragment).getAdapterObserver()).thenReturn(wrappedObserver);

        observer = new ItemObserver(fragment);
    }

    @Test
    public void testShowsErrorState() {
        final Exception error = new Exception();
        observer.onError(fragment, error);
        verify((AdapterViewAware) fragment).setEmptyViewStatus(EmptyListView.Status.ERROR);
        verify(wrappedObserver).onError(error);
    }

    @Test
    public void testShowsEmptyState() {
        observer.onCompleted(fragment);
        verify((AdapterViewAware) fragment).setEmptyViewStatus(EmptyListView.Status.OK);
        verify(wrappedObserver, never()).onNext(anyObject());
        verify(wrappedObserver).onCompleted();
    }

    @Test
    public void testShowsContent() {
        observer.onNext(fragment, 1);
        observer.onCompleted(fragment);

        verify((AdapterViewAware) fragment).setEmptyViewStatus(EmptyListView.Status.OK);
        verify(wrappedObserver, times(1)).onNext(1);
    }
}
