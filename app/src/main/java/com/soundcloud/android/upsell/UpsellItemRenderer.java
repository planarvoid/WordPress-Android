package com.soundcloud.android.upsell;

import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.presentation.CellRenderer;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

public abstract class UpsellItemRenderer<T> implements CellRenderer<T> {

    private final FeatureOperations featureOperations;

    public interface Listener<T> {
        void onUpsellItemDismissed(int position, T item);
        void onUpsellItemClicked(Context context, int position, T item);
        void onUpsellItemCreated();
    }

    private Listener<T> listener;

    UpsellItemRenderer(FeatureOperations featureOperations) {
        this.featureOperations = featureOperations;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        if (listener != null) {
            listener.onUpsellItemCreated();
        }
        return parent;
    }

    @Override
    public void bindItemView(int position, View view, List<T> items) {
        bindItemView(position, view, items.get(position));
    }

    private void bindItemView(final int position, View view, T item) {
        ButterKnife.<TextView>findById(view, R.id.title).setText(getTitle(view.getContext()));
        ButterKnife.<TextView>findById(view, R.id.description).setText(getDescription(view.getContext()));

        view.setEnabled(false);
        if (listener != null) {
            ButterKnife.findById(view, R.id.close_button).setOnClickListener(v -> listener.onUpsellItemDismissed(position, item));
            bindActionButton(view, position, item);
        }
    }

    private void bindActionButton(final View view, int position, T item) {
        final Button action = ButterKnife.findById(view, R.id.action_button);
        setButtonText(view, action);
        action.setOnClickListener(v -> listener.onUpsellItemClicked(view.getContext(), position, item));
    }

    private void setButtonText(View view, Button action) {
        if (featureOperations.isHighTierTrialEligible()) {
            action.setText(getTrialActionButtonText(view.getContext(), featureOperations.getHighTierTrialDays()));
        } else {
            action.setText(R.string.upsell_upgrade_button);
        }
    }

    public void setListener(Listener<T> listener) {
        this.listener = listener;
    }

    protected abstract String getTitle(Context context);
    protected abstract String getDescription(Context context);
    protected abstract String getTrialActionButtonText(Context context, int trialDays);
}
