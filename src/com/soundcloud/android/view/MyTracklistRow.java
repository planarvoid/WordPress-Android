
package com.soundcloud.android.view;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import android.content.Context;
import android.database.Cursor;
import android.os.Parcelable;
import android.text.TextUtils;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import com.google.android.imageloader.ImageLoader;
import com.soundcloud.android.R;
import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.utils.ImageUtils;

import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.IOException;

public class MyTracklistRow extends TrackInfoBar {
    private TextView mTitle;
    private TextView mCreatedAt;
    private TextView mPrivateIndicator;
    private Drawable mPrivateBgDrawable;
    private Drawable mVeryPrivateBgDrawable;
    private ImageView mCloseIcon;

    public MyTracklistRow(Context activity, LazyBaseAdapter adapter) {
        super(activity, adapter);
        mTitle = (TextView) findViewById(R.id.track);
        mCreatedAt = (TextView) findViewById(R.id.track_created_at);
        mPrivateIndicator = (TextView) findViewById(R.id.private_indicator);
        mCloseIcon = (ImageView) findViewById(R.id.close_icon);
    }

    @Override
    protected int getRowResourceId() {
        return R.layout.record_list_item_row;
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
        display(cursor.getPosition(), new Track(cursor));
    }
    @Override
    public void display(int position, Parcelable p) {
        Recording recording = ((Recording) p);

        mTitle.setText(recording.sharingNote(getResources()));

        if (!recording.is_private) {
            mPrivateIndicator.setVisibility(View.GONE);
        } else {
            if (!TextUtils.isEmpty(recording.private_username)){
                mPrivateIndicator.setBackgroundDrawable(getVeryPrivateBgDrawable());
                mPrivateIndicator.setText(recording.private_username);
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

        mCloseIcon.setVisibility(recording.upload_status == 1 ? View.VISIBLE : View.GONE);

        if (recording.artwork_path == null) {
            mImageLoader.unbind(mIcon);
        } else {
            ImageLoader.Options options = new ImageLoader.Options();
            try {
                options.decodeInSampleSize = ImageUtils.determineResizeOptions(
                        recording.artwork_path,
                        (int) (getContext().getResources().getDisplayMetrics().density * ImageUtils.GRAPHIC_DIMENSIONS_BADGE),
                        (int) (getContext().getResources().getDisplayMetrics().density * ImageUtils.GRAPHIC_DIMENSIONS_BADGE), false
                ).inSampleSize;
            } catch (IOException e) {
                Log.w(TAG, "error", e);
            }
            mImageLoader.bind((BaseAdapter) mAdapter, mIcon, recording.artwork_path.getAbsolutePath(), options);
        }
    }
}
