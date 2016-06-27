package com.soundcloud.android.stations;

import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.presentation.CellRenderer;
import com.viewpagerindicator.CirclePageIndicator;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.List;

public class RecommendedStationsBucketRenderer implements CellRenderer<RecommendedStationsBucketItem> {

    private final RecommendedStationsAdapterFactory adapterFactory;
    private Listener listener;
    private RecommendedStationsAdapter adapter;

    public interface Listener {
        void onRecommendedStationClicked(Context context, StationRecord station);
    }

    @Inject
    public RecommendedStationsBucketRenderer(RecommendedStationsAdapterFactory adapterFactory) {
        this.adapterFactory = adapterFactory;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.station_recommendation_bucket, parent, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<RecommendedStationsBucketItem> items) {
        final ViewPager viewPager = ButterKnife.findById(itemView, R.id.stations_pager);
        final CirclePageIndicator indicator = ButterKnife.findById(itemView, R.id.page_indicator);
        RecommendedStationsBucketItem recommendedStationsItem = items.get(position);
        adapter = adapterFactory.create(listener, recommendedStationsItem.getStations());
        viewPager.setAdapter(adapter);
        indicator.setViewPager(viewPager);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void notifyAdapter() {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }
}
