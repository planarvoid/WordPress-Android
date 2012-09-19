package com.soundcloud.android.view.tour;

import com.soundcloud.android.R;
import com.soundcloud.android.tracking.Page;
import com.soundcloud.android.tracking.Tracking;

import android.content.Context;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

@Tracking(page = Page.Entry_tour__done)
public class Finish extends TourLayout {
    public Finish(Context context) {
        super(context, R.layout.tour_finish);
        ((TextView) findViewById(R.id.txt_message)).setText(Html.fromHtml(getContext().getString(R.string.tour_finish_message)));
        ((TextView) findViewById(R.id.txt_message)).setMovementMethod(LinkMovementMethod.getInstance());
    }
}
