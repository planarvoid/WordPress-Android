package com.soundcloud.android.activity.auth;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.User;
import com.soundcloud.android.task.GetTokensTask;
import com.soundcloud.android.task.SignupTask;
import com.soundcloud.android.tracking.Click;
import com.soundcloud.android.tracking.Page;
import com.soundcloud.android.tracking.Tracking;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.api.Token;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

@Tracking(page = Page.Entry_signup__main)
public class SignUp extends Activity {
    public static final Uri TERMS_OF_USE_URL = Uri.parse("http://m.soundcloud.com/terms-of-use");

    private static final int MIN_PASSWORD_LENGTH = 4;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        build();
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

        emailField.setText(CloudUtils.suggestEmail(this));

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
                    CloudUtils.showToast(SignUp.this, R.string.authentication_error_incomplete_fields);
                } else if (!CloudUtils.checkEmail(emailField.getText())) {
                    CloudUtils.showToast(SignUp.this, R.string.authentication_error_invalid_email);
                } else if (!choosePasswordField.getText().toString().equals(repeatPasswordField.getText().toString())) {
                    CloudUtils.showToast(SignUp.this, R.string.authentication_error_password_mismatch);
                } else if (!checkPassword(choosePasswordField.getText())) {
                    CloudUtils.showToast(SignUp.this, R.string.authentication_error_password_too_short);
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
                progress = CloudUtils.showProgress(SignUp.this,
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

    private void signupFail(String error) {
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


    static boolean checkPassword(CharSequence password) {
        return password != null && password.length() >= MIN_PASSWORD_LENGTH;
    }
}
