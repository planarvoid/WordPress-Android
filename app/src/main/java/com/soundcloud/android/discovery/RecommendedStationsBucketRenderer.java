package com.soundcloud.android.discovery;

import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.presentation.CellRenderer;
import com.viewpagerindicator.CirclePageIndicator;

import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

class RecommendedStationsBucketRenderer implements CellRenderer<DiscoveryItem> {

    @Inject
    public RecommendedStationsBucketRenderer() {
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.station_recommendation_bucket, parent, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<DiscoveryItem> items) {
        final ViewPager viewPager = ButterKnife.findById(itemView, R.id.stations_pager);
        final CirclePageIndicator indicator = ButterKnife.findById(itemView, R.id.page_indicator);
        int cardsPerPage = itemView.getResources().getInteger(R.integer.stations_recommendation_card_count);

        viewPager.setAdapter(new StationsAdapter(cardsPerPage));
        indicator.setViewPager(viewPager);
    }

    private class StationsAdapter extends PagerAdapter {

        private final int MAX_CARDS = 12;
        private final int cardsPerPage;

        private LayoutInflater inflater;

        StationsAdapter(int cardsPerPage) {
            this.cardsPerPage = cardsPerPage;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            inflater = LayoutInflater.from(container.getContext());
            final ViewGroup view = (ViewGroup) inflater.inflate(R.layout.station_recommendation_page, container, false);

            container.addView(view);
            bindCards(view);

            return view;
        }

        private void bindCards(ViewGroup container) {
            container.removeAllViews();

            for (int i = 0; i < cardsPerPage; i++) {
                inflater.inflate(R.layout.station_recommendation_card, container);
                bindCard(container.getChildAt(i));
            }
        }

        private void bindCard(View view) {
            ButterKnife.<TextView>findById(view, R.id.station_title)
                    .setText(R.string.stations_collection_title_recent_stations);
            ButterKnife.<TextView>findById(view, R.id.station_type)
                    .setText(R.string.station_type_track);
        }

        @Override
        public int getCount() {
            return MAX_CARDS / cardsPerPage;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view.equals(object);
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }
    }
}
