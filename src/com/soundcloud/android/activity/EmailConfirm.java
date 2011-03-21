
package com.soundcloud.android.activity;

import com.soundcloud.android.R;

import android.app.Activity;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.TextView;

public class EmailConfirm extends Activity  {


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.email_confirmation);

        findViewById(R.id.btn_resend).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });

        ((TextView) findViewById(R.id.txt_email_confirm_no_thanks)).setText(
                Html.fromHtml("<u>Thanks, I will confirm it later</u>"));
        findViewById(R.id.txt_email_confirm_no_thanks).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }

}
