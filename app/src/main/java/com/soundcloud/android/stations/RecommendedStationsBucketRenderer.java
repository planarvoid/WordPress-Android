package com.soundcloud.android.stations;

import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.view.pageindicator.CirclePageIndicator;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.lang.ref.WeakReference;
import java.util.List;

public class RecommendedStationsBucketRenderer implements CellRenderer<RecommendedStationsBucketItem> {

    private final RecommendedStationsAdapterFactory adapterFactory;
    private Listener listener;
    private WeakReference<ViewPager> viewPagerRef;

    public void detach() {
        if (viewPagerRef != null) {
            final ViewPager viewPager = viewPagerRef.get();
            if (viewPager != null) {
                viewPager.setAdapter(null);
                viewPager.removeAllViews();
            }
            viewPagerRef = null;
        }
    }

    public interface Listener {
        void onRecommendedStationClicked(Context context, StationRecord station);
    }

    @Inject
    RecommendedStationsBucketRenderer(RecommendedStationsAdapterFactory adapterFactory) {
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
        ViewPager viewPager = ButterKnife.findById(itemView, R.id.stations_pager);
        final CirclePageIndicator indicator = ButterKnife.findById(itemView, R.id.page_indicator);
        RecommendedStationsBucketItem recommendedStationsItem = items.get(position);
        RecommendedStationsAdapter adapter = adapterFactory.create(listener, recommendedStationsItem.getStations());
        viewPager.setAdapter(adapter);
        indicator.setViewPager(viewPager);
        viewPagerRef = new WeakReference<>(viewPager);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

}
