package com.soundcloud.android.view;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.LocationPicker;
import com.soundcloud.android.activity.ScUpload;
import com.soundcloud.android.model.FoursquareVenue;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.task.FoursquareVenueTask;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.utils.ImageUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class RecordingMetaData extends RelativeLayout{

    private Activity mActivity;
    private Recording mRecording;
    private File mImageDir, mArtworkFile;

    /* package */ EditText mWhatText;
    /* package */ TextView mWhereText;
    private ImageView mArtwork;

    private String mFourSquareVenueId;
    private double mLong, mLat;

    // used for preloading foursquare venues
    private ArrayList<FoursquareVenue> mVenues = new ArrayList<FoursquareVenue>();
    private Location mLocation;

    public RecordingMetaData(Context context) {
        super(context);
        init();
    }

    public RecordingMetaData(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RecordingMetaData(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init(){
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.metadata, this);

        mImageDir = new File(Consts.EXTERNAL_STORAGE_DIRECTORY, "recordings/images");
        CloudUtils.mkdirs(mImageDir);


        mArtwork = (ImageView) findViewById(R.id.artwork);
        mWhatText = (EditText) findViewById(R.id.what);
        mWhereText = (TextView) findViewById(R.id.where);
        if (mLocation == null) preloadLocations();
    }

    public void setActivity(Activity activity){
        mActivity = activity;

         mWhereText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), LocationPicker.class);
                intent.putExtra("name", ((TextView) v).getText().toString());
                if (mRecording.longitude != 0) {
                    intent.putExtra("long", mRecording.longitude);
                }
                if (mRecording.latitude != 0) {
                    intent.putExtra("lat", mRecording.latitude);
                }
                synchronized (this) {
                  intent.putParcelableArrayListExtra("venues", mVenues);
                  intent.putExtra("location", mLocation);
                }
                mActivity.startActivityForResult(intent, LocationPicker.PICK_VENUE);
            }
        });

        mArtwork.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getContext(), R.string.cloud_upload_clear_artwork, Toast.LENGTH_LONG).show();
            }
        });


        findViewById(R.id.txt_artwork_bg).setOnClickListener(
            new ImageUtils.ImagePickListener(mActivity) {
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
    }

    private void preloadLocations() {
        LocationManager mgr = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
        final Location location = mgr.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
        if (location != null) {
            new FoursquareVenueTask() {
                @Override
                protected void onPostExecute(List<FoursquareVenue> venues) {
                    if (venues != null && !venues.isEmpty()) {
                        synchronized (this) {
                          mLocation = location;
                          mVenues.addAll(venues);
                        }
                    }
                }
            }.execute(location);
        }
    }

    public void setRecording(Recording recording){
        mRecording = recording;
    }

    /* package */
    public void setWhere(String where, String id, double lng, double lat) {
        if (where != null) {
            mWhereText.setTextKeepState(where);
        }
        mFourSquareVenueId = id;
        mLong = lng;
        mLat = lat;
    }

    public void onSaveInstanceState(Bundle state) {
        state.putString("createWhatValue", mWhatText.getText().toString());
        state.putString("createWhereValue", mWhereText.getText().toString());

        if (mArtworkFile != null) {
            state.putString("createArtworkPath", mArtworkFile.getAbsolutePath());
        }

        state.putParcelableArrayList("venues", mVenues);
        state.putParcelable("location", mLocation);
    }

    public void onRestoreInstanceState(Bundle state) {
        mWhatText.setText(state.getString("createWhatValue"));
        mWhereText.setText(state.getString("createWhereValue"));
        mVenues = state.getParcelableArrayList("venues");
        mLocation = state.getParcelable("location");

        if (!TextUtils.isEmpty(state.getString("createArtworkPath"))) {
            setImage(new File(state.getString("createArtworkPath")));
        }
    }

    public void setImage(File file) {
        mArtworkFile = file;
        ImageUtils.setImage(file, mArtwork, (int) (getResources().getDisplayMetrics().density * 100f),(int) (getResources().getDisplayMetrics().density * 100f));
    }


    public void clearArtwork() {
        mArtworkFile = null;
        mArtwork.setVisibility(View.GONE);
        if (mArtwork.getDrawable() instanceof BitmapDrawable) {
            ImageUtils.clearBitmap(((BitmapDrawable) mArtwork.getDrawable()).getBitmap());
        }
    }

    public void mapToRecording(final Recording recording) {
        recording.what_text = mWhatText.getText().toString();
        recording.where_text = mWhereText.getText().toString();
        recording.artwork_path = mArtworkFile;

        if (mFourSquareVenueId != null) {
            recording.four_square_venue_id = mFourSquareVenueId;
        }
        recording.latitude = mLat;
        recording.longitude = mLong;
    }

    public void mapFromRecording(final Recording recording) {
        if (!TextUtils.isEmpty(recording.what_text)) mWhatText.setTextKeepState(recording.what_text);
        if (!TextUtils.isEmpty(recording.where_text)) mWhereText.setTextKeepState(recording.where_text);
        if (recording.artwork_path != null) setImage(recording.artwork_path);

        setWhere(TextUtils.isEmpty(recording.where_text) ? "" : recording.where_text,
                TextUtils.isEmpty(recording.four_square_venue_id) ? "" : recording.four_square_venue_id,
                recording.longitude,
                recording.latitude);

    }

    public File getCurrentImageFile() {
        return (mRecording == null) ? null : mRecording.generateImageFile(mImageDir);
    }

    public void setDefaultImage() {
        setImage(getCurrentImageFile());
    }
}
