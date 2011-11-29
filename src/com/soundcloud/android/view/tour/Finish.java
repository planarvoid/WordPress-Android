package com.soundcloud.android.view.tour;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;

import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class Finish extends TourLayout {
    public Finish(Context context) {
        super(context);

        ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.tour_finish, this);

        init(getContext().getString(R.string.tour_finish_title));
        //((TextView) findViewById(R.id.txt_message)).setText(Html.fromHtml(getContext().getString(R.string.tour_finish_message)));
        //((TextView) findViewById(R.id.txt_message)).setMovementMethod(LinkMovementMethod.getInstance());
        /*findViewById(R.id.btn_done).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getApp().trackEvent(Consts.Tracking.Categories.TOUR, "completed");
                finish();
            }
        });*/

    }


}
