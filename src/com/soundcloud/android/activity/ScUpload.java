package com.soundcloud.android.activity;

import com.soundcloud.android.CloudAPI;
import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.R;
import com.soundcloud.android.task.UploadTask;
import com.soundcloud.android.view.AccessList;
import com.soundcloud.android.view.ConnectionList;
import com.soundcloud.utils.CloudCache;
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
import android.os.RemoteException;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateFormat;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class ScUpload extends ScActivity {
    private static final String TAG = "ScCreate";

    private ViewFlipper mSharingFlipper;

    private RadioGroup mRdoPrivacy;

    /* package */  RadioButton mRdoPrivate, mRdoPublic;
    /* package */  EditText mWhatText;
    /* package */  TextView mWhereText;

    private ImageView mArtwork;

    private File mUploadFile;

    private String mArtworkUri;
    private Bitmap mArtworkBitmap;

    /* package */ ConnectionList mConnectionList;
    /* package */ AccessList mAccessList;

    private String mFourSquareVenueId;
    private double mLong, mLat;

    boolean mExternalUpload;
    private int mAudioProfile;

    public void setPrivateShareEmails(String[] emails) {
        mAccessList.getAdapter().setAccessList(Arrays.asList(emails));
    }

    public void setWhere(String where, String id, double lng, double lat) {
        if (where != null) mWhereText.setTextKeepState(where);
        mFourSquareVenueId = id;
        mLong = lng;
        mLat = lat;
    }

    private static String UPLOAD_TEMP_PICTURE_PATH = CloudCache.EXTERNAL_CACHE_DIRECTORY + "tmp.bmp";
    private static final Pattern RAW_PATTERN = Pattern.compile("^.*\\.(2|pcm)$");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sc_upload);
        initResourceRefs();

        File uploadFile = null;
        if (getIntent().hasExtra(Intent.EXTRA_STREAM)) {
            Uri stream = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
            if ("file".equals(stream.getScheme())) {
                uploadFile = new File(stream.getPath());
            }
        }

        if (uploadFile != null && uploadFile.exists()) {
            setUploadFile(uploadFile);
            mExternalUpload = true;
        } else if (getIntent().hasExtra("uploadFilePath")){
            uploadFile = new File(getIntent().getStringExtra("uploadFilePath"));
            if (uploadFile != null && uploadFile.exists()) {
                setUploadFile(uploadFile);
            } else {
                throw new IllegalArgumentException("no uploadFilePath path extra");
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mConnectionList.getAdapter().loadIfNecessary();
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
                setResult(RESULT_CANCELED);
                finish();
            }
        });

        findViewById(R.id.btn_upload).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startUpload();
                setResult(RESULT_OK);
                finish();
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
                                i.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, Uri.fromFile(new
                                        File(UPLOAD_TEMP_PICTURE_PATH)));
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

        if (!TextUtils.isEmpty(mArtworkUri)) {
            state.putString("createArtworkPath", mArtworkUri);
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

        if (!TextUtils.isEmpty(state.getString("createArtworkPath"))) {
            if (state.getString("createArtworkPath").contentEquals(UPLOAD_TEMP_PICTURE_PATH))
                setTakenImage(); //account for rotation
            else
                setPickedImage(state.getString("createArtworkPath"));
        }

        super.onRestoreInstanceState(state);
    }

    public void setPickedImage(String imageUri) {
        try {

            Options opt = CloudUtils.determineResizeOptions(new File(imageUri),
                    (int) getResources().getDisplayMetrics().density * 100,
                    (int) getResources().getDisplayMetrics().density * 100);

            mArtworkUri = imageUri;

            if (mArtworkBitmap != null)
                CloudUtils.clearBitmap(mArtworkBitmap);

            Matrix mat = new Matrix();
            mArtwork.setImageMatrix(mat);

            Options sampleOpt = new BitmapFactory.Options();
            sampleOpt.inSampleSize = opt.inSampleSize;

            try {

                mArtworkBitmap = BitmapFactory.decodeFile(mArtworkUri, sampleOpt);
                mArtwork.setImageBitmap(mArtworkBitmap);
                mArtwork.setVisibility(View.VISIBLE);
            } catch (Exception e){
                //temp
            }
        } catch (IOException e) {
            Log.e(TAG, "error", e);
        }
    }

    // TODO move this code into a helper class
    public void setTakenImage() {
        mArtworkUri = UPLOAD_TEMP_PICTURE_PATH;
        try {
            final int density = (int) (getResources().getDisplayMetrics().density * 100);
            Options opt = CloudUtils.determineResizeOptions(new File(UPLOAD_TEMP_PICTURE_PATH), density, density);

            ExifInterface exif = new ExifInterface(mArtworkUri);
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

            mArtworkBitmap = BitmapFactory.decodeFile(mArtworkUri, sampleOpt);

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
        mArtworkUri = null;
        mArtwork.setVisibility(View.GONE);

        if (mArtworkBitmap != null)
            CloudUtils.clearBitmap(mArtworkBitmap);
    }



    void startUpload() {
        if (mCreateService == null) return;

        boolean uploading;
        try {
            uploading = mCreateService.isUploading();
        } catch (RemoteException e) {
            Log.e(TAG, "error", e);
            uploading = true;
        }

        if (!uploading) {
            final boolean privateUpload = mRdoPrivacy.getCheckedRadioButtonId() == R.id.rdo_private;
            final Map<String, Object> data = new HashMap<String, Object>();
            data.put(CloudAPI.Params.SHARING, privateUpload ? CloudAPI.Params.PRIVATE : CloudAPI.Params.PUBLIC);
            data.put(CloudAPI.Params.DOWNLOADABLE, false);
            data.put(CloudAPI.Params.STREAMABLE, true);


            if (!privateUpload) {
                Log.v(TAG, "public track upload");

                final List<Integer> serviceIds = mConnectionList.postToServiceIds();

                 if (!serviceIds.isEmpty()) {
                    data.put(CloudAPI.Params.SHARING_NOTE, generateSharingNote());
                    data.put(CloudAPI.Params.POST_TO, serviceIds);
                 } else {
                    data.put(CloudAPI.Params.POST_TO_EMPTY, "");
                 }
            } else {
                Log.v(TAG, "private track upload");

                final List<String> sharedEmails = mAccessList.getAdapter().getAccessList();
                if (sharedEmails != null && !sharedEmails.isEmpty()) {
                    data.put(CloudAPI.Params.SHARED_EMAILS, sharedEmails);
                }
            }

            data.put(UploadTask.Params.SOURCE_PATH, mUploadFile.getAbsolutePath());

            final String title = generateTitle();
            data.put(CloudAPI.Params.TITLE, title);
            data.put(CloudAPI.Params.TYPE, "recording");

            // add machine tags
            List<String> tags = new ArrayList<String>();

            if (mExternalUpload) {
                tags.add("soundcloud:source=android-3rdparty-upload");
            } else {
                tags.add("soundcloud:source=android-record");
            }

            if (mFourSquareVenueId != null) tags.add("foursquare:venue="+mFourSquareVenueId);
            if (mLat  != 0) tags.add("geo:lat="+mLat);
            if (mLong != 0) tags.add("geo:lon="+mLong);
            data.put(CloudAPI.Params.TAG_LIST, TextUtils.join(" ", tags));

            if (mAudioProfile == Profile.RAW && !mExternalUpload) {
                data.put(UploadTask.Params.OGG_FILENAME,new File(mUploadFile.getParentFile(), generateFilename(title,"ogg")).getAbsolutePath());
                data.put(UploadTask.Params.ENCODE, true);
            } else {
                if (!mExternalUpload){
                    File newRecFile = new File(mUploadFile.getParentFile(), generateFilename(title, "mp4"));
                    if (mUploadFile == null || mUploadFile.renameTo(newRecFile)) {
                        mUploadFile = newRecFile;
                    }
                }
            }

            if (!TextUtils.isEmpty(mArtworkUri)) {
                data.put(UploadTask.Params.ARTWORK_PATH, mArtworkUri);
            }

            try {
                mCreateService.uploadTrack(data);
            } catch (RemoteException ignored) {
                Log.e(TAG, "error", ignored);
            } finally {
                mUploadFile = null;
            }
        } else {
            showToast(R.string.wait_for_upload_to_finish);
        }
    }

    private static String dateString(long modified) {
        final Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(modified);

        String day = cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.ENGLISH);
        String dayTime;

        if (cal.get(Calendar.HOUR_OF_DAY) <= 12) {
            dayTime = "morning";
        } else if (cal.get(Calendar.HOUR_OF_DAY) <= 17) {
            dayTime = "afternoon";
        } else if (cal.get(Calendar.HOUR_OF_DAY) <= 21) {
           dayTime = "evening";
        } else {
           dayTime = "night";
        }
        return day + " " + dayTime;
    }



    private String generateFilename(String title, String extension) {
        return String.format("%s_%s.%s", title,
               DateFormat.format("yyyy-MM-dd-hh-mm-ss", mUploadFile.lastModified()), extension);
    }

    private String generateTitle() {
      return generateSharingNote();
    }

    private String generateSharingNote() {
        String note;
        if (mWhatText.length() > 0) {
            if (mWhereText.length() > 0) {
                note = String.format("%s at %s", mWhatText.getText(), mWhereText.getText());
            } else {
                note = mWhatText.getText().toString();
            }
        } else {
            if (mWhereText.length() > 0) {
                note = String.format("Sounds from %s", mWhereText.getText());
            } else {
                note = String.format("Sounds from %s", dateString(mUploadFile.lastModified()));
            }
        }
        return note;
    }


    /* package */ void setUploadFile(File f) {
        mUploadFile = f;
        if (f != null) mAudioProfile = isRawFilename(f.getName()) ? Profile.RAW : Profile.ENCODED_LOW;
    }

    private boolean isRawFilename(String filename){
        return RAW_PATTERN.matcher(filename).matches();
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
                    setPickedImage(filePath);
                }
                break;
            case CloudUtils.RequestCodes.GALLERY_IMAGE_TAKE:
                if (resultCode == RESULT_OK) {
                    setTakenImage();
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
