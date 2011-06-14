package com.soundcloud.android.activity.auth;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.R;
import com.soundcloud.android.objects.User;
import com.soundcloud.android.task.AddUserInfoTask;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.utils.ImageUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;

public class AddInfo extends Activity {
    private File mAvatarFile;
    private Bitmap mAvatarBitmap;
    private ImageView mArtworkImg;

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

        mArtworkImg = (ImageView) findViewById(R.id.artwork);
        final TextView artworkField = (TextView) findViewById(R.id.txt_artwork_bg);

        usernameField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_NEXT ||
                        (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER &&
                                event.getAction() == KeyEvent.ACTION_DOWN)) {
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

        mArtworkImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CloudUtils.showToast(AddInfo.this, R.string.cloud_upload_clear_artwork);
            }
        });

        artworkField.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(AddInfo.this)
                .setMessage("Where would you like to get the image?").setPositiveButton(
                        "Take a new picture", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int whichButton) {
                                try {
                                    mAvatarFile = createTempAvatarFile();
                                    Intent i = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                                    i.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, Uri.fromFile(mAvatarFile));
                                    startActivityForResult(i, CloudUtils.RequestCodes.GALLERY_IMAGE_TAKE);
                                } catch (IOException e) {
                                    Log.w(TAG, "error", e);
                                }
                            }
                        }).setNegativeButton("Use existing image", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int whichButton) {
                                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                                intent.setType("image/*");
                                startActivityForResult(intent, CloudUtils.RequestCodes.GALLERY_IMAGE_PICK);
                            }
                        }).create().show();
            }
        });

        mArtworkImg.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                clearArtwork();
                return true;
            }
        });
    }

    @SuppressWarnings({"unchecked"})
    void addUserInfo(User user, String newUsername, File avatarFile) {
        if (!TextUtils.isEmpty(newUsername)) {
            user.username = newUsername;
            user.permalink = newUsername;
        }
        new AddUserInfoTask((AndroidCloudAPI) getApplication()) {
            ProgressDialog dialog;
            @Override
            protected void onPreExecute() {
                dialog = ProgressDialog.show(AddInfo.this, null, getString(R.string.authentication_add_info_progress_message));
            }
            @Override
            protected void onPostExecute(User user) {
                dialog.dismiss();
                if (user != null) {
                    finishSignup();
                } else {
                    CloudUtils.showToast(AddInfo.this, getFirstError() == null ?
                            getString(R.string.authentication_add_info_error) :
                            getString(R.string.authentication_add_info_error_reason, getFirstError()));
                }
            }
        }.execute(Pair.create(user, avatarFile));
    }

    private void finishSignup() {
        setResult(RESULT_OK, getIntent());
        finish();
    }

    private void setImage(String filePath) {
        setImage(new File(filePath));
    }

    private void setImage(File imageFile) {
        mAvatarFile = imageFile;

        try {
            // XXX move to ImageUtils
            final int density = (int) (getResources().getDisplayMetrics().density * 100);
            Options opt = ImageUtils.determineResizeOptions(mAvatarFile, density, density);

            if (mAvatarBitmap != null) {
                ImageUtils.clearBitmap(mAvatarBitmap);
            }

            Options sampleOpt = new BitmapFactory.Options();
            sampleOpt.inSampleSize = opt.inSampleSize;

            mAvatarBitmap = BitmapFactory.decodeFile(mAvatarFile.getAbsolutePath(), sampleOpt);

            Matrix m = new Matrix();
            float scale;
            float dx = 0, dy = 0;

            // assumes height and width are the same
            int viewDimension = (int) (getResources().getDisplayMetrics().density * 100);

            if (mAvatarBitmap.getWidth() > mAvatarBitmap.getHeight()) {
                scale = viewDimension / (float) mAvatarBitmap.getHeight();
                dx = (viewDimension - mAvatarBitmap.getWidth() * scale) * 0.5f;
            } else {
                scale = viewDimension / (float) mAvatarBitmap.getWidth();
                dy = (viewDimension - mAvatarBitmap.getHeight() * scale) * 0.5f;
            }

            m.setScale(scale, scale);
            m.postTranslate((int) (dx + 0.5f), (int) (dy + 0.5f));
            if (ImageUtils.getExifRotation(mAvatarFile.getAbsolutePath()) != 0) {
                m.postRotate(90, viewDimension / 2, viewDimension / 2);
            }

            mArtworkImg.setScaleType(ScaleType.MATRIX);
            mArtworkImg.setImageMatrix(m);

            mArtworkImg.setImageBitmap(mAvatarBitmap);
            mArtworkImg.setVisibility(View.VISIBLE);

        } catch (IOException e) {
            Log.e(TAG, "error", e);
        }
    }

    public void clearArtwork() {
        mAvatarFile = null;
        mArtworkImg.setVisibility(View.GONE);

        if (mAvatarBitmap != null) {
            ImageUtils.clearBitmap(mAvatarBitmap);
        }
    }

    private File createTempAvatarFile() throws IOException {
        return File.createTempFile(Long.toString(System.currentTimeMillis()), ".bmp", CloudUtils.getCacheDir(this));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        clearArtwork();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent result) {
        switch (requestCode) {
            case CloudUtils.RequestCodes.GALLERY_IMAGE_PICK:
                if (resultCode == RESULT_OK) {
                    setImage(ImageUtils.getFromMediaUri(getContentResolver(), result.getData()));
                }
                break;
            case CloudUtils.RequestCodes.GALLERY_IMAGE_TAKE:
                if (resultCode == RESULT_OK) {
                    setImage(mAvatarFile);
                }
                break;
        }
    }

}
