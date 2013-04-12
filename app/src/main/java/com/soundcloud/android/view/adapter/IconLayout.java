
package com.soundcloud.android.view.adapter;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.imageloader.ImageLoader;
import com.soundcloud.android.utils.ImageUtils;
import org.jetbrains.annotations.Nullable;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

/**
 * The base class for anything that needs to lazily load an icon.
 */
public abstract class IconLayout extends FrameLayout {

    private ImageLoader.Options mIconOptions;

    protected ImageLoader mImageLoader;
    protected ImageView mIcon;
    private ImageLoader.BindResult mCurrentIconBindResult;
    private ImageLoader.Callback mImageLoaderCallback = new ImageLoader.Callback() {
        @Override
        public void onImageError(ImageView view, String url, Throwable error) {
            mCurrentIconBindResult = ImageLoader.BindResult.ERROR;
        }

        @Override
        public void onImageLoaded(ImageView view, String url) {
            mCurrentIconBindResult = ImageLoader.BindResult.OK;
        }

    };

    public IconLayout(Context context) {
        this(context,null);
    }

    public IconLayout(Context context, @Nullable AttributeSet attributeSet) {
        super(context, attributeSet);

        if (mIconOptions == null) mIconOptions = ImageLoader.Options.listFadeIn();
        mImageLoader = ImageLoader.get(context);

        addContent(attributeSet);

        mIcon = (ImageView) findViewById(R.id.icon);
    }

    public long getCurrentUserId() {
        return SoundCloudApplication.getUserId();
    }

    protected abstract View addContent(AttributeSet attributeSet);

    public ImageView getIcon() {
        return mIcon;
    }

    protected void loadIcon() {
        final String iconUri = getIconRemoteUri();
        if (ImageUtils.checkIconShouldLoad(iconUri)) {
            mCurrentIconBindResult = mImageLoader.bind(mIcon, iconUri, mImageLoaderCallback, mIconOptions);
            if (mCurrentIconBindResult != ImageLoader.BindResult.OK){
                mIcon.setImageResource(getDefaultArtworkResId());
            }
        } else {
            mCurrentIconBindResult = ImageLoader.BindResult.OK;
            mImageLoader.unbind(mIcon);
            mIcon.setImageResource(getDefaultArtworkResId());
        }
    }

    protected boolean lastImageLoadFailed() {
        return mCurrentIconBindResult == ImageLoader.BindResult.ERROR;
    }

    abstract protected int getDefaultArtworkResId();

    public String getIconRemoteUri() {
        return "";
    }
}
