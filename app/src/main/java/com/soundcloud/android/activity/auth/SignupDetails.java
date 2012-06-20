package com.soundcloud.android.activity.auth;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.User;
import com.soundcloud.android.task.AddUserInfoTask;
import com.soundcloud.android.tracking.Click;
import com.soundcloud.android.tracking.Page;
import com.soundcloud.android.tracking.Tracking;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.ImageUtils;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;

@Tracking(page = Page.Entry_signup__details)
public class SignupDetails extends Activity {
    private File mAvatarFile;
    private ImageView mAvatarView;
    private TextView mAvatarText;

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

    @Override
    public void onDestroy() {
        super.onDestroy();
        clearArtwork();
    }

    private void build() {
        setContentView(R.layout.signup_details);
        final User user = getIntent().getParcelableExtra("user");

        final SoundCloudApplication app = (SoundCloudApplication) getApplication();
        final TextView username = (TextView) findViewById(R.id.txt_username);
        username.setHint(R.string.authentication_add_info_username_hint);

        mAvatarView = (ImageView) findViewById(R.id.artwork);
        mAvatarText = (TextView) findViewById(R.id.txt_artwork_bg);

        username.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                //noinspection SimplifiableIfStatement
                if (actionId == EditorInfo.IME_ACTION_NEXT ||
                        (event != null &&
                                event.getKeyCode() == KeyEvent.KEYCODE_ENTER &&
                                event.getAction() == KeyEvent.ACTION_DOWN)) {
                    return mAvatarFile == null && mAvatarText.performClick();
                } else {
                    return false;
                }
            }
        });

        findViewById(R.id.btn_skip).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                app.track(Click.Signup_Signup_details_skip);
                finishSignup();
            }
        });

        findViewById(R.id.btn_save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                app.track(Click.Signup_Signup_details_next);
                addUserInfo(user, username.getText().toString(), mAvatarFile);
            }
        });

        mAvatarView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AndroidUtils.showToast(SignupDetails.this, R.string.cloud_upload_clear_artwork);
            }
        });

        mAvatarText.setOnClickListener(
                new ImageUtils.ImagePickListener(this) {
                    @Override
                    protected File getFile() {
                        mAvatarFile = createTempAvatarFile();
                        return mAvatarFile;
                    }
                }
        );

        mAvatarView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                clearArtwork();
                return true;
            }
        });
    }

    @SuppressWarnings({"unchecked"})
    User addUserInfo(User user, String newUsername, File avatarFile) {
        if (!TextUtils.isEmpty(newUsername)) {
            user.username = newUsername;
            user.permalink = newUsername;
        }
        new AddUserInfoTask((AndroidCloudAPI) getApplication()) {
            ProgressDialog dialog;
            @Override protected void onPreExecute() {
                dialog = AndroidUtils.showProgress(SignupDetails.this,
                        R.string.authentication_add_info_progress_message);
            }

            @Override protected void onPostExecute(User user) {
                if (!isFinishing()) {
                    try {
                        if (dialog != null) dialog.dismiss();
                    } catch (IllegalArgumentException ignored) {
                    }

                    if (user != null) {
                        finishSignup();
                    } else {
                        AndroidUtils.showToast(SignupDetails.this, getFirstError() == null ?
                                getString(R.string.authentication_add_info_error) :
                                getString(R.string.authentication_add_info_error_reason, getFirstError()));
                    }
                }
            }
        }.execute(Pair.create(user, avatarFile));
        return user;
    }

    private void finishSignup() {
        setResult(RESULT_OK, getIntent());
        finish();
    }

    private void setImage(final File file) {
        if (file != null) {
            if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "setImage("+file+")");

            mAvatarFile = file;
            ImageUtils.setImage(file, mAvatarView,
                (int) (getResources().getDisplayMetrics().density * 100f),
                (int) (getResources().getDisplayMetrics().density * 100f));

            mAvatarText.setVisibility(View.GONE);
        }
    }

    private void clearArtwork() {
        mAvatarFile = null;
        mAvatarView.setVisibility(View.GONE);
        mAvatarText.setVisibility(View.VISIBLE);
    }

    private File createTempAvatarFile()  {
        try {
            return File.createTempFile(Long.toString(System.currentTimeMillis()), ".bmp", IOUtils.getCacheDir(this));
        } catch (IOException e) {
            Log.w(TAG, "error creating avatar temp file", e);
            return null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent result) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case Consts.RequestCodes.GALLERY_IMAGE_PICK:
                    mAvatarFile = createTempAvatarFile();
                    ImageUtils.sendCropIntent(this, result.getData(), Uri.fromFile(mAvatarFile));
                    break;
                case Consts.RequestCodes.GALLERY_IMAGE_TAKE:
                    ImageUtils.sendCropIntent(this, Uri.fromFile(mAvatarFile));
                    break;
                case Consts.RequestCodes.IMAGE_CROP: {
                    if (resultCode == RESULT_OK) {
                        setImage(mAvatarFile);
                    }
                    break;
                }
            }
        }
    }

}
