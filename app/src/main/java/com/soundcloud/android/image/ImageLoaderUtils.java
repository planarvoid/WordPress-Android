package com.soundcloud.android.image;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.MemoryCacheUtil;
import com.soundcloud.android.R;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.utils.ScTextUtils;

import android.content.Context;
import android.graphics.Bitmap;

public class ImageLoaderUtils {

    private final ImageLoader mImageLoader;
    private final Context mContext;

    public ImageLoaderUtils(Context context) {
        this(ImageLoader.getInstance(), context);
    }

    public ImageLoaderUtils(ImageLoader imageLoader, Context context) {
        mImageLoader = imageLoader;
        mContext = context;
    }

    /**
     * Get an instance of a list sized bitmap for a particular track (for player image substitution)
     */
    public Bitmap getCachedTrackListIcon(Track track) {
        final String listArtworkUrl = track.getListArtworkUrl(mContext);
        if (ScTextUtils.isBlank(listArtworkUrl)) {
            return null;
        } else {
            return mImageLoader.getMemoryCache().get(MemoryCacheUtil.generateKey(
                    listArtworkUrl,
                    new com.nostra13.universalimageloader.core.assist.ImageSize(
                            (int) mContext.getResources().getDimension(R.dimen.list_icon_width),
                            (int) mContext.getResources().getDimension(R.dimen.list_icon_height)
                    )
            ));
        }
    }
}
