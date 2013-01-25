
package com.soundcloud.android.view;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.adapter.IScAdapter;
import com.soundcloud.android.imageloader.ImageLoader;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.utils.ImageUtils;
import com.soundcloud.android.view.adapter.TrackInfoBar;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.TextView;

public class MyTracklistRow extends TrackInfoBar {
    private TextView mTitle;
    private TextView mCreatedAt;
    private TextView mPrivateIndicator;
    private Drawable mPrivateBgDrawable;
    private Drawable mVeryPrivateBgDrawable;
    private final int mTargetIconDimension;

    public MyTracklistRow(Context activity, IScAdapter adapter) {
        super(activity, adapter);
        mTitle = (TextView) findViewById(R.id.track);
        mCreatedAt = (TextView) findViewById(R.id.track_created_at);
        mPrivateIndicator = (TextView) findViewById(R.id.private_indicator);
        mTargetIconDimension = (int) (getContext().getResources().getDisplayMetrics().density * ImageUtils.GRAPHIC_DIMENSIONS_BADGE);
    }

    @Override
    protected View addContent() {
        return View.inflate(getContext(), R.layout.record_list_item_row, this);
    }

    private Drawable getPrivateBgDrawable(){
          if (mPrivateBgDrawable == null) {
              mPrivateBgDrawable = getResources().getDrawable(R.drawable.round_rect_gray);
              mPrivateBgDrawable.setBounds(0, 0, mPrivateBgDrawable.getIntrinsicWidth(), mPrivateBgDrawable.getIntrinsicHeight());
          }
        return mPrivateBgDrawable;
    }

    private Drawable getVeryPrivateBgDrawable(){
          if (mVeryPrivateBgDrawable == null) {
              mVeryPrivateBgDrawable = getResources().getDrawable(R.drawable.round_rect_orange);
              mVeryPrivateBgDrawable.setBounds(0, 0, mVeryPrivateBgDrawable.getIntrinsicWidth(), mVeryPrivateBgDrawable.getIntrinsicHeight());
          }
        return mVeryPrivateBgDrawable;
    }

     @Override
    public void display(Cursor cursor) {
        display(cursor.getPosition(), SoundCloudApplication.MODEL_MANAGER.getTrackFromCursor(cursor));
    }
    @Override
    public void display(int position, Parcelable p) {
        if (!(p instanceof Recording)) {
            SoundCloudApplication.handleSilentException("item "+p+" at position " +position + " is not a recording, "+
                    "adapter="+mAdapter, null);
            return;
        }

        final Recording recording = ((Recording) p);

        mTitle.setText(recording.sharingNote(getResources()));

        if (!recording.is_private) {
            mPrivateIndicator.setVisibility(View.GONE);
        } else {
            if (!TextUtils.isEmpty(recording.getRecipientUsername())){
                mPrivateIndicator.setBackgroundDrawable(getVeryPrivateBgDrawable());
                mPrivateIndicator.setText(recording.getRecipientUsername());
            } else {
                final int sharedToCount = TextUtils.isEmpty(recording.shared_emails) ? 0
                        : recording.shared_emails.split(",").length;
                if (sharedToCount < 8){
                    mPrivateIndicator.setBackgroundDrawable(getVeryPrivateBgDrawable());
                } else {
                    mPrivateIndicator.setBackgroundDrawable(getPrivateBgDrawable());
                }
                mPrivateIndicator.setText(sharedToCount > 0 ? (sharedToCount == 1 ?
                            getContext().getString(R.string.tracklist_item_shared_with_1_person) :
                            getContext().getString(R.string.tracklist_item_shared_with_x_people, sharedToCount))
                        : getContext().getString(R.string.tracklist_item_shared_with_you));
            }
            mPrivateIndicator.setVisibility(View.VISIBLE);
        }

        mCreatedAt.setTextColor(getContext().getResources().getColor(R.color.listTxtRecSecondary));
        mCreatedAt.setText(recording.getStatus(getContext().getResources()));
        setArtwork(recording);

        if (recording.isUploading()) {
            if (findViewById(R.id.processing_progress) != null) {
                findViewById(R.id.processing_progress).setVisibility(View.VISIBLE);
            } else {
                ((ViewStub) findViewById(R.id.processing_progress_stub)).inflate();
            }
        } else if (findViewById(R.id.processing_progress) != null) {
            findViewById(R.id.processing_progress).setVisibility(View.GONE);
        }
    }

    private void setArtwork(final Recording recording) {

        if (recording.artwork_path == null) {
            mImageLoader.unbind(mIcon);
        } else {
            // use getBitmap instead of bind here because we have to account for exif rotation on local images
            final String fileUri = Uri.fromFile(recording.artwork_path).toString();
            mImageLoader.getBitmap(
                    fileUri,
                    new ImageLoader.BitmapCallback(){
                        @Override
                        public void onImageLoaded(Bitmap bitmap, String url) {
                            if (fileUri.equals(url)) setImageBitmap(recording, bitmap);
                        }
                    },
                    ImageUtils.getImageLoaderOptionsWithResizeSet(recording.artwork_path, mTargetIconDimension, mTargetIconDimension, false)
            );
        }
    }

    private void setImageBitmap(Recording recording, Bitmap b) {
        ImageUtils.setImage(b, mIcon,
                (int) getResources().getDimension(R.dimen.list_icon_width),
                (int) getResources().getDimension(R.dimen.list_icon_height),
                ImageUtils.getExifRotation(recording.artwork_path));
    }
}
