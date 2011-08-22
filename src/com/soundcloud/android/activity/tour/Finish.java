package com.soundcloud.android.activity.tour;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;
import com.soundcloud.android.R;

public class Finish extends Activity {

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.tour_finish);


        ((TextView) findViewById(R.id.txt_message)).setText(Html.fromHtml(getString(R.string.tour_finish_message)));
        ((TextView) findViewById(R.id.txt_message)).setMovementMethod(LinkMovementMethod.getInstance());
        findViewById(R.id.btn_done).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });


    }
}
