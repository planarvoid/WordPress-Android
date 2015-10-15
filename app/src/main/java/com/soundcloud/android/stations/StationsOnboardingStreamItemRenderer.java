package com.soundcloud.android.stations;

import com.soundcloud.android.R;
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

    @Inject
    public StationsOnboardingStreamItemRenderer() {
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.stations_onboarding_stream_notification_list_item, parent, false);
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
