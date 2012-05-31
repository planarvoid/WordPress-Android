package com.soundcloud.android.view.create;

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
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.LocationPicker;
import com.soundcloud.android.model.FoursquareVenue;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.task.create.FoursquareVenueTask;
import com.soundcloud.android.tracking.Click;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.ImageUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class RecordingMetaData extends RelativeLayout {

    private Activity mActivity;
    private Recording mRecording;
    private File mArtworkFile;

    private EditText mWhatText;
    private TextView mWhereText;
    private ImageView mArtwork;

    private String mFourSquareVenueId;
    private double mLong, mLat;

    // used for preloading foursquare venues
    private ArrayList<FoursquareVenue> mVenues = new ArrayList<FoursquareVenue>();
    private Location mLocation;

    @SuppressWarnings("UnusedDeclaration")
    public RecordingMetaData(Context context) {
        super(context);
        init();
    }

    @SuppressWarnings("UnusedDeclaration")
    public RecordingMetaData(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    @SuppressWarnings("UnusedDeclaration")
    public RecordingMetaData(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.metadata, this);

        IOUtils.mkdirs(Recording.IMAGE_DIR);

        mArtwork = (ImageView) findViewById(R.id.artwork);
        mWhatText = (EditText) findViewById(R.id.what);
        mWhereText = (TextView) findViewById(R.id.where);
        if (mLocation == null) preloadLocations();
    }

    public void setActivity(Activity activity) {
        mActivity = activity;

         mWhereText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRecording == null) return;
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
                mActivity.startActivityForResult(intent, Consts.RequestCodes.PICK_VENUE);
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

                // tracking shizzle
                @Override public void onClick() { getApp().track(Click.Record_details_add_image); }
                @Override public void onExistingImage() { getApp().track(Click.Record_details_existing_image); }
                @Override public void onNewImage() { getApp().track(Click.Record_details_new_image); }
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
        setRecording(recording,false);
    }

    public void setRecording(Recording recording, boolean map){
        mRecording = recording;
        if (map) mapFromRecording(recording);
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
        state.putParcelable("recording", mRecording);
    }

    public void onRestoreInstanceState(Bundle state) {
        mWhatText.setText(state.getString("createWhatValue"));
        mWhereText.setText(state.getString("createWhereValue"));
        mVenues = state.getParcelableArrayList("venues");
        mLocation = state.getParcelable("location");
        mRecording = state.getParcelable("recording");

        if (!TextUtils.isEmpty(state.getString("createArtworkPath"))) {
            setImage(new File(state.getString("createArtworkPath")));
        }
    }

    public void reset(){
        mWhatText.setText(null);
        mWhereText.setText(null);
        clearArtwork();
        mRecording = null;
    }

    public void setImage(File file) {
        if (file != null){
            mArtworkFile = file;
            ImageUtils.setImage(file, mArtwork, (int) (getResources().getDisplayMetrics().density * 100f),(int) (getResources().getDisplayMetrics().density * 100f));
        }
    }

    private void clearArtwork() {
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
        return (mRecording == null) ? null : mRecording.generateImageFile(Recording.IMAGE_DIR);
    }

    public void onDestroy(){
        clearArtwork();
    }

    private SoundCloudApplication getApp() {
        return (SoundCloudApplication) mActivity.getApplication();
    }
}
