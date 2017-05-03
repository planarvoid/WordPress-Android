package com.soundcloud.android.discovery;


import com.soundcloud.android.Navigator;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.search.SearchItemRenderer.SearchListener;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import hu.akarnokd.rxjava.interop.RxJavaInterop;
import io.reactivex.BackpressureStrategy;
import org.jetbrains.annotations.Nullable;
import rx.Observable;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class DiscoveryPresenter extends RecyclerViewPresenter<List<DiscoveryCard>, DiscoveryCard> implements SearchListener {

    private final DiscoveryAdapter adapter;
    private final Navigator navigator;
    private final DiscoveryOperations discoveryOperations;

    @Inject
    DiscoveryPresenter(SwipeRefreshAttacher swipeRefreshAttacher, DiscoveryAdapter adapter, Navigator navigator, DiscoveryOperations discoveryOperations) {
        super(swipeRefreshAttacher, Options.defaults());
        this.adapter = adapter;
        this.navigator = navigator;
        this.discoveryOperations = discoveryOperations;
    }

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
        super.onCreate(fragment, bundle);
        getBinding().connect();
        adapter.setSearchListener(this);
    }

    @Override
    protected CollectionBinding<List<DiscoveryCard>, DiscoveryCard> onBuildBinding(Bundle bundle) {
        Observable<List<DiscoveryCard>> dataSource = RxJavaInterop.toV1Observable(discoveryOperations.discoveryCards(), BackpressureStrategy.ERROR).map(this::addSearchItem);
        return CollectionBinding
                .from(dataSource)
                .withAdapter(adapter)
                .build();
    }

    private List<DiscoveryCard> addSearchItem(List<DiscoveryCard> discoveryCards) {
        final List<DiscoveryCard> result = new ArrayList<>(discoveryCards.size() + 1);
        result.add(DiscoveryCard.forSearchItem());
        result.addAll(discoveryCards);
        return result;
    }

    @Override
    protected CollectionBinding<List<DiscoveryCard>, DiscoveryCard> onRefreshBinding() {
        Observable<List<DiscoveryCard>> dataSource = RxJavaInterop.toV1Observable(discoveryOperations.refreshDiscoveryCards(), BackpressureStrategy.ERROR).map(this::addSearchItem);
        return CollectionBinding
                .from(dataSource)
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
