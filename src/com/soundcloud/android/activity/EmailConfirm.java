
package com.soundcloud.android.activity;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.R;
import com.soundcloud.android.task.AsyncApiTask;
import org.apache.http.HttpResponse;

import android.app.Activity;
import android.content.Intent;
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

    public static final String RESEND = "com.soundcloud.android.RESEND";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final long lastReminded = getLastReminded();
        final long elapsed = System.currentTimeMillis() - lastReminded;

        if (lastReminded > 0 && elapsed < REMIND_PERIOD) {
            setResult(RESULT_CANCELED);
            finish();
        } else {
            setContentView(R.layout.email_confirmation);
            findViewById(R.id.btn_resend).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setResult(RESULT_OK, new Intent(RESEND));
                    new ResendConfirmationTask((AndroidCloudAPI) getApplication()).execute();
                    finish();
                }
            });

            ((TextView) findViewById(R.id.txt_email_confirm_no_thanks)).setText(
                Html.fromHtml("<u>Thanks, I will confirm it later</u>")
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
        public ResendConfirmationTask(AndroidCloudAPI api) {
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
