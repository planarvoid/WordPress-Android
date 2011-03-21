
package com.soundcloud.android.activity;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.CloudAPI;
import com.soundcloud.android.R;
import com.soundcloud.android.task.AsyncApiTask;
import org.apache.http.HttpResponse;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.IOException;

public class EmailConfirm extends Activity  {
    public static final String PREF_LAST_REMINDED = "confirmation_last_reminded";
    public static final int REMIND_PERIOD = 86400 * 1000 * 7; // 1 week

    public static final int NO_THANKS = 0;
    public static final int RESEND = 1;
    public static final int IGNORED = 2;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final long lastReminded = getLastReminded();
        if (lastReminded > 0 &&
            System.currentTimeMillis() - lastReminded < REMIND_PERIOD) {
            Log.v(TAG, "reminder skipped");
            setResult(IGNORED);
            finish();
        } else {
            setContentView(R.layout.email_confirmation);
            findViewById(R.id.btn_resend).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setResult(RESEND);
                    new ResendConfirmationTask((CloudAPI) getApplication()).execute();
                    finish();
                }
            });

            ((TextView) findViewById(R.id.txt_email_confirm_no_thanks)).setText(
                Html.fromHtml("<u>Thanks, I will confirm it later</u>")
            );

            findViewById(R.id.txt_email_confirm_no_thanks).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setResult(NO_THANKS);
                    finish();
                }
            });
            updateLastReminded();
        }
    }

    private void updateLastReminded() {
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putLong(PREF_LAST_REMINDED, System.currentTimeMillis())
                .commit();
    }

    private long getLastReminded() {
        return PreferenceManager
                .getDefaultSharedPreferences(this)
                .getLong(PREF_LAST_REMINDED, -1);
    }

    static class ResendConfirmationTask extends AsyncApiTask<Void, Void, Boolean> {
        public ResendConfirmationTask(CloudAPI api) {
            super(api);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                HttpResponse resp = api().postContent(MY_CONFIRMATION, null);
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
