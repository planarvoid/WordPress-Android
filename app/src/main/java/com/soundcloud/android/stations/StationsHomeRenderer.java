package com.soundcloud.android.stations;

import butterknife.ButterKnife;
import com.soundcloud.android.R;
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

class StationsHomeRenderer implements CellRenderer<StationBucket> {

    private final StationRenderer stationRenderer;

    @Inject
    public StationsHomeRenderer(StationRenderer stationRenderer) {
        this.stationRenderer = stationRenderer;
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
        recyclerView.setAdapter(new StationsPreviewAdapter(stationRenderer));
    }

    @Override
    public void bindItemView(int i, View parent, final List<StationBucket> buckets) {
        final StationBucket stationBucket = buckets.get(i);
        ((TextView) parent.findViewById(R.id.title)).setText(stationBucket.getTitle());
        bindStationsPreview(parent, stationBucket);
    }

    private void bindStationsPreview(View view, StationBucket stationBucket) {
        final StationsPreviewAdapter adapter = ((StationsPreviewAdapter) findRecyclerView(view).getAdapter());
        adapter.setStationBucket(stationBucket);
        adapter.notifyDataSetChanged();
    }

    private RecyclerView findRecyclerView(View view) {
        return ButterKnife.findById(view, R.id.ak_recycler_view);
    }

    private static class StationsPreviewAdapter extends RecyclerView.Adapter<StationsPreviewAdapter.StationViewHolder> {
        private final StationRenderer stationRenderer;
        private StationBucket stationBucket;

        StationsPreviewAdapter(StationRenderer stationRenderer) {
            this.stationRenderer = stationRenderer;
        }

        public void setStationBucket(StationBucket stationBucket) {
            this.stationBucket = stationBucket;
        }

        @Override
        public StationViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new StationViewHolder(stationRenderer.createItemView(parent));
        }

        @Override
        public void onBindViewHolder(StationViewHolder holder, int position) {
            stationRenderer.bindItemView(position, holder.itemView, stationBucket.getStations());
        }

        @Override
        public int getItemCount() {
            return stationBucket.getStations().size();
        }

        public static class StationViewHolder extends RecyclerView.ViewHolder {
            public StationViewHolder(View itemView) {
                super(itemView);
            }
        }
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
