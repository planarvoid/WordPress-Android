package com.soundcloud.android.activity;

import com.soundcloud.android.R;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.utils.ImageUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;

public class AuthAddInfo extends Activity {

    private File mAvatarFile;
    private Bitmap mAvatarBitmap;
    private ImageView mArtworkImg;


    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        build();
    }

    protected void build() {
        setContentView(R.layout.auth_add_info);

        final EditText usernameField = (EditText) findViewById(R.id.txt_username);
        final Button skipBtn = (Button) findViewById(R.id.btn_skip);
        final Button saveBtn = (Button) findViewById(R.id.btn_save);

        mArtworkImg = (ImageView) findViewById(R.id.artwork);
        final TextView artworkField = (TextView) findViewById(R.id.txt_artwork_bg);

        usernameField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_NEXT ||
                        (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER &&
                                event.getAction() == KeyEvent.ACTION_DOWN)) {
                    if (mAvatarFile == null)
                        return artworkField.performClick();
                    else
                        return false;
                } else {
                    return false;
                }
            }
        });

        skipBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(getClass().getSimpleName(),"skip");
            }
        });

        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(getClass().getSimpleName(),"Save username " + usernameField.getText().toString());
            }
        });

        mArtworkImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CloudUtils.showToast(AuthAddInfo.this, R.string.cloud_upload_clear_artwork);
            }
        });

        artworkField.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(AuthAddInfo.this)
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
                                    e.printStackTrace();
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

    public void setImage(String filePath) {
        setImage(new File(filePath));
    }

    public void setImage(File imageFile) {
        mAvatarFile = imageFile;

        try {
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
            Log.e(getClass().getSimpleName(), "error", e);
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
    protected void onActivityResult(int requestCode, int resultCode, Intent result) {
        switch (requestCode) {
            case CloudUtils.RequestCodes.GALLERY_IMAGE_PICK:
                if (resultCode == RESULT_OK) {
                    Uri selectedImage = result.getData();
                    String[] filePathColumn = { MediaStore.MediaColumns.DATA };
                    Cursor cursor = getContentResolver().query(selectedImage,
                            filePathColumn, null, null, null);
                    cursor.moveToFirst();
                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    String filePath = cursor.getString(columnIndex);
                    cursor.close();
                    setImage(filePath);
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
