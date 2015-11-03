package com.soundcloud.android.collections;

import com.soundcloud.android.R;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;

import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

class OnboardingItemCellRenderer implements CellRenderer<CollectionsItem> {
    private final FeatureFlags featureFlags;

    interface Listener {
        void onCollectionsOnboardingItemClosed(int position);
    }

    @Nullable private Listener listener;

    @Inject
    public OnboardingItemCellRenderer(FeatureFlags featureFlags) {
        this.featureFlags = featureFlags;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.collections_onboarding_item, parent, false);

        if (featureFlags.isEnabled(Flag.STATIONS_SOFT_LAUNCH)) {
            ((TextView) view.findViewById(R.id.title)).setText(R.string.collections_with_stations_onboarding_title);
        }

        return view;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    public void bindItemView(final int position, View itemView, List<CollectionsItem> items) {
        itemView.setEnabled(false);
        if (listener != null) {
            itemView.findViewById(R.id.close_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onCollectionsOnboardingItemClosed(position);
                }
            });
        }
    }

}
