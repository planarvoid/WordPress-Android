package com.soundcloud.android.stations;

import butterknife.ButterKnife;
import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.presentation.CellRenderer;

import android.content.Context;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

class StationsBucketRenderer implements CellRenderer<StationBucket> {

    private final StationRenderer stationRenderer;
    private final Navigator navigator;
    private final PlayQueueManager playQueueManager;

    @Inject
    public StationsBucketRenderer(StationRenderer stationRenderer,
                                  Navigator navigator,
                                  PlayQueueManager playQueueManager) {
        this.stationRenderer = stationRenderer;
        this.navigator = navigator;
        this.playQueueManager = playQueueManager;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        final View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.stations_home_item, parent, false);
        initRecyclerViewForStationsPreview(findRecyclerView(itemView));
        return itemView;
    }

    private void initRecyclerViewForStationsPreview(RecyclerView recyclerView) {
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new WrapContentGridLayoutManager(recyclerView.getContext()));
        recyclerView.setAdapter(new StationsAdapter(stationRenderer));
    }

    @Override
    public void bindItemView(int i, final View parent, final List<StationBucket> buckets) {
        final StationBucket stationBucket = buckets.get(i);
        ButterKnife.<TextView>findById(parent, R.id.title).setText(stationBucket.getTitle());
        bindShowAllView(parent.findViewById(R.id.view_all), stationBucket);
        bindStationsPreview(parent, stationBucket);
    }

    private void bindShowAllView(View view, StationBucket bucket) {
        if (bucket.getStationViewModels().size() > bucket.getBucketSize()) {
            view.setVisibility(View.VISIBLE);
            view.setOnClickListener(onViewAllClick(bucket.getCollectionType()));
        } else {
            view.setVisibility(View.GONE);
        }
    }

    private void bindStationsPreview(View view, StationBucket stationBucket) {
        final StationsAdapter adapter = ((StationsAdapter) findRecyclerView(view).getAdapter());
        final List<StationViewModel> stationViewModels = stationBucket.getStationViewModels();

        adapter.clear();
        for (int i = 0; i < stationViewModels.size() && i < stationBucket.getBucketSize(); i++) {
            adapter.addItem(stationViewModels.get(i));
        }

        adapter.notifyDataSetChanged();
    }

    private RecyclerView findRecyclerView(View view) {
        return ButterKnife.findById(view, R.id.ak_recycler_view);
    }

    private View.OnClickListener onViewAllClick(final int type) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigator.openViewAllStations(view.getContext(), type);
            }
        };
    }

    /**
     * By default, recycler views cannot use `wrap_content` as a height attribute.
     * However, in our use case, we need to support `wrap_content` to support nested
     * recycler views.
     *
     * This LayoutManager can be applied to a RecyclerView which needs to `wrap_content`
     * and support a grid structure.
     *
     * This LayoutManager only works with grids whose children are all of the same size.
     */
    private static class WrapContentGridLayoutManager extends GridLayoutManager {
        public WrapContentGridLayoutManager(Context context) {
            super(context, context.getResources().getInteger(R.integer.stations_grid_span_count));
        }

        @Override
        public void onMeasure(RecyclerView.Recycler recycler, RecyclerView.State state, int widthSpec, int heightSpec) {
            final int widthSize = View.MeasureSpec.getSize(widthSpec);
            final int height = measureHeight(recycler, state, widthSize);

            setMeasuredDimension(widthSize, height);
        }

        private int measureHeight(RecyclerView.Recycler recycler, RecyclerView.State state, int width) {
            if (getItemCount() == 0) {
                return 0;
            }

            final View child = recycler.getViewForPosition(0);
            final int itemHeight = getChildMeasuredHeight(width, child);
            final int numberOfRows = getNumberOfRows(state.getItemCount());
            recycler.recycleView(child);
            return numberOfRows * itemHeight;
        }

        private int getNumberOfRows(int itemCount) {
            return (int) Math.ceil(itemCount / (double) getSpanCount());
        }

        private int getChildMeasuredHeight(int width, View child) {
            measureChild(child, width / getSpanCount());
            return getDecoratedMeasuredHeight(child);
        }

        private void measureChild(View child, int width) {
            final int widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY);
            final int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            child.measure(widthSpec, heightSpec);
        }
    }

}
