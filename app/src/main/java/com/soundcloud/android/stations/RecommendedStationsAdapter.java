package com.soundcloud.android.stations;

import static com.soundcloud.android.stations.StationTypes.getHumanReadableType;

import butterknife.ButterKnife;
import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;

import android.content.res.Resources;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

@AutoFactory(allowSubclasses = true)
class RecommendedStationsAdapter extends PagerAdapter {

    private final View.OnClickListener onRecommendedStationClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (listener != null) {
                listener.onRecommendedStationClicked(view.getContext(), (StationRecord) view.getTag());
            }
        }
    };

    private final RecommendedStationsBucketRenderer.Listener listener;
    private final ImageOperations imageOperations;
    private final List<StationViewModel> stations;
    private final Resources resources;
    private final int cardsPerPage;

    private LayoutInflater inflater;

    RecommendedStationsAdapter(RecommendedStationsBucketRenderer.Listener stationCardClickListener,
                               List<StationViewModel> stations,
                               @Provided ImageOperations imageOperations,
                               @Provided Resources resources) {
        this.imageOperations = imageOperations;
        this.resources = resources;
        this.listener = stationCardClickListener;
        this.stations = stations;
        this.cardsPerPage = resources.getInteger(R.integer.stations_recommendation_card_count);
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
        for (int i = 0; i < cardsPerPage; i++) {
            if (container.getChildAt(i) == null) {
                inflater.inflate(R.layout.station_item, container);
            }

            final View card = container.getChildAt(i);
            final int stationPosition = position * cardsPerPage + i;

            if (stationPosition < stations.size()) {
                card.setVisibility(View.VISIBLE);
                bindCard(card, stationPosition);
            } else {
                card.setVisibility(View.INVISIBLE);
            }
        }
    }

    private void bindCard(View view, int position) {
        final StationViewModel stationVM = stations.get(position);
        final StationRecord station = stationVM.getStation();

        final TextView typeView = ButterKnife.findById(view, R.id.type);
        typeView.setText(getHumanReadableType(resources, station.getType()));
        typeView.setVisibility(stationVM.isPlaying() ? View.GONE : View.VISIBLE);

        ButterKnife.<TextView>findById(view, R.id.now_playing)
                .setVisibility(stationVM.isPlaying() ? View.VISIBLE : View.GONE);
        ButterKnife.<TextView>findById(view, R.id.title).setText(station.getTitle());

        imageOperations.displayInAdapterView(station,
                                             ApiImageSize.T500,
                                             ButterKnife.findById(view, R.id.artwork));
        view.setTag(station);
        view.setOnClickListener(onRecommendedStationClickListener);
    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
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
