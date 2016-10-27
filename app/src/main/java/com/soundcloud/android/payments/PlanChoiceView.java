package com.soundcloud.android.payments;

import butterknife.Bind;
import butterknife.ButterKnife;
import com.soundcloud.android.R;

import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import javax.inject.Inject;

class PlanChoiceView {

    @Bind(R.id.buy_1) Button buyMidTier;
    @Bind(R.id.buy_2) Button buyHighTier;

    interface Listener {
        void onPurchaseMidTier();
        void onPurchaseHighTier();
    }

    @Inject
    PlanChoiceView() {}

    public void setupContentView(AppCompatActivity activity, Listener listener) {
        ButterKnife.bind(this, activity.findViewById(android.R.id.content));
        setupListener(listener);
    }

    private void setupListener(final Listener listener) {
        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.buy_1:
                        listener.onPurchaseMidTier();
                        break;
                    case R.id.buy_2:
                        listener.onPurchaseHighTier();
                        break;
                    default:
                        throw new IllegalArgumentException("Click on unknown View ID");
                }
            }
        };
        buyMidTier.setOnClickListener(clickListener);
        buyHighTier.setOnClickListener(clickListener);
    }

    void displayChoices(AvailableWebProducts products) {
        buyMidTier.setText(products.midTier().get().getPrice());
        buyHighTier.setText(products.highTier().get().getPrice());
    }

}
