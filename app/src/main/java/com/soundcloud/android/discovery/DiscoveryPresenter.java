package com.soundcloud.android.discovery;


import com.soundcloud.android.Navigator;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.search.SearchItemRenderer.SearchListener;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import org.jetbrains.annotations.Nullable;
import rx.Observable;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import javax.inject.Inject;
import java.util.List;

public class DiscoveryPresenter extends RecyclerViewPresenter<List<DiscoveryCard>, DiscoveryCard> implements SearchListener {

    private final DiscoveryAdapter adapter;
    private final Navigator navigator;

    @Inject
    DiscoveryPresenter(SwipeRefreshAttacher swipeRefreshAttacher, DiscoveryAdapter adapter, Navigator navigator) {
        super(swipeRefreshAttacher);
        this.adapter = adapter;
        this.navigator = navigator;
    }

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
        super.onCreate(fragment, bundle);
        getBinding().connect();
        adapter.setSearchListener(this);
    }

    @Override
    protected CollectionBinding<List<DiscoveryCard>, DiscoveryCard> onBuildBinding(Bundle bundle) {
        return CollectionBinding
                .from(Observable.just(DiscoveryCard.forSearchItem()).toList())
                .withAdapter(adapter)
                .build();
    }

    @Override
    protected CollectionBinding<List<DiscoveryCard>, DiscoveryCard> onRefreshBinding() {
        return CollectionBinding
                .from(Observable.just(DiscoveryCard.forSearchItem()).toList())
                .withAdapter(adapter)
                .build();
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }

    @Override
    public void onSearchClicked(Context context) {
        navigator.openSearch((Activity) context);
    }
}
