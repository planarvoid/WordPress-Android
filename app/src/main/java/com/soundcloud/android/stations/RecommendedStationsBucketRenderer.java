package com.soundcloud.android.stations;

import static com.soundcloud.android.stations.StationTypes.getHumanReadableType;

import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.presentation.CellRenderer;
import com.viewpagerindicator.CirclePageIndicator;

import android.content.Context;
import android.content.res.Resources;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

public class RecommendedStationsBucketRenderer implements CellRenderer<RecommendedStationsBucket> {

    private final ImageOperations imageOperations;
    private final Resources resources;
    private Listener listener;

    private View.OnClickListener onRecommendedStationClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (listener != null) {
                listener.onRecommendedStationClicked(view.getContext(), (StationRecord) view.getTag());
            }
        }
    };

    public interface Listener {
        void onRecommendedStationClicked(Context context, StationRecord station);
    }

    @Inject
    public RecommendedStationsBucketRenderer(ImageOperations imageOperations,
                                             Resources resources) {
        this.imageOperations = imageOperations;
        this.resources = resources;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.station_recommendation_bucket, parent, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<RecommendedStationsBucket> items) {
        final ViewPager viewPager = ButterKnife.findById(itemView, R.id.stations_pager);
        final CirclePageIndicator indicator = ButterKnife.findById(itemView, R.id.page_indicator);
        int cardsPerPage = itemView.getResources().getInteger(R.integer.stations_recommendation_card_count);
        RecommendedStationsBucket recommendedStationsBucket = items.get(position);

        viewPager.setAdapter(new StationsAdapter(cardsPerPage, recommendedStationsBucket.stationRecords));
        indicator.setViewPager(viewPager);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    private class StationsAdapter extends PagerAdapter {

        private final int cardsPerPage;
        private final List<StationRecord> stations;

        private LayoutInflater inflater;

        StationsAdapter(int cardsPerPage, List<StationRecord> stations) {
            this.cardsPerPage = cardsPerPage;
            this.stations = stations;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            inflater = LayoutInflater.from(container.getContext());
            final ViewGroup view = (ViewGroup) inflater.inflate(R.layout.station_recommendation_page, container, false);

            container.addView(view);
            bindCards(view, position);

            return view;
        }

        private void bindCards(ViewGroup container, int position) {
            container.removeAllViews();

            for (int i = 0; i < cardsPerPage; i++) {
                inflater.inflate(R.layout.station_item, container);
                View card = container.getChildAt(i);

                int stationPosition = position * cardsPerPage + i;

                if (stationPosition < stations.size()) {
                    card.setVisibility(View.VISIBLE);
                    bindCard(card, stationPosition);
                }
            }
        }

        private void bindCard(View view, int position) {
            final StationRecord stationRecord = stations.get(position);
            ButterKnife.<TextView>findById(view, R.id.title).setText(stationRecord.getTitle());
            ButterKnife.<TextView>findById(view, R.id.type).setText(getHumanReadableType(resources, stationRecord.getType()));
            imageOperations.displayInAdapterView(stationRecord,
                                                 ApiImageSize.T500,
                                                 ButterKnife.<ImageView>findById(view, R.id.artwork));
            view.setTag(stationRecord);
            view.setOnClickListener(onRecommendedStationClickListener);
        }

        @Override
        public int getCount() {
            return (int) Math.ceil((float) stations.size() / (float) cardsPerPage);
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
