package com.soundcloud.android.discovery;

import com.soundcloud.android.api.model.ChartCategory;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.sync.charts.ApiChart;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.functions.Function;
import rx.functions.Func1;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import javax.inject.Inject;
import java.util.List;

public class GenresPresenter extends RecyclerViewPresenter<List<ApiChart>, ChartListItem> {

    private static final Func1<List<ApiChart>, List<ChartListItem>> TO_PRESENTATION_MODELS = new Func1<List<ApiChart>, List<ChartListItem>>() {
        public List<ChartListItem> call(List<ApiChart> apiCharts) {
            return Lists.transform(apiCharts, new Function<ApiChart, ChartListItem>() {
                public ChartListItem apply(ApiChart input) {
                    return new ChartListItem(input.tracks().getCollection(), input.genre());
                }
            });
        }
    };
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
    protected CollectionBinding<List<ApiChart>, ChartListItem> onBuildBinding(Bundle bundle) {
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
