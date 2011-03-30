package com.soundcloud.android.activity;

import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudDB;
import com.soundcloud.android.objects.Recording;
import com.soundcloud.android.objects.Recording.Recordings;
import com.soundcloud.android.view.AccessList;
import com.soundcloud.android.view.ConnectionList;
import com.soundcloud.utils.record.CloudRecorder.Profile;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class ScUpload extends ScActivity {
    private static final String TAG = "ScCreate";

    private ViewFlipper mSharingFlipper;

    private RadioGroup mRdoPrivacy;

    /* package */  RadioButton mRdoPrivate, mRdoPublic;
    /* package */  EditText mWhatText;
    /* package */  TextView mWhereText;

    private ImageView mArtwork;

    private File mImageDir,mArtworkFile;
    private Bitmap mArtworkBitmap;

    /* package */ ConnectionList mConnectionList;
    /* package */ AccessList mAccessList;

    private String mFourSquareVenueId;
    private double mLong, mLat;

    private Recording mRecording;

    public void setPrivateShareEmails(String[] emails) {
        mAccessList.getAdapter().setAccessList(Arrays.asList(emails));
    }

    public void setWhere(String where, String id, double lng, double lat) {
        if (where != null) mWhereText.setTextKeepState(where);
        mFourSquareVenueId = id;
        mLong = lng;
        mLat = lat;
    }

    private static final Pattern RAW_PATTERN = Pattern.compile("^.*\\.(2|pcm)$");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sc_upload);
        initResourceRefs();

        mImageDir = new File(CloudUtils.EXTERNAL_STORAGE_DIRECTORY + "/recordings/images");
        if (!mImageDir.exists()) mImageDir.mkdirs();

        File uploadFile = null;
        if (getIntent().hasExtra(Intent.EXTRA_STREAM)) {
            Uri stream = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
            if ("file".equals(stream.getScheme())) {
                uploadFile = new File(stream.getPath());
            }
        }

        Cursor cursor = null;
        if (uploadFile != null && uploadFile.exists()) {
            Recording r = new Recording();
            r.audio_path = uploadFile.getAbsolutePath();
            r.audio_profile = Profile.ENCODED_LOW;
            r.timestamp = uploadFile.lastModified();
            r.external_upload = true;
            r.user_id = CloudUtils.getCurrentUserId(ScUpload.this);
            Uri uri = SoundCloudDB.getInstance().insertRecording(getContentResolver(), r);
            cursor = getContentResolver().query(uri, null, null, null, null);

        } else if (getIntent().hasExtra("recordingId")
                && getIntent().getLongExtra("recordingId", 0) != 0) {
            cursor = getContentResolver().query(Recordings.CONTENT_URI, null,
                    Recordings.ID + "='" + getIntent().getLongExtra("recordingId", 0) + "'", null,
                    null);
        } else if (getIntent().hasExtra("recordingUri")) {
            cursor = getContentResolver().query(
                    ((Uri) getIntent().getParcelableExtra("recordingUri")), null, null, null, null);
        }

        if (cursor != null) {
            cursor.moveToFirst();
            mRecording = new Recording(cursor);
            uploadFile = new File(mRecording.audio_path);
            if (uploadFile != null && uploadFile.exists()) {
                mapFromRecording();
            } else {
                cursor.close();
                errorOut("Record file is missing");
            }
            cursor.close();
        } else {
            errorOut("Recording not found");
        }

    }

    private void errorOut(CharSequence error){
        showToast(error);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mConnectionList.getAdapter().loadIfNecessary();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mRecording != null) {
            // recording exists and hasn't been uploaded
            mapToRecording();
            SoundCloudDB.getInstance().updateRecording(ScUpload.this.getContentResolver(), mRecording);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        clearArtwork();
    }

    /*
    * Whenever the UI is re-created (due f.ex. to orientation change) we have
    * to reinitialize references to the views.
    */
    private void initResourceRefs() {

        findViewById(R.id.btn_cancel).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent i = (new Intent(ScUpload.this, Main.class))
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra("tabTag", "record");
                startActivity(i);
            }
        });

        findViewById(R.id.btn_upload).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mapToRecording();
                if (startUpload(mRecording)){
                    setResult(RESULT_OK);
                    finish();
                }
            }
        });

        mArtwork = (ImageView) findViewById(R.id.artwork);
        TextView mArtworkBg = (TextView) findViewById(R.id.txt_artwork_bg);

        mWhatText = (EditText) findViewById(R.id.what);
        mWhereText = (TextView) findViewById(R.id.where);

        mWhereText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ScUpload.this, LocationPicker.class);
                intent.putExtra("name", ((TextView)v).getText().toString());
                if (mRecording.longitude != 0)intent.putExtra("long", mRecording.longitude);
                if (mRecording.latitude != 0) intent.putExtra("lat", mRecording.latitude);
                startActivityForResult(intent, LocationPicker.PICK_VENUE);
            }
        });

        mWhatText.addTextChangedListener(new Capitalizer(mWhatText));
        mSharingFlipper = (ViewFlipper) findViewById(R.id.vfSharing);
        mRdoPrivacy = (RadioGroup) findViewById(R.id.rdo_privacy);
        mRdoPublic = (RadioButton) findViewById(R.id.rdo_public);
        mRdoPrivate = (RadioButton) findViewById(R.id.rdo_private);

        mRdoPrivacy.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){
            @Override public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.rdo_public:   mSharingFlipper.setDisplayedChild(0); break;
                    case R.id.rdo_private:  mSharingFlipper.setDisplayedChild(1); break;

                }
            }
        });

        mArtwork.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showToast(R.string.cloud_upload_clear_artwork);
            }
        });

        mArtworkBg.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                new AlertDialog.Builder(ScUpload.this)
                        .setMessage("Where would you like to get the image?").setPositiveButton(
                        "Take a new picture", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                Intent i = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                                i.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, Uri.fromFile(getCurrentImageFile()));
                                startActivityForResult(i, CloudUtils.RequestCodes.GALLERY_IMAGE_TAKE);
                            }
                        }).setNegativeButton("Use existing image", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("image/*");
                        startActivityForResult(intent, CloudUtils.RequestCodes.GALLERY_IMAGE_PICK);
                    }
                }).create().show();
            }
        });

        mArtwork.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                clearArtwork();
                return true;
            }
        });

        mConnectionList = (ConnectionList) findViewById(R.id.connectionList);
        mConnectionList.setAdapter(new ConnectionList.Adapter(this.getSoundCloudApplication()));

        mAccessList = (AccessList) findViewById(R.id.accessList);
        mAccessList.setAdapter(new AccessList.Adapter());
        mAccessList.getAdapter().setAccessList(null);

        mAccessList.getAdapter().registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                findViewById(R.id.btn_add_emails).setVisibility(
                    mAccessList.getAdapter().getCount() > 0 ? View.GONE : View.VISIBLE
                );
            }
        });

        findViewById(R.id.btn_add_emails).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<String> accessList = mAccessList.getAdapter().getAccessList();
                Intent intent = new Intent(ScUpload.this, EmailPicker.class);
                if (accessList != null) {
                    intent.putExtra(EmailPicker.BUNDLE_KEY, accessList.toArray(new String[accessList.size()]));
                    if (v instanceof TextView) {
                        intent.putExtra(EmailPicker.SELECTED, ((TextView) v).getText());
                    }
                }
                startActivityForResult(intent, EmailPicker.PICK_EMAILS);
            }
        });
    }

    @Override
    public void onReauthenticate() {
        onRefresh();
    }

    @Override
    public void onRefresh() {
        mConnectionList.getAdapter().clear();
        mConnectionList.getAdapter().loadIfNecessary();
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        state.putString("createWhatValue", mWhatText.getText().toString());
        state.putString("createWhereValue", mWhereText.getText().toString());
        state.putInt("createPrivacyValue", mRdoPrivacy.getCheckedRadioButtonId());

        if (mArtworkFile != null) {
            state.putString("createArtworkPath", mArtworkFile.getAbsolutePath());
        }
        super.onSaveInstanceState(state);
    }

    @Override
    public void onRestoreInstanceState(Bundle state) {

        mWhatText.setText(state.getString("createWhatValue"));
        mWhereText.setText(state.getString("createWhereValue"));

        if (state.getInt("createPrivacyValue") == R.id.rdo_private) {
            mRdoPrivate.setChecked(true);
        } else {
            mRdoPublic.setChecked(true);
        }

        if (!TextUtils.isEmpty(state.getString("createArtworkPath"))) setImage(state.getString("createArtworkPath"));

        super.onRestoreInstanceState(state);
    }

    // TODO move this code into a helper class
    public void setImage(String filePath) {
        setImage(new File(filePath));
    }

    public void setImage(File imageFile) {
        Log.i(TAG,"SET IMAGE FILE " + imageFile.getAbsolutePath());
        mArtworkFile = imageFile;

        try {
            final int density = (int) (getResources().getDisplayMetrics().density * 100);
            Options opt = CloudUtils.determineResizeOptions(mArtworkFile, density, density);

            ExifInterface exif = new ExifInterface(mArtworkFile.getAbsolutePath());
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);
            int degree = 0;
            if (orientation != -1) {
                // We only recognize a subset of orientation tag values.
                switch (orientation) {
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        degree = 90;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        degree = 180;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        degree = 270;
                        break;
                    default:
                        degree = 0;
                        break;
                }
            }
            if (mArtworkBitmap != null) CloudUtils.clearBitmap(mArtworkBitmap);

            Options sampleOpt = new BitmapFactory.Options();
            sampleOpt.inSampleSize = opt.inSampleSize;

            mArtworkBitmap = BitmapFactory.decodeFile(mArtworkFile.getAbsolutePath(), sampleOpt);

            Matrix m = new Matrix();
            float scale;
            float dx = 0, dy = 0;
            int vwidth = (int) (getResources().getDisplayMetrics().density * 100);

            if (mArtworkBitmap.getWidth() > mArtworkBitmap.getHeight()) {
                scale = (float) vwidth / (float) mArtworkBitmap.getHeight();
                dx = (vwidth - mArtworkBitmap.getWidth() * scale) * 0.5f;
            } else {
                scale = (float) vwidth / (float) mArtworkBitmap.getWidth();
                dy = (vwidth - mArtworkBitmap.getHeight() * scale) * 0.5f;
            }

            m.setScale(scale, scale);
            m.postTranslate((int) (dx + 0.5f), (int) (dy + 0.5f));
            //pivot point in the middle, may need to change this
            if (degree != 0) m.postRotate(90, vwidth / 2, vwidth / 2);

            mArtwork.setScaleType(ScaleType.MATRIX);
            mArtwork.setImageMatrix(m);

            mArtwork.setImageBitmap(mArtworkBitmap);
            mArtwork.setVisibility(View.VISIBLE);
        } catch (IOException e) {
            Log.e(TAG, "error", e);
        }
    }

    public void clearArtwork() {
        mArtworkFile = null;
        mArtwork.setVisibility(View.GONE);

        if (mArtworkBitmap != null)
            CloudUtils.clearBitmap(mArtworkBitmap);
    }

    private void mapFromRecording(){
        if (!TextUtils.isEmpty(mRecording.what_text)) mWhatText.setTextKeepState(mRecording.what_text);
        if (!TextUtils.isEmpty(mRecording.where_text)) mWhereText.setTextKeepState(mRecording.where_text);
        if (!TextUtils.isEmpty(mRecording.artwork_path)) setImage(mRecording.artwork_path);
        if (!TextUtils.isEmpty(mRecording.shared_emails)) setPrivateShareEmails(mRecording.shared_emails.split(","));

        setWhere(TextUtils.isEmpty(mRecording.where_text) ? "" : mRecording.where_text,
                TextUtils.isEmpty(mRecording.four_square_venue_id) ? ""
                        : mRecording.four_square_venue_id, mRecording.longitude,
                mRecording.latitude);

        if (mRecording.is_private) {
            mRdoPrivate.setChecked(true);
        } else {
            mRdoPublic.setChecked(true);
        }
    }

    private void mapToRecording(){
        mRecording.is_private = mRdoPrivacy.getCheckedRadioButtonId() == R.id.rdo_private;
        mRecording.what_text = mWhatText.getText().toString();
        mRecording.where_text = mWhereText.getText().toString();
        if (mArtworkFile != null) mRecording.artwork_path = mArtworkFile.getAbsolutePath();
        if (mFourSquareVenueId != null) mRecording.four_square_venue_id = mFourSquareVenueId;
        mRecording.latitude = mLat;
        mRecording.longitude = mLong;
        if (mRecording.is_private){
            if (mConnectionList.postToServiceIds() != null) mRecording.service_ids = CloudUtils.join(mConnectionList.postToServiceIds(), ",");
            mRecording.shared_emails = null;
        } else {
            mRecording.service_ids = null;
            if (mAccessList.getAdapter().getAccessList() != null) mRecording.shared_emails = CloudUtils.join(mAccessList.getAdapter().getAccessList(), ",");
        }

    }

    private File getCurrentImageFile(){
        if (mRecording == null)
            return null;
        else{
            File f = new File(mRecording.audio_path);
            return new File(mImageDir, f.getName().substring(0, f.getName().lastIndexOf(".")) + ".bmp");
        }
    }

    public static class Capitalizer implements TextWatcher {
        private TextView text;
        public Capitalizer(TextView text) {
            this.text = text;
        }

        public void afterTextChanged(Editable s) {
            if (s.length() == 1
            && !s.toString().toUpperCase().contentEquals(s.toString())) {
                text.setTextKeepState(s.toString().toUpperCase());
            }
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
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
                    setImage(getCurrentImageFile());
                }
                break;


            case EmailPicker.PICK_EMAILS:
                if (resultCode == RESULT_OK &&result != null && result.hasExtra(EmailPicker.BUNDLE_KEY)) {
                    String[] emails = result.getExtras().getStringArray(EmailPicker.BUNDLE_KEY);
                    if (emails != null) {
                        setPrivateShareEmails(emails);
                    }
                }
                break;
            case LocationPicker.PICK_VENUE:
                if (resultCode == RESULT_OK && result != null && result.hasExtra("name")) {
                    setWhere(result.getStringExtra("name"),
                            result.getStringExtra("id"),
                            result.getDoubleExtra("longitude", 0),
                            result.getDoubleExtra("latitude", 0));
                }
                break;
            case Connect.MAKE_CONNECTION:
                if (resultCode == RESULT_OK) {
                    boolean success = result.getBooleanExtra("success", false);
                    String msg = getString(
                            success ? R.string.connect_success : R.string.connect_failure,
                            result.getStringExtra("service"));
                    Toast toast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.BOTTOM, 0, 0);
                    toast.show();

                    if (success) mConnectionList.getAdapter().load();
                }
        }
    }

}
