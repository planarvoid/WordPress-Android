package com.soundcloud.android.collection;

import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.configuration.experiments.ChangeLikeToSaveExperimentStringHelper;
import com.soundcloud.android.presentation.CellRenderer;

import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

class OfflineOnboardingItemCellRenderer implements CellRenderer<CollectionItem> {

    private final ChangeLikeToSaveExperimentStringHelper changeLikeToSaveExperimentStringHelper;

    @Nullable private Listener listener;

    @Inject
    OfflineOnboardingItemCellRenderer(ChangeLikeToSaveExperimentStringHelper changeLikeToSaveExperimentStringHelper) {
        this.changeLikeToSaveExperimentStringHelper = changeLikeToSaveExperimentStringHelper;
    }

    interface Listener {
        void onCollectionsOfflineOnboardingItemClosed(int position);
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext())
                             .inflate(R.layout.collections_offline_onboarding_item, parent, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<CollectionItem> items) {
        TextView body = ButterKnife.findById(itemView, R.id.description);
        body.setText(changeLikeToSaveExperimentStringHelper.getString(ChangeLikeToSaveExperimentStringHelper.ExperimentString.COLLECTIONS_OFFLINE_ONBOARDING_BODY));

        itemView.setEnabled(false);
        if (listener != null) {
            itemView.findViewById(R.id.close_button).setOnClickListener(v -> listener.onCollectionsOfflineOnboardingItemClosed(position));
        }
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

}
