package com.soundcloud.android.creators.upload;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnLongClick;
import com.soundcloud.android.R;
import com.soundcloud.android.api.legacy.model.Recording;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.images.ImageUtils;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
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
    private Drawable placeholder;
    private Activity activity;

    @Bind(R.id.title) EditText titleText;
    @Bind(R.id.artwork) ImageView artwork;

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
        View view = inflater.inflate(R.layout.metadata, this);
        ButterKnife.bind(this, view);

        if (!isInEditMode()) {
            IOUtils.mkdirs(Recording.IMAGE_DIR);
        }
    }

    public void setActivity(final FragmentActivity activity) {
        this.activity = activity;
    }

    @OnClick(R.id.artwork_button)
    void onArtworkButtonClick() {
        showImagePickerDialog();
    }

    @OnClick(R.id.artwork)
    void onArtworkClick() {
        if (!hasImage()) {
            showImagePickerDialog();
        } else {
            Toast.makeText(getContext(), R.string.cloud_upload_clear_artwork, Toast.LENGTH_LONG).show();
        }
    }

    @OnLongClick(R.id.artwork)
    boolean onArtworkLongClick() {
        recording.clearArtwork();
        clearArtwork();
        return true;
    }

    private void showImagePickerDialog() {
        ImageUtils.showImagePickerDialog(activity, recording.getImageFile());
    }

    public void setRecording(Recording recording, boolean map) {
        this.recording = recording;
        if (map) {
            mapFromRecording(recording);
        }

        if (recording != null) {
            titleText.setHint(recording.sharingNote(getResources()));
            titleText.setText(recording.title);
            setImage(recording.artwork_path);
        }
    }

    /* package */
    public void setTitle(String title) {
        if (title != null) {
            titleText.setTextKeepState(title);
        }
    }

    public void onSaveInstanceState(Bundle state) {
        state.putString("createTitleValue", titleText.getText().toString());
        state.putParcelable("recording", recording);
    }

    public void onRestoreInstanceState(Bundle state) {
        titleText.setText(state.getString("createTitleValue"));
        recording = state.getParcelable("recording");

        if (!TextUtils.isEmpty(state.getString("createArtworkPath"))) {
            setImage(new File(state.getString("createArtworkPath")));
        }
    }

    public void reset() {
        titleText.setText(null);
        clearArtwork();
        recording = null;
    }

    public void setImage(File file) {
        if (file != null) {
            int iconWidth = (int) getResources().getDimension(R.dimen.record_progress_icon_width);
            int iconHeight = (int) getResources().getDimension(R.dimen.share_progress_icon_height);
            ImageUtils.setImage(file, artwork, iconWidth, iconHeight);
        } else {
            clearArtwork();
        }
    }

    public boolean hasPlaceholder() {
        return placeholder != null;
    }

    public void setPlaceholder(Drawable drawable) {
        placeholder = drawable;
        setImage(placeholder);
    }

    public void setImage(Drawable drawable) {
        artwork.setImageDrawable(drawable);
    }

    private void clearArtwork() {
        if (hasImage()) {
            ImageUtils.recycleImageViewBitmap(artwork);
        }

        if (placeholder != null) {
            setImage(placeholder);
        }
    }

    private boolean hasImage() {
        return artwork.getDrawable() instanceof BitmapDrawable;
    }

    public void mapToRecording(final Recording recording) {
        recording.title = titleText.getText().toString();
    }

    public void mapFromRecording(final Recording recording) {
        setTitle(recording.title);
        setImage(recording.artwork_path);
    }

    public void onDestroy() {
        clearArtwork();
    }
}
