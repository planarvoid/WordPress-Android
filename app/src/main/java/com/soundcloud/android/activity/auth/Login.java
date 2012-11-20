package com.soundcloud.android.activity.auth;

import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.tracking.Click;
import com.soundcloud.android.tracking.Page;
import com.soundcloud.android.tracking.Tracking;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.ScTextUtils;

import android.content.Intent;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;


@Tracking(page = Page.Entry_login__main)
public class Login extends AbstractLoginActivity {

    @Override
    protected void onResume() {
        super.onResume();
        ((SoundCloudApplication)getApplication()).track(getClass());

        int backgroundId         = getIntent().getExtras().getInt(Start.TOUR_BACKGROUND_EXTRA, R.drawable.tour_image_1);
        Drawable drawable        = getResources().getDrawable(backgroundId);
        ImageView backgroundView = (ImageView) findViewById(R.id.tour_background_image);

        backgroundView.setImageDrawable(drawable);
    }

    protected void build() {
        setContentView(R.layout.login);

        final EditText emailField = (EditText) findViewById(R.id.txt_email_address);
        final EditText passwordField = (EditText) findViewById(R.id.txt_password);
        final View loginBtn = findViewById(R.id.btn_login);

        emailField.setText(AndroidUtils.suggestEmail(this));

        passwordField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @SuppressWarnings({"SimplifiableIfStatement"})
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE ||
                        (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER &&
                                event.getAction() == KeyEvent.ACTION_DOWN)) {
                    return loginBtn.performClick();
                } else {
                    return false;
                }
            }
        });

        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (emailField.getText().length() == 0 || passwordField.getText().length() == 0) {
                    AndroidUtils.showToast(Login.this, R.string.authentication_error_incomplete_fields);
                } else {
                    ((SoundCloudApplication)getApplication()).track(Click.Login_Login_done);

                    final String email = emailField.getText().toString();
                    final String password = passwordField.getText().toString();
                    login(email, password);
                }
            }
        });

        ScTextUtils.clickify(((TextView) findViewById(R.id.txt_msg)),
                getResources().getString(R.string.authentication_I_forgot_my_password),
                new ScTextUtils.ClickSpan.OnClickListener() {
                    @Override
                    public void onClick() {
                        Intent i = new Intent(Login.this, Recover.class);
                        if (emailField.getText().length() > 0) {
                            i.putExtra("email", emailField.getText().toString());
                        }
                        startActivityForResult(i, 0);
                    }
                }, true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            Start.handleRecoverResult(this, data);
        }
    }
}
