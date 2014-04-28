
package com.soundcloud.android.onboarding.auth;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.Actions;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.api.AsyncApiTask;
import com.soundcloud.android.api.PublicApi;
import com.soundcloud.android.api.PublicCloudAPI;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.IOException;

public class EmailConfirmationActivity extends ScActivity {

    private PublicCloudAPI publicCloudAPI;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        publicCloudAPI = new PublicApi(this);

        setContentView(R.layout.email_confirmation_activity);
        findViewById(R.id.btn_resend).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_OK, new Intent(Actions.RESEND));
                new ResendConfirmationTask(publicCloudAPI).execute((Void)null);
                finish();
            }
        });

        ((TextView) findViewById(R.id.txt_email_confirm_no_thanks)).setText(
            Html.fromHtml(getString(R.string.email_confirmation_thanks_later))
        );

        findViewById(R.id.txt_email_confirm_no_thanks).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
        updateLastReminded();
    }

    private void updateLastReminded() {
        accountOperations.setAccountData(Consts.PrefKeys.LAST_EMAIL_CONFIRMATION_REMINDER,
                String.valueOf(System.currentTimeMillis()));
    }

    static class ResendConfirmationTask extends AsyncApiTask<Void, Void, Boolean> {
        public ResendConfirmationTask(PublicCloudAPI api) {
            super(api);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                HttpResponse resp = api.post(Request.to(MY_CONFIRMATION));
                switch (resp.getStatusLine().getStatusCode()) {
                    case SC_ACCEPTED:   return true;  // email sent
                    case SC_OK:         return false; // already confirmed, no email sent
                    default: {
                        Log.w(TAG, "unexpected status code " + resp.getStatusLine());
                        return false;
                    }
                }
            } catch (IOException e) {
                Log.w(TAG, "error requesting confirmation email", e);
                return false;
            }
        }
    }
}
