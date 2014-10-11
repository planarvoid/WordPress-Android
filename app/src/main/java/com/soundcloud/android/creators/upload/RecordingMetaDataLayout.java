package com.soundcloud.android.creators.upload;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.api.legacy.model.FoursquareVenue;
import com.soundcloud.android.api.legacy.model.Recording;
import com.soundcloud.android.creators.upload.tasks.FoursquareVenueTask;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.images.ImageUtils;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class RecordingMetaDataLayout extends RelativeLayout {
    private Recording recording;
    private File artworkFile;

    private EditText whatText;
    private TextView whereText;
    private ImageView artwork;

    private String fourSquareVenueId;
    private double longitude, latitude;

    // used for preloading foursquare venues
    private List<FoursquareVenue> venues = new ArrayList<>();
    private Location location;

    @SuppressWarnings("UnusedDeclaration")
    public RecordingMetaDataLayout(Context context) {
        super(context);
        init();
    }

    @SuppressWarnings("UnusedDeclaration")
    public RecordingMetaDataLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    @SuppressWarnings("UnusedDeclaration")
    public RecordingMetaDataLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.metadata, this);

        IOUtils.mkdirs(Recording.IMAGE_DIR);

        artwork = (ImageView) findViewById(R.id.artwork);
        whatText = (EditText) findViewById(R.id.what);
        whereText = (TextView) findViewById(R.id.where);
        if (location == null) {
            preloadLocations();
        }
    }

    public void setActivity(final FragmentActivity activity) {
        whereText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (recording == null) {
                    return;
                }
                Intent intent = new Intent(getContext(), LocationPickerActivity.class);
                intent.putExtra("name", ((TextView) v).getText().toString());
                if (recording.longitude != 0) {
                    intent.putExtra("long", recording.longitude);
                }
                if (recording.latitude != 0) {
                    intent.putExtra("lat", recording.latitude);
                }
                synchronized (this) {
                    intent.putParcelableArrayListExtra("venues", venues);
                    intent.putExtra("location", location);
                }
                activity.startActivityForResult(intent, Consts.RequestCodes.PICK_VENUE);
            }
        });

        artwork.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getContext(), R.string.cloud_upload_clear_artwork, Toast.LENGTH_LONG).show();
            }
        });


        findViewById(R.id.txt_artwork_bg).setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ImageUtils.showImagePickerDialog(activity, activity.getSupportFragmentManager(), UploadActivity.DIALOG_PICK_IMAGE);
                    }
                });

        artwork.setOnLongClickListener(new View.OnLongClickListener() {
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
                            RecordingMetaDataLayout.this.location = location;
                            RecordingMetaDataLayout.this.venues.addAll(venues);
                        }
                    }
                }
            }.execute(location);
        }
    }

    public void setRecording(Recording recording, boolean map) {
        this.recording = recording;
        if (map) {
            mapFromRecording(recording);
        }
    }

    /* package */
    public void setWhere(String where, String id, double lng, double lat) {
        if (where != null) {
            whereText.setTextKeepState(where);
        }
        fourSquareVenueId = id;
        longitude = lng;
        latitude = lat;
    }

    public void onSaveInstanceState(Bundle state) {
        state.putString("createWhatValue", whatText.getText().toString());
        state.putString("createWhereValue", whereText.getText().toString());

        if (artworkFile != null) {
            state.putString("createArtworkPath", artworkFile.getAbsolutePath());
        }

        state.putParcelableArrayList("venues", venues);
        state.putParcelable("location", location);
        state.putParcelable("recording", recording);
    }

    public void onRestoreInstanceState(Bundle state) {
        whatText.setText(state.getString("createWhatValue"));
        whereText.setText(state.getString("createWhereValue"));
        venues = state.getParcelableArrayList("venues");
        location = state.getParcelable("location");
        recording = state.getParcelable("recording");

        if (!TextUtils.isEmpty(state.getString("createArtworkPath"))) {
            setImage(new File(state.getString("createArtworkPath")));
        }
    }

    public void reset() {
        whatText.setText(null);
        whereText.setText(null);
        clearArtwork();
        recording = null;
    }

    public void setImage(File file) {
        if (file != null) {
            artworkFile = file;
            ImageUtils.setImage(file, artwork, (int) (getResources().getDisplayMetrics().density * 100f), (int) (getResources().getDisplayMetrics().density * 100f));
        }
    }

    private void clearArtwork() {
        artworkFile = null;
        artwork.setVisibility(View.GONE);
        if (artwork.getDrawable() instanceof BitmapDrawable) {
            ImageUtils.recycleImageViewBitmap(artwork);
        }
    }

    public void mapToRecording(final Recording recording) {
        recording.what_text = whatText.getText().toString();
        recording.where_text = whereText.getText().toString();
        recording.artwork_path = artworkFile;

        if (fourSquareVenueId != null) {
            recording.four_square_venue_id = fourSquareVenueId;
        }
        recording.latitude = latitude;
        recording.longitude = longitude;
    }

    public void mapFromRecording(final Recording recording) {
        if (!TextUtils.isEmpty(recording.what_text)) {
            whatText.setTextKeepState(recording.what_text);
        }
        if (!TextUtils.isEmpty(recording.where_text)) {
            whereText.setTextKeepState(recording.where_text);
        }
        if (recording.artwork_path != null) {
            setImage(recording.artwork_path);
        }

        setWhere(TextUtils.isEmpty(recording.where_text) ? "" : recording.where_text,
                TextUtils.isEmpty(recording.four_square_venue_id) ? "" : recording.four_square_venue_id,
                recording.longitude,
                recording.latitude);

    }

    public void onDestroy() {
        clearArtwork();
    }

}
