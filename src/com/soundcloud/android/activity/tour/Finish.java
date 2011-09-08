package com.soundcloud.android.activity.tour;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;

import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;

public class Finish extends TourActivity {

    @Override
    protected void onResume() {
        getApp().setCustomVar(1, "tour", "finished", 1);
        super.onResume();
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.tour_finish);

        ((TextView) findViewById(R.id.txt_title)).setText(getString(R.string.tour_finish_title));
        ((TextView) findViewById(R.id.txt_message)).setText(Html.fromHtml(getString(R.string.tour_finish_message)));
        ((TextView) findViewById(R.id.txt_message)).setMovementMethod(LinkMovementMethod.getInstance());
        findViewById(R.id.btn_done).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getApp().trackEvent(Consts.Tracking.Categories.TOUR, "completed");
                finish();
            }
        });
    }
}
