package com.soundcloud.android.activity.auth;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.User;
import com.soundcloud.android.task.GetTokensTask;
import com.soundcloud.android.task.SignupTask;
import com.soundcloud.android.utils.ClickSpan;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.api.Token;

import android.accounts.Account;
import android.accounts.AccountManager;
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

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class SignUp extends Activity {
    public static final Uri TERMS_OF_USE_URL = Uri.parse("http://m.soundcloud.com/terms-of-use");

    private static final int MIN_PASSWORD_LENGTH = 4;
    public static final Pattern EMAIL_ADDRESS_PATTERN = Pattern.compile(
            "[a-zA-Z0-9\\+\\._%\\-\\+]{1,256}" +
                    "@" +
                    "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                    "(" +
                    "\\." +
                    "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
                    ")+"
    );

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        build();
    }

    @Override
    protected void onResume() {
        super.onResume();
        final SoundCloudApplication app = (SoundCloudApplication) getApplication();
        app.trackPage(Consts.TrackingEvents.SIGNUP);
    }

    protected void build() {
        setContentView(R.layout.signup);

        final EditText emailField = (EditText) findViewById(R.id.txt_email_address);
        final EditText choosePasswordField = (EditText) findViewById(R.id.txt_choose_a_password);
        final EditText repeatPasswordField = (EditText) findViewById(R.id.txt_repeat_your_password);
        final Button signupBtn = (Button) findViewById(R.id.btn_signup);

        emailField.setText(suggestEmail());

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
                if (emailField.getText().length() == 0 ||
                        choosePasswordField.getText().length() == 0 ||
                        repeatPasswordField.getText().length() == 0) {
                    CloudUtils.showToast(SignUp.this, R.string.authentication_error_incomplete_fields);
                } else if (!checkEmail(emailField.getText())) {
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

        CloudUtils.clickify(((TextView) findViewById(R.id.txt_msg)),
                getResources().getString(R.string.authentication_terms_of_use),
                new ClickSpan.OnClickListener() {
                    @Override public void onClick() {
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
                progress = ProgressDialog.show(SignUp.this, "",
                        SignUp.this.getString(R.string.authentication_signup_progress_message));
            }

            @Override
            protected void onPostExecute(final User user) {
                if (!isFinishing()) {
                    try {
                        progress.dismiss();
                    } catch (IllegalArgumentException ignored) {}
                }

                if (user != null) {
                    // need to create user account as soon as possible, so the refresh logic in
                    // SoundCloudApplication works properly
                    final boolean signedUp = app.addUserAccount(user, app.getToken());

                    final Bundle param = new Bundle();
                    param.putString("username", email);
                    param.putString("password", password);
                    new GetTokensTask(mApi) {
                        @Override protected void onPostExecute(Token token) {
                            if (token != null) {
                                app.trackPage(Consts.TrackingEvents.LOGIN);
                                startActivityForResult(new Intent(SignUp.this, AddInfo.class)
                                        .putExtra("signed_up", signedUp ? "native" : null)
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


    private String suggestEmail() {
        Map<String,Integer> counts = new HashMap<String,Integer>();
        Account[] accounts = AccountManager.get(this).getAccounts();
        for (Account account : accounts) {
            if (checkEmail(account.name)) {
                if (counts.get(account.name) == null) {
                    counts.put(account.name, 1);
                } else {
                    counts.put(account.name, counts.get(account.name) + 1);
                }
            }
        }
        if (counts.isEmpty()) {
            return null;
        } else {
            int max = 0;
            String candidate = null;
            for (Map.Entry<String,Integer> e : counts.entrySet()) {
                if (e.getValue() > max) {
                    max = e.getValue();
                    candidate = e.getKey();
                }
            }
            return candidate;
        }
    }

    static boolean checkEmail(CharSequence email) {
        return EMAIL_ADDRESS_PATTERN.matcher(email).matches();
    }

    static boolean checkPassword(CharSequence password) {
        return password != null && password.length() >= MIN_PASSWORD_LENGTH;
    }
}
