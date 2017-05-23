package com.soundcloud.android.collection;

import com.soundcloud.android.R;
import com.soundcloud.android.configuration.experiments.ChangeLikeToSaveExperimentStringHelper;
import com.soundcloud.android.configuration.experiments.ChangeLikeToSaveExperimentStringHelper.ExperimentString;
import com.soundcloud.android.presentation.CellRenderer;

import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

class OnboardingItemCellRenderer implements CellRenderer<CollectionItem> {

    private final ChangeLikeToSaveExperimentStringHelper changeLikeToSaveExperimentStringHelper;

    @Nullable private Listener listener;

    interface Listener {
        void onCollectionsOnboardingItemClosed(int position);
    }

    @Inject
    OnboardingItemCellRenderer(ChangeLikeToSaveExperimentStringHelper changeLikeToSaveExperimentStringHelper) {
        this.changeLikeToSaveExperimentStringHelper = changeLikeToSaveExperimentStringHelper;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext())
                                  .inflate(R.layout.collections_onboarding_item, parent, false);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    public void bindItemView(final int position, View itemView, List<CollectionItem> items) {
        TextView title = (TextView) itemView.findViewById(R.id.title);
        title.setText(changeLikeToSaveExperimentStringHelper.getString(ExperimentString.COLLECTIONS_ONBOARDING_TITLE));

        itemView.setEnabled(false);
        if (listener != null) {
            itemView.findViewById(R.id.close_button).setOnClickListener(v -> listener.onCollectionsOnboardingItemClosed(position));
        }
    }

}
