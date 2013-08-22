package com.soundcloud.android.rx.observers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.soundcloud.android.adapter.ScAdapter;
import com.soundcloud.android.fragment.behavior.EmptyViewAware;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observer;

import android.support.v4.app.Fragment;
import android.view.View;

@RunWith(SoundCloudTestRunner.class)
public class PullToRefreshObserverTest {

    @Mock
    private Observer wrappedObserver;
    @Mock
    private PullToRefreshBase pullToRefreshView;
    @Mock
    private Observer observer;
    @Mock
    private ScAdapter adapter;

    private PullToRefreshObserver pullToRefreshObserver;

    @Before
    public void setUp() throws Exception {
        Fragment fragment = mock(Fragment.class, withSettings().extraInterfaces(EmptyViewAware.class));
        when(fragment.isAdded()).thenReturn(true);

        View fragmentLayout = mock(View.class);
        when(fragment.getView()).thenReturn(fragmentLayout);

        int pullToRefreshViewId = 1;
        when(fragmentLayout.findViewById(pullToRefreshViewId)).thenReturn(pullToRefreshView);

        pullToRefreshObserver = new PullToRefreshObserver(fragment, pullToRefreshViewId, adapter, wrappedObserver);
    }

    @Test
    public void shouldClearAdapterAfterAllItemsHaveBeenReceived() {
        pullToRefreshObserver.onCompleted();
        verify(adapter).clear();
        verify(adapter).notifyDataSetChanged();
    }

    @Test
    public void shouldForwardCallsToDelegateOnCompletion() {
        pullToRefreshObserver.onNext(1);
        pullToRefreshObserver.onCompleted();

        verify(wrappedObserver).onNext(1);
        verify(wrappedObserver).onCompleted();
    }

    @Ignore("onRefreshComplete is final and not mockable")
    @Test
    public void shouldHidePullToRefreshSpinnerOnCompletion() {
        pullToRefreshObserver.onCompleted();

        verify(wrappedObserver).onCompleted();
        verify(pullToRefreshView).onRefreshComplete();
    }

    @Ignore("onRefreshComplete is final and not mockable")
    @Test
    public void shouldHidePullToRefreshSpinnerOnError() {
        Exception e = new Exception();
        pullToRefreshObserver.onError(e);

        verify(wrappedObserver).onError(e);
        verify(pullToRefreshView).onRefreshComplete();
    }

}
