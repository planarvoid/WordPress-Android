package com.soundcloud.android.stations;

import com.soundcloud.android.R;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.stream.StreamItem;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.List;

public class StationsOnboardingStreamItemRenderer implements CellRenderer<StreamItem> {

    public interface Listener {
        void onStationOnboardingItemClosed(int position);
    }

    private Listener listener;

    @Inject
    public StationsOnboardingStreamItemRenderer() {
        // everything for dagger
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext())
                             .inflate(R.layout.stations_onboarding_stream_notification_card, parent, false);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    public void bindItemView(final int position, View itemView, List<StreamItem> notifications) {
        itemView.setEnabled(false);
        if (listener != null) {
            itemView.findViewById(R.id.close_button).setOnClickListener(v -> listener.onStationOnboardingItemClosed(position));
        }
    }

}
