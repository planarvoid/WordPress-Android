package com.soundcloud.android.activities;

import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import javax.inject.Inject;

class ActivitiesPresenter extends RecyclerViewPresenter<ActivityItem> {

    private final ActivitiesOperations operations;
    private final ActivitiesAdapter adapter;

    @Inject
    ActivitiesPresenter(SwipeRefreshAttacher swipeRefreshAttacher,
                        ActivitiesOperations operations,
                        ActivitiesAdapter adapter) {
        super(swipeRefreshAttacher, Options.list().build());
        this.operations = operations;
        this.adapter = adapter;
    }

    @Override
    public void onCreate(Fragment fragment, Bundle bundle) {
        super.onCreate(fragment, bundle);
        getBinding().connect();
    }

    @Override
    protected CollectionBinding<ActivityItem> onBuildBinding(Bundle fragmentArgs) {
        return CollectionBinding.from(operations.initialActivities())
                .withAdapter(adapter)
                .withPager(operations.pagingFunc())
                .build();
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }
}
