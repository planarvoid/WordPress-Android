package com.soundcloud.android.activity.auth;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.objects.User;
import com.soundcloud.android.task.AsyncApiTask;
import com.soundcloud.android.task.GetTokensTask;
import com.soundcloud.android.utils.ClickSpan;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.api.CloudAPI;
import com.soundcloud.api.Http;
import com.soundcloud.api.Token;

import org.apache.http.HttpResponse;

import android.app.Activity;
import android.app.ProgressDialog;
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

import java.io.IOException;
import java.util.regex.Pattern;

public class SignUp extends Activity {
    public static final Uri TERMS_OF_USE = Uri.parse("http://m.soundcloud.com/terms-of-use");

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

    protected void build() {
        setContentView(R.layout.auth_signup);

        final EditText emailField = (EditText) findViewById(R.id.txt_email_address);
        final EditText choosePasswordField = (EditText) findViewById(R.id.txt_choose_a_password);
        final EditText repeatPasswordField = (EditText) findViewById(R.id.txt_repeat_your_password);
        final Button signupBtn = (Button) findViewById(R.id.btn_signup);

        repeatPasswordField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
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

        findViewById(R.id.btn_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
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
                        startActivity(new Intent(Intent.ACTION_VIEW, TERMS_OF_USE));
                    }
                });
    }

    private void signup(final String email, final String password) {
        new SignupTask((SoundCloudApplication) getApplication()) {
            ProgressDialog progress;
            @Override
            protected void onPreExecute() {
                progress = ProgressDialog.show(SignUp.this, "",
                        SignUp.this.getString(R.string.authentication_signup_progress_message));
            }

            @Override
            protected void onPostExecute(final User user) {
                progress.dismiss();
                if (user != null) {
                    Log.d(TAG, "created user " + user);
                    new GetTokensTask(api()) {
                        @Override protected void onPostExecute(Token token) {
                            if (token != null) {
                                startActivityForResult(new Intent(SignUp.this, AddInfo.class)
                                        .putExtra("user", user)
                                        .putExtra("token", token), 0);
                            } else {
                                signupFail();
                            }
                        }
                    }.execute(email, password);
                } else {
                    signupFail();
                }
            }
        }.execute(email, password);
    }

    private void signupFail() {
        CloudUtils.showToast(SignUp.this, "Errorz");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        setResult(resultCode, data);
        finish();
    }

    static class SignupTask extends AsyncApiTask<String, Void, User> implements CloudAPI.UserParams {
        public SignupTask(AndroidCloudAPI api) {
            super(api);
        }

        @Override
        protected User doInBackground(String... params) {
            final String email = params[0];
            final String password = params[1];

            try {
                final Token signup = api().signupToken();
                HttpResponse resp = api().postContent(USERS, new Http.Params(
                        EMAIL, email,
                        PASSWORD, password,
                        PASSWORD_CONFIRMATION, password,
                        TERMS_OF_USE, "1"
                ).withToken(signup));

                final int code = resp.getStatusLine().getStatusCode();
                if (code == SC_CREATED) {
                    return api().getMapper().readValue(resp.getEntity().getContent(), User.class);
                } else {
                    warn("invalid response", resp);
                    return null;
                }
            } catch (IOException e) {
                warn("error creating user", e);
                return null;
            }
        }
    }

    static boolean checkEmail(CharSequence email) {
        return EMAIL_ADDRESS_PATTERN.matcher(email).matches();
    }

    static boolean checkPassword(CharSequence password) {
        return password != null && password.length() >= MIN_PASSWORD_LENGTH;
    }
}
