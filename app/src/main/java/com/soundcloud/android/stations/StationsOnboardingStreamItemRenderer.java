package com.soundcloud.android.stations;

import com.soundcloud.android.R;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;

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

    private final FeatureFlags featureFlags;

    @Inject
    public StationsOnboardingStreamItemRenderer(FeatureFlags featureFlags) {
        this.featureFlags = featureFlags;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        int layoutId = featureFlags.isEnabled(Flag.NEW_STREAM)
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
