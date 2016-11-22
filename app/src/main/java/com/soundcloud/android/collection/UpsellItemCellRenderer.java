package com.soundcloud.android.collection;

import com.soundcloud.android.R;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.presentation.CellRenderer;

import android.content.Context;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import javax.inject.Inject;
import java.util.List;

class UpsellItemCellRenderer implements CellRenderer<CollectionItem> {

    private final FeatureOperations featureOperations;

    interface Listener {
        void onUpsellClose(int position);
        void onUpsell(Context context);
    }

    @Nullable private Listener listener;

    @Inject UpsellItemCellRenderer(FeatureOperations featureOperations) {
        this.featureOperations = featureOperations;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.collections_upsell_item, parent, false);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    public void bindItemView(final int position, View itemView, List<CollectionItem> items) {
        itemView.setEnabled(false);
        if (listener != null) {
            final View.OnClickListener clickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switch(v.getId()) {
                        case R.id.close_button:
                            listener.onUpsellClose(position);
                            break;
                        case R.id.upsell_button:
                            listener.onUpsell(v.getContext());
                            break;
                        default:
                            break;
                    }
                }
            };
            itemView.findViewById(R.id.close_button).setOnClickListener(clickListener);
            Button upgrade = (Button) itemView.findViewById(R.id.upsell_button);
            configureUpgradeButton(upgrade, clickListener);
        }
    }

    private void configureUpgradeButton(Button upgrade, View.OnClickListener clickListener) {
        upgrade.setOnClickListener(clickListener);
        if (featureOperations.isHighTierTrialEligible()) {
            upgrade.setText(upgrade.getResources().getString(R.string.conversion_buy_trial,
                    featureOperations.getHighTierTrialDays()));
        } else {
            upgrade.setText(R.string.upsell_upgrade_button);
        }
    }

}
