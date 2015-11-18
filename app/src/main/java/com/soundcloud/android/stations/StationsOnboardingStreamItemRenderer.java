package com.soundcloud.android.stations;

import com.soundcloud.android.R;
import com.soundcloud.android.configuration.experiments.StreamDesignExperiment;
import com.soundcloud.android.presentation.CellRenderer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.List;

public class StationsOnboardingStreamItemRenderer implements CellRenderer<StationOnboardingStreamItem> {

    public interface Listener {
        void onStationOnboardingItemClosed(int position);
    }

    private Listener listener;

    private final StreamDesignExperiment streamExperiment;

    @Inject
    public StationsOnboardingStreamItemRenderer(StreamDesignExperiment streamExperiment) {
        this.streamExperiment = streamExperiment;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        int layoutId = streamExperiment.isCardDesign()
                ? R.layout.stations_onboarding_stream_notification_card
                : R.layout.stations_onboarding_stream_notification_item;
        return LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    public void bindItemView(final int position, View itemView, List<StationOnboardingStreamItem> notifications) {
        itemView.setEnabled(false);
        if (listener != null) {
            itemView.findViewById(R.id.close_button).setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    listener.onStationOnboardingItemClosed(position);
                }
            });
        }
    }

}
