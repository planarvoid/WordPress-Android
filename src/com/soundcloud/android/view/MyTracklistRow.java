
package com.soundcloud.android.view;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.google.android.imageloader.ImageLoader;
import com.google.android.imageloader.ImageLoader.BindResult;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.utils.FastBitmapDrawable;
import com.soundcloud.android.utils.ImageUtils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.View;

import java.io.IOException;

public class MyTracklistRow extends TracklistRow {

    public MyTracklistRow(ScActivity _activity, LazyBaseAdapter _adapter) {
        super(_activity, _adapter);
    }

    @Override
    protected int getRowResourceId() {
        return R.layout.record_list_item_row;
    }

    @Override
    public void display(int position) {

        Recording recording = ((Recording) mAdapter.getItem(position));

        mTitle.setText(recording.sharingNote());

        if (recording.is_private) {
            mPrivateIndicator.setVisibility(View.VISIBLE);
        } else {
            mPrivateIndicator.setVisibility(View.GONE);
        }

        mCreatedAt.setTextColor(mActivity.getResources().getColor(R.color.listTxtRecSecondary));
        mCreatedAt.setText(recording.getStatus(mActivity.getResources()));

        mCloseIcon.setVisibility(recording.upload_status == 1 ? View.VISIBLE : View.GONE);

        if (recording.artwork_path == null) {
            mImageLoader.unbind(getRowIcon());
        } else {
            BindResult result;
            ImageLoader.Options options = new ImageLoader.Options();
            try {
                options.decodeInSampleSize = ImageUtils.determineResizeOptions(
                        recording.artwork_path,
                        (int) (getContext().getResources().getDisplayMetrics().density * ImageUtils.GRAPHIC_DIMENSIONS_BADGE),
                        (int) (getContext().getResources().getDisplayMetrics().density * ImageUtils.GRAPHIC_DIMENSIONS_BADGE)).inSampleSize;
            } catch (IOException e) {
                Log.w(TAG, "error", e);
            }
            result = mImageLoader.bind(mAdapter, getRowIcon(), recording.artwork_path.getAbsolutePath(), options);
        }
    }


}
