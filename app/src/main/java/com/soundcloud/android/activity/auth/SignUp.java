package com.soundcloud.android.activity.auth;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.User;
import com.soundcloud.android.task.GetTokensTask;
import com.soundcloud.android.task.SignupTask;
import com.soundcloud.android.tracking.Click;
import com.soundcloud.android.tracking.Page;
import com.soundcloud.android.tracking.Tracking;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.api.Env;
import com.soundcloud.api.Token;
import org.jetbrains.annotations.Nullable;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

@Tracking(page = Page.Entry_signup__main)
public class SignUp extends Activity {
    private static final Uri TERMS_OF_USE_URL = Uri.parse("http://m.soundcloud.com/terms-of-use");
    private static final File SIGNUP_LOG = new File(Consts.EXTERNAL_STORAGE_DIRECTORY, ".dr");

    private static final int MIN_PASSWORD_LENGTH = 4;
    public static final int THROTTLE_WINDOW = 60 * 60 * 1000;
    public static final int THROTTLE_AFTER_ATTEMPT = 3;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        if (!shouldThrottle(this)){
            build();
        } else {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://m.soundcloud.com")));
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        ((SoundCloudApplication)getApplication()).track(getClass());
    }

    protected void build() {
        setContentView(R.layout.signup);

        final SoundCloudApplication app = (SoundCloudApplication) getApplication();
        final EditText emailField = (EditText) findViewById(R.id.txt_email_address);
        final EditText choosePasswordField = (EditText) findViewById(R.id.txt_choose_a_password);
        final EditText repeatPasswordField = (EditText) findViewById(R.id.txt_repeat_your_password);
        final Button signupBtn = (Button) findViewById(R.id.btn_signup);

        emailField.setText(AndroidUtils.suggestEmail(this));

        repeatPasswordField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @SuppressWarnings({"SimplifiableIfStatement"})
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE ||
                        (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER &&
                                event.getAction() == KeyEvent.ACTION_DOWN)) {
                    return signupBtn.performClick();
                } else {
                    return false;
                }
            }
        });

        signupBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                app.track(Click.Signup_Signup_done);

                if (emailField.getText().length() == 0 ||
                        choosePasswordField.getText().length() == 0 ||
                        repeatPasswordField.getText().length() == 0) {
                    AndroidUtils.showToast(SignUp.this, R.string.authentication_error_incomplete_fields);
                } else if (!ScTextUtils.isEmail(emailField.getText())) {
                    AndroidUtils.showToast(SignUp.this, R.string.authentication_error_invalid_email);
                } else if (!choosePasswordField.getText().toString().equals(repeatPasswordField.getText().toString())) {
                    AndroidUtils.showToast(SignUp.this, R.string.authentication_error_password_mismatch);
                } else if (!checkPassword(choosePasswordField.getText())) {
                    AndroidUtils.showToast(SignUp.this, R.string.authentication_error_password_too_short);
                } else {
                    final String email = emailField.getText().toString();
                    final String password = choosePasswordField.getText().toString();

                    signup(email, password);
                }
            }
        });

        ScTextUtils.clickify(((TextView) findViewById(R.id.txt_msg)),
                getResources().getString(R.string.authentication_terms_of_use),
                new ScTextUtils.ClickSpan.OnClickListener() {
                    @Override
                    public void onClick() {
                        app.track(Click.Signup_Signup_terms);
                        startActivity(new Intent(Intent.ACTION_VIEW, TERMS_OF_USE_URL));
                    }
                }, true);
    }

    private void signup(final String email, final String password) {
        final SoundCloudApplication app = (SoundCloudApplication) getApplication();

        new SignupTask(app) {
            ProgressDialog progress;
            @Override
            protected void onPreExecute() {
                progress = AndroidUtils.showProgress(SignUp.this,
                        R.string.authentication_signup_progress_message);
            }

            @Override
            protected void onPostExecute(final User user) {
                if (!isFinishing()) {
                    try {
                        progress.dismiss();
                    } catch (IllegalArgumentException ignored) {}
                }

                if (user != null) {
                    writeNewSignupToLog();

                    // need to create user account as soon as possible, so the executeRefreshTask logic in
                    // SoundCloudApplication works properly
                    final boolean signedUp = app.addUserAccount(user, app.getToken(), SignupVia.API);

                    final Bundle param = new Bundle();
                    param.putString("username", email);
                    param.putString("password", password);
                    new GetTokensTask(mApi) {
                        @Override protected void onPostExecute(Token token) {
                            if (token != null) {
                                startActivityForResult(new Intent(SignUp.this, SignupDetails.class)
                                    .putExtra(SignupVia.EXTRA,
                                            signedUp ? SignupVia.API.name : null)
                                    .putExtra("user", user)
                                    .putExtra("token", token), 0);
                            } else {
                                signupFail(null);
                            }
                        }
                    }.execute(param);
                } else {
                    signupFail(getFirstError());
                }
            }
        }.execute(email, password);
    }

    private void signupFail(@Nullable String error) {
        if (!isFinishing()) {
          new AlertDialog.Builder(this)
                  .setTitle(error != null ? R.string.authentication_signup_failure_title :  R.string.authentication_signup_error_title)
                  .setMessage(error != null ? error : getString(R.string.authentication_signup_error_message))
                  .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                      @Override
                      public void onClick(DialogInterface dialog, int which) {
                      }
                  })
                  .show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        setResult(resultCode, data);
        finish();
    }

    static boolean shouldThrottle(Context context) {
        AndroidCloudAPI api = (AndroidCloudAPI) context.getApplicationContext();
        // don't throttle sandbox requests - we need it for integration testing
        if (api.getEnv() ==  Env.SANDBOX) return false;

        final long[] signupLog = readLog();
        if (signupLog == null) {
            return false;
        } else {
            int i = signupLog.length - 1;
            while (i >= 0 &&
                    System.currentTimeMillis() - signupLog[i] < THROTTLE_WINDOW &&
                    signupLog.length - i <= THROTTLE_AFTER_ATTEMPT) {
                i--;
            }
            return signupLog.length - i > THROTTLE_AFTER_ATTEMPT;
        }
    }

    @Nullable static long[] readLog() {
        try {
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(SIGNUP_LOG));
            return (long[]) in.readObject();
        } catch (IOException e) {
            Log.e(SoundCloudApplication.TAG, "Error reading sign up log ", e);
        } catch (ClassNotFoundException e) {
            Log.e(SoundCloudApplication.TAG, "Error reading sign up log ", e);
        }
        return null;
    }

    static boolean writeNewSignupToLog() {
        return writeNewSignupToLog(System.currentTimeMillis());
    }

    static boolean writeNewSignupToLog(long timestamp) {
        long[] toWrite, current = readLog();
        if (current == null) {
            toWrite = new long[1];
        } else {
            toWrite = new long[current.length + 1];
            System.arraycopy(current, 0, toWrite, 0, current.length);
        }
        toWrite[toWrite.length - 1] = timestamp;
        return writeLog(toWrite);
    }

    static boolean writeLog(long[] toWrite) {
        try {
            IOUtils.mkdirs(SIGNUP_LOG.getParentFile());

            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(SIGNUP_LOG));
            out.writeObject(toWrite);
            out.close();
            return true;
        } catch (IOException e) {
            Log.w(SoundCloudApplication.TAG, "Error writing to sign up log ", e);
            return false;
        }
    }

    static boolean checkPassword(CharSequence password) {
        return password != null && password.length() >= MIN_PASSWORD_LENGTH;
    }
}
