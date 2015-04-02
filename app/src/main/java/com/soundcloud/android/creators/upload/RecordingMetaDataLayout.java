package com.soundcloud.android.creators.upload;

import com.soundcloud.android.R;
import com.soundcloud.android.api.legacy.model.Recording;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.images.ImageUtils;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.File;

public class RecordingMetaDataLayout extends RelativeLayout {

    private Recording recording;
    private File artworkFile;

    private EditText whatText;
    private EditText whereText;
    private ImageView artwork;

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
        whereText = (EditText) findViewById(R.id.where);
    }

    public void setActivity(final FragmentActivity activity) {
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
                        ImageUtils.showImagePickerDialog(activity, recording.generateImageFile(Recording.IMAGE_DIR));
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

    public void setRecording(Recording recording, boolean map) {
        this.recording = recording;
        if (map) {
            mapFromRecording(recording);
        }
    }

    /* package */
    public void setWhere(String where) {
        if (where != null) {
            whereText.setTextKeepState(where);
        }
    }

    public void onSaveInstanceState(Bundle state) {
        state.putString("createWhatValue", whatText.getText().toString());
        state.putString("createWhereValue", whereText.getText().toString());

        if (artworkFile != null) {
            state.putString("createArtworkPath", artworkFile.getAbsolutePath());
        }

        state.putParcelable("recording", recording);
    }

    public void onRestoreInstanceState(Bundle state) {
        whatText.setText(state.getString("createWhatValue"));
        whereText.setText(state.getString("createWhereValue"));
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

        setWhere(TextUtils.isEmpty(recording.where_text) ? "" : recording.where_text);
    }

    public void onDestroy() {
        clearArtwork();
    }

}
