package com.soundcloud.android.activity;


import com.soundcloud.android.R;
import com.soundcloud.android.objects.FoursquareVenue;
import com.soundcloud.android.objects.Recording;
import com.soundcloud.android.provider.DatabaseHelper.Content;
import com.soundcloud.android.task.FoursquareVenueTask;
import com.soundcloud.android.utils.Capitalizer;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.utils.ImageUtils;
import com.soundcloud.android.view.AccessList;
import com.soundcloud.android.view.ConnectionList;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ScUpload extends ScActivity {
    private ViewFlipper mSharingFlipper;
    private RadioGroup mRdoPrivacy;
    /* package */ RadioButton mRdoPrivate, mRdoPublic;
    /* package */ EditText mWhatText;
    /* package */ TextView mWhereText;

    private ImageView mArtwork;
    private File mImageDir, mArtworkFile;
    /* package */ ConnectionList mConnectionList;
    /* package */ AccessList mAccessList;
    private String mFourSquareVenueId;
    private double mLong, mLat;
    private Recording mRecording;


    // used for preloading foursquare venues
    private ArrayList<FoursquareVenue> mVenues = new ArrayList<FoursquareVenue>();
    private Location mLocation;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.sc_upload);
        initResourceRefs();

        mImageDir = new File(CloudUtils.EXTERNAL_STORAGE_DIRECTORY + "/recordings/images");
        CloudUtils.mkdirs(mImageDir);

        Cursor cursor = null;
        Uri uri = null;
        File uploadFile = fileFromIntent(getIntent());
        if (uploadFile != null && uploadFile.exists()) {
            // 3rd party upload, disable "record another sound button"
            findViewById(R.id.btn_cancel).setVisibility(View.GONE);
            Recording r = new Recording(uploadFile);
            r.external_upload = true;
            r.user_id = getUserId();
            r.timestamp =  System.currentTimeMillis(); // XXX also set in ctor
            uri = getContentResolver().insert(Content.RECORDINGS, r.buildContentValues());
        } else if (getIntent() != null) {
            uri = getIntent().getData();
        }

        if (uri != null) {
            cursor = getContentResolver().query(uri, null, null, null, null);
        }

        if (cursor != null && cursor.moveToFirst()) {
            mRecording = new Recording(cursor);
            if (mRecording.exists()) {
                mapFromRecording();
            } else {
                errorOut("Record file is missing");
            }
            cursor.close();
        } else {
            errorOut("Recording not found");
        }

        if (mLocation == null) preloadLocations();
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
            getContentResolver().update(mRecording.toUri(), mRecording.buildContentValues(), null, null);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        clearArtwork();
    }

    private void setPrivateShareEmails(String[] emails) {
        mAccessList.getAdapter().setAccessList(Arrays.asList(emails));
    }

    /* package */ void setWhere(String where, String id, double lng, double lat) {
        if (where != null) {
            mWhereText.setTextKeepState(where);
        }
        mFourSquareVenueId = id;
        mLong = lng;
        mLat = lat;
    }


    private void initResourceRefs() {

        findViewById(R.id.btn_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = (new Intent(ScUpload.this, Main.class))
                        .addCategory(Intent.CATEGORY_LAUNCHER)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        .putExtra("tabTag", "record");
                startActivity(i);
            }
        });

        findViewById(R.id.btn_upload).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mapToRecording();
                if (startUpload()) {
                    setResult(RESULT_OK);
                    finish();
                }
            }
        });

        mArtwork = (ImageView) findViewById(R.id.artwork);
        mWhatText = (EditText) findViewById(R.id.what);
        mWhereText = (TextView) findViewById(R.id.where);

        mWhereText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ScUpload.this, LocationPicker.class);
                intent.putExtra("name", ((TextView) v).getText().toString());
                if (mRecording.longitude != 0) {
                    intent.putExtra("long", mRecording.longitude);
                }
                if (mRecording.latitude != 0) {
                    intent.putExtra("lat", mRecording.latitude);
                }
                synchronized (ScUpload.this) {
                  intent.putParcelableArrayListExtra("venues", mVenues);
                  intent.putExtra("location", mLocation);
                }
                startActivityForResult(intent, LocationPicker.PICK_VENUE);
            }
        });

        mWhatText.addTextChangedListener(new Capitalizer(mWhatText));
        mSharingFlipper = (ViewFlipper) findViewById(R.id.vfSharing);
        mRdoPrivacy = (RadioGroup) findViewById(R.id.rdo_privacy);
        mRdoPublic = (RadioButton) findViewById(R.id.rdo_public);
        mRdoPrivate = (RadioButton) findViewById(R.id.rdo_private);

        mRdoPrivacy.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.rdo_public:
                        mSharingFlipper.setDisplayedChild(0);
                        break;
                    case R.id.rdo_private:
                        mSharingFlipper.setDisplayedChild(1);
                        break;
                }
            }
        });

        mArtwork.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showToast(R.string.cloud_upload_clear_artwork);
            }
        });

        findViewById(R.id.txt_artwork_bg).setOnClickListener(
            new ImageUtils.ImagePickListener(this) {
                @Override protected File getFile() {
                    return getCurrentImageFile();
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

        mConnectionList = (ConnectionList) findViewById(R.id.connectionList);
        mConnectionList.setAdapter(new ConnectionList.Adapter(this.getApp()));

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

    /* package */ boolean startUpload() {
        return mRecording != null && startUpload(mRecording);
    }

    private void errorOut(CharSequence error) {
        showToast(error);
        finish();
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

        state.putParcelableArrayList("venues", mVenues);
        state.putParcelable("location", mLocation);

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

        mVenues = state.getParcelableArrayList("venues");
        mLocation = state.getParcelable("location");

        if (!TextUtils.isEmpty(state.getString("createArtworkPath"))) {
            setImage(new File(state.getString("createArtworkPath")));
        }
        super.onRestoreInstanceState(state);
    }

    public void setImage(File file) {
        mArtworkFile = file;
        ImageUtils.setImage(file, mArtwork, getResources().getDisplayMetrics());
    }

    // for testing purposes
    void setRecording(Recording r) {
        mRecording = r;
        mapToRecording();
    }

    private void clearArtwork() {
        mArtworkFile = null;
        mArtwork.setVisibility(View.GONE);
        if (mArtwork.getDrawable() instanceof BitmapDrawable) {
            ImageUtils.clearBitmap(((BitmapDrawable) mArtwork.getDrawable()).getBitmap());
        }
    }

    // XXX pass Recording in
    private void mapFromRecording() {
        if (!TextUtils.isEmpty(mRecording.what_text)) mWhatText.setTextKeepState(mRecording.what_text);
        if (!TextUtils.isEmpty(mRecording.where_text)) mWhereText.setTextKeepState(mRecording.where_text);
        if (mRecording.artwork_path != null) setImage(mRecording.artwork_path);
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

    private void mapToRecording() {
        mRecording.is_private = mRdoPrivacy.getCheckedRadioButtonId() == R.id.rdo_private;
        mRecording.what_text = mWhatText.getText().toString();
        mRecording.where_text = mWhereText.getText().toString();
        mRecording.artwork_path = mArtworkFile;

        if (mFourSquareVenueId != null) {
            mRecording.four_square_venue_id = mFourSquareVenueId;
        }
        mRecording.latitude = mLat;
        mRecording.longitude = mLong;
        if (!mRecording.is_private) {
            if (mConnectionList.postToServiceIds() != null) {
                mRecording.service_ids = TextUtils.join(",", mConnectionList.postToServiceIds());
            }
            mRecording.shared_emails = null;
        } else {
            mRecording.service_ids = null;
            if (mAccessList.getAdapter().getAccessList() != null) {
                mRecording.shared_emails = TextUtils.join(",", mAccessList.getAdapter().getAccessList());
            }
        }
    }

    private File getCurrentImageFile() {
        return (mRecording == null) ? null : mRecording.generateImageFile(mImageDir);
    }

    private void preloadLocations() {
        LocationManager mgr = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        final Location location = mgr.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
        if (location != null) {
            new FoursquareVenueTask() {
                @Override
                protected void onPostExecute(List<FoursquareVenue> venues) {
                    if (venues != null && !venues.isEmpty()) {
                        synchronized (ScUpload.this) {
                          mLocation = location;
                          mVenues.addAll(venues);
                        }
                    }
                }
            }.execute(location);
        }
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
                    setImage(getCurrentImageFile());
                }
                break;

            case EmailPicker.PICK_EMAILS:
                if (resultCode == RESULT_OK && result != null && result.hasExtra(EmailPicker.BUNDLE_KEY)) {
                    String[] emails = result.getExtras().getStringArray(EmailPicker.BUNDLE_KEY);
                    if (emails != null) {
                        setPrivateShareEmails(emails);
                    }
                }
                break;
            case LocationPicker.PICK_VENUE:
                if (resultCode == RESULT_OK && result != null && result.hasExtra("name")) {
                    // XXX candidate for model?
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

                    if (success) {
                        mConnectionList.getAdapter().load();
                    }
                }
        }
    }

    private File fileFromIntent(Intent intent) {
        if (intent != null && intent.hasExtra(Intent.EXTRA_STREAM)) {
            Uri stream = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if ("file".equals(stream.getScheme())) {
                return new File(stream.getPath());
            }
        }
        return null;
    }
}
