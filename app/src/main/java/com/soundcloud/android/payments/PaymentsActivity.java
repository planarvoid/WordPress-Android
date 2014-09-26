package com.soundcloud.android.payments;

import butterknife.ButterKnife;
import butterknife.OnClick;
import com.soundcloud.android.R;
import com.soundcloud.android.actionbar.ActionBarController;
import com.soundcloud.android.main.ScActivity;

import android.os.Bundle;
import android.widget.Toast;

public class PaymentsActivity extends ScActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.payments_activity);
        ButterKnife.inject(this);
    }

    @OnClick(R.id.subscription_buy) void buy() {
        Toast.makeText(this, "User clicked buy", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected ActionBarController createActionBarController() {
        // No overflow or search
        return null;
    }

}
