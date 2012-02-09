package com.soundcloud.android.activity.auth;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.User;
import com.soundcloud.android.task.AddUserInfoTask;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.ImageUtils;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;

public class AddInfo extends Activity {
    private File mAvatarFile;
    private ImageView mArtwork;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        build();
    }

    protected void build() {
        setContentView(R.layout.add_info);
        final User user = getIntent().getParcelableExtra("user");

        final EditText usernameField = (EditText) findViewById(R.id.txt_username);
        usernameField.setHint(R.string.authentication_add_info_username_hint);

        mArtwork = (ImageView) findViewById(R.id.artwork);
        final TextView artworkField = (TextView) findViewById(R.id.txt_artwork_bg);

        usernameField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                //noinspection SimplifiableIfStatement
                if (actionId == EditorInfo.IME_ACTION_NEXT ||
                (event != null &&
                 event.getKeyCode() == KeyEvent.KEYCODE_ENTER &&
                 event.getAction()  == KeyEvent.ACTION_DOWN)) {
                    return mAvatarFile == null && artworkField.performClick();
                } else {
                    return false;
                }
            }
        });

        findViewById(R.id.btn_skip).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishSignup();
            }
        });

        findViewById(R.id.btn_save).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                addUserInfo(user, usernameField.getText().toString(), mAvatarFile);
            }
        });

        mArtwork.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CloudUtils.showToast(AddInfo.this, R.string.cloud_upload_clear_artwork);
            }
        });

        artworkField.setOnClickListener(
            new ImageUtils.ImagePickListener(this) {
                @Override protected File getFile() {
                    mAvatarFile = createTempAvatarFile();
                    return mAvatarFile;
                }
            }
        );

        mArtwork.setOnLongClickListener(new View.OnLongClickListener() {
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
                dialog = CloudUtils.showProgress(AddInfo.this,
                        R.string.authentication_add_info_progress_message);
            }

            @Override protected void onPostExecute(User user) {
                if (dialog != null) dialog.dismiss();
                if (user != null) {
                    finishSignup();
                } else {
                    CloudUtils.showToast(AddInfo.this, getFirstError() == null ?
                            getString(R.string.authentication_add_info_error) :
                            getString(R.string.authentication_add_info_error_reason, getFirstError()));
                }
            }
        }.execute(Pair.create(user, avatarFile));
        return user;
    }

    private void finishSignup() {
        setResult(RESULT_OK, getIntent());
        finish();
    }

     @Override
    protected void onResume() {
        super.onResume();
        ((SoundCloudApplication) getApplication()).trackPage(Consts.Tracking.SIGNUP_DETAILS);
    }

    private void setImage(final File file) {
        if (file != null) {
            mAvatarFile = file;
            ImageUtils.setImage(file, mArtwork, (int) (getResources().getDisplayMetrics().density * 100f),
                (int) (getResources().getDisplayMetrics().density * 100f));
        }
    }

    public void clearArtwork() {
        mAvatarFile = null;
        mArtwork.setVisibility(View.GONE);
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
    public void onDestroy() {
        super.onDestroy();
        clearArtwork();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent result) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case ImageUtils.ImagePickListener.GALLERY_IMAGE_PICK:
                    setImage(IOUtils.getFromMediaUri(getContentResolver(), result.getData()));
                    break;
                case ImageUtils.ImagePickListener.GALLERY_IMAGE_TAKE:
                    setImage(mAvatarFile);
                    break;
            }
        }
    }
}
