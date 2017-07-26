package com.soundcloud.android.olddiscovery.charts;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.navigation.NavigationTarget;
import com.soundcloud.android.navigation.Navigator;
import com.soundcloud.android.presentation.RefreshableScreen;
import com.soundcloud.android.rx.observers.LambdaSubscriber;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.LeakCanaryWrapper;
import com.soundcloud.android.utils.Urns;
import com.soundcloud.android.view.MultiSwipeRefreshLayout;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.lightcycle.LightCycleSupportFragment;
import rx.Subscription;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

public class ChartTracksFragment extends LightCycleSupportFragment<ChartTracksFragment> implements RefreshableScreen {

    public static final String EXTRA_TYPE = "chartType";
    public static final String EXTRA_CATEGORY = "chartCategory";
    public static final String EXTRA_GENRE_URN = "chartGenreUrn";
    public static final String EXTRA_HEADER = "chartHeader";

    @Inject @LightCycle ChartTracksPresenter presenter;
    @Inject Navigator navigator;
    @Inject LeakCanaryWrapper leakCanaryWrapper;

    private Subscription errorSubscription;

    static ChartTracksFragment create(ChartType type, Urn genreUrn) {
        final Bundle bundle = new Bundle();
        bundle.putSerializable(EXTRA_TYPE, type);
        Urns.writeToBundle(bundle, EXTRA_GENRE_URN, genreUrn);

        final ChartTracksFragment fragment = new ChartTracksFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    public ChartTracksFragment() {
        setRetainInstance(true);
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.default_recyclerview_with_refresh, container, false);
    }

    @Override
    public MultiSwipeRefreshLayout getRefreshLayout() {
        return (MultiSwipeRefreshLayout) getView().findViewById(R.id.str_layout);
    }

    @Override
    public View[] getRefreshableViews() {
        return new View[]{presenter.getRecyclerView(), presenter.getEmptyView()};
    }

    @Override
    public void onStart() {
        super.onStart();
        errorSubscription = presenter.invalidGenreError()
                                     .subscribe(LambdaSubscriber.onNext(invalidGenre -> {
                                         ErrorUtils.handleSilentException("Charts Deeplink: Genre not found", new ChartsDeeplinkNotFoundException());
                                         navigator.navigateTo(NavigationTarget.forAllGenres());
                                         getActivity().finish();
                                     }));
    }

    @Override
    public void onStop() {
        errorSubscription.unsubscribe();
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        leakCanaryWrapper.watch(this);
    }
}
