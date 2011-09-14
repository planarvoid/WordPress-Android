
package com.soundcloud.android.view;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.google.android.imageloader.ImageLoader;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.utils.ImageUtils;

import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.IOException;

public class MyTracklistRow extends TracklistRow {
    private TextView mTitle;
    private TextView mCreatedAt;
    private Drawable mPrivateDrawable;

    public MyTracklistRow(ScActivity activity, LazyBaseAdapter adapter) {
        super(activity, adapter);
        mTitle = (TextView) findViewById(R.id.track);
        mCreatedAt = (TextView) findViewById(R.id.track_created_at);
    }

    @Override
    protected int getRowResourceId() {
        return R.layout.record_list_item_row;
    }

    private Drawable getPrivateDrawable() {
        if (mPrivateDrawable == null) mPrivateDrawable = getContext().getResources().getDrawable(R.drawable.very_private);
        return mPrivateDrawable;
    }

    @Override
    public void display(int position) {
        Recording recording = ((Recording) mAdapter.getItem(position));

        mTitle.setText(recording.sharingNote(getResources()));

        if (recording.is_private) {
            mTitle.setCompoundDrawablesWithIntrinsicBounds(null,null,getPrivateDrawable(),null);
        } else {
            mTitle.setCompoundDrawables(null,null,null,null);
        }

        mCreatedAt.setTextColor(mActivity.getResources().getColor(R.color.listTxtRecSecondary));
        mCreatedAt.setText(recording.getStatus(mActivity.getResources()));

        mCloseIcon.setVisibility(recording.upload_status == 1 ? View.VISIBLE : View.GONE);

        if (recording.artwork_path == null) {
            mImageLoader.unbind(getRowIcon());
        } else {
            ImageLoader.Options options = new ImageLoader.Options();
            try {
                options.decodeInSampleSize = ImageUtils.determineResizeOptions(
                        recording.artwork_path,
                        (int) (getContext().getResources().getDisplayMetrics().density * ImageUtils.GRAPHIC_DIMENSIONS_BADGE),
                        (int) (getContext().getResources().getDisplayMetrics().density * ImageUtils.GRAPHIC_DIMENSIONS_BADGE)).inSampleSize;
            } catch (IOException e) {
                Log.w(TAG, "error", e);
            }
            mImageLoader.bind(mAdapter, getRowIcon(), recording.artwork_path.getAbsolutePath(), options);
        }
    }
}
