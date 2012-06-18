
package com.soundcloud.android.activity.auth;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.Actions;
import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.task.AsyncApiTask;
import com.soundcloud.android.tracking.Page;
import com.soundcloud.android.tracking.Tracking;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.IOException;

@Tracking(page = Page.Entry_confirm_your_email)
public class EmailConfirm extends Activity  {
    public static final String PREF_LAST_REMINDED = "confirmation_last_reminded";
    public static final int REMIND_PERIOD = 86400 * 1000 * 7; // 1 week

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final long lastReminded = getLastReminded();

        if (lastReminded > 0 && System.currentTimeMillis() - lastReminded < REMIND_PERIOD) {
            setResult(RESULT_CANCELED);
            finish();
        } else {
            setContentView(R.layout.email_confirmation);
            findViewById(R.id.btn_resend).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setResult(RESULT_OK, new Intent(Actions.RESEND));
                    new ResendConfirmationTask((AndroidCloudAPI) getApplication()).execute((Void)null);
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
    }

    @Override
    protected void onResume() {
        super.onResume();
        getApp().track(getClass());
    }

    private void updateLastReminded() {
        getApp().setAccountData(PREF_LAST_REMINDED, System.currentTimeMillis() + "");
    }

    // XXX this check should happen earlier
    private long getLastReminded() {
        return getApp().getAccountDataLong(PREF_LAST_REMINDED);
    }

    private SoundCloudApplication getApp() {
        return (SoundCloudApplication) getApplication();
    }

    static class ResendConfirmationTask extends AsyncApiTask<Void, Void, Boolean> {
        public ResendConfirmationTask(AndroidCloudAPI api) {
            super(api);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                HttpResponse resp = mApi.post(Request.to(MY_CONFIRMATION));
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
