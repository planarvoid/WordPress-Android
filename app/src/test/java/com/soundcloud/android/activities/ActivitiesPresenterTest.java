package com.soundcloud.android.activities;

import static java.util.Collections.singletonList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.FragmentRule;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;

public class ActivitiesPresenterTest extends AndroidUnitTest {

    @Rule public final FragmentRule fragmentRule = new FragmentRule(R.layout.default_recyclerview_with_refresh);

    private ActivitiesPresenter presenter;

    @Mock private SwipeRefreshAttacher swipeRefreshAttacher;
    @Mock private ActivitiesOperations operations;
    @Mock private ActivitiesAdapter adapter;

    @Before
    public void setUp() throws Exception {
        presenter = new ActivitiesPresenter(swipeRefreshAttacher, operations, adapter);
    }

    @Test
    public void shouldLoadInitialItemsInOnCreate() {
        ActivityItem activityItem = new ActivityItem(TestPropertySets.activityTrackLike());
        when(operations.initialActivities()).thenReturn(Observable.just(singletonList(activityItem)));

        presenter.onCreate(fragmentRule.getFragment(), null);

        verify(adapter).onNext(singletonList(activityItem));
    }
}
