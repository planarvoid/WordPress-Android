package com.soundcloud.android.rx.observers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.soundcloud.android.adapter.ScAdapter;
import com.soundcloud.android.fragment.behavior.AdapterViewAware;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observer;
import rx.android.RxFragmentObserver;

import android.support.v4.app.Fragment;
import android.view.View;

@RunWith(SoundCloudTestRunner.class)
public class PullToRefreshObserverTest {

    private final int pullToRefreshViewId = 1;
    private Fragment fragment;

    @Mock
    private RxFragmentObserver wrappedObserver;
    @Mock
    private PullToRefreshBase pullToRefreshView;
    @Mock
    private Observer observer;
    @Mock
    private ScAdapter adapter;

    private PullToRefreshObserver pullToRefreshObserver;

    @Before
    public void setUp() throws Exception {
        fragment = mock(Fragment.class, withSettings().extraInterfaces(AdapterViewAware.class));
        when(fragment.isAdded()).thenReturn(true);
        when(((AdapterViewAware) fragment).getAdapterObserver()).thenReturn(observer);

        View fragmentLayout = mock(View.class);
        when(fragment.getView()).thenReturn(fragmentLayout);
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

        verify(wrappedObserver).onNext(fragment, 1);
        verify(wrappedObserver).onCompleted(fragment);
    }

    @Ignore("onRefreshComplete is final and not mockable")
    @Test
    public void shouldHidePullToRefreshSpinnerOnCompletion() {
        pullToRefreshObserver.onCompleted();

        verify(wrappedObserver).onCompleted(fragment);
        verify(pullToRefreshView).onRefreshComplete();
    }

    @Ignore("onRefreshComplete is final and not mockable")
    @Test
    public void shouldHidePullToRefreshSpinnerOnError() {
        Exception e = new Exception();
        pullToRefreshObserver.onError(e);

        verify(wrappedObserver).onError(fragment, e);
        verify(pullToRefreshView).onRefreshComplete();
    }

}
