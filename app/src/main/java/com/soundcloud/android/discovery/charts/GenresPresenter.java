package com.soundcloud.android.discovery.charts;

import com.soundcloud.android.api.model.ChartCategory;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.java.collections.Lists;
import rx.functions.Func1;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import javax.inject.Inject;
import java.util.List;

public class GenresPresenter extends RecyclerViewPresenter<List<Chart>, ChartListItem> {

    private static final Func1<List<Chart>, List<ChartListItem>> TO_PRESENTATION_MODELS = charts -> Lists.transform(charts, input -> new ChartListItem(input.trackArtworks(),
                                                                                                                                               input.genre(),
                                                                                                                                               input.displayName(),
                                                                                                                                               input.bucketType(),
                                                                                                                                               input.type(),
                                                                                                                                               input.category()));
    private final ChartsOperations chartsOperations;
    private final ChartListItemAdapter chartListItemAdapter;

    @Inject
    public GenresPresenter(SwipeRefreshAttacher swipeRefreshAttacher,
                           ChartsOperations chartsOperations,
                           ChartListItemAdapter chartListItemAdapter) {
        super(swipeRefreshAttacher);
        this.chartsOperations = chartsOperations;
        this.chartListItemAdapter = chartListItemAdapter;
    }

    @Override
    public void onCreate(Fragment fragment, Bundle bundle) {
        super.onCreate(fragment, bundle);
        getBinding().connect();
    }

    @Override
    protected CollectionBinding<List<Chart>, ChartListItem> onBuildBinding(Bundle bundle) {
        final ChartCategory chartCategory = (ChartCategory) bundle.getSerializable(GenresFragment.EXTRA_CHART_CATEGORY);
        return CollectionBinding
                .from(chartsOperations.genresByCategory(chartCategory), TO_PRESENTATION_MODELS)
                .withAdapter(chartListItemAdapter)
                .build();
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }
}
