
package com.soundcloud.android.view.adapter;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.imageloader.OldImageLoader;
import com.soundcloud.android.utils.images.ImageUtils;
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

    private OldImageLoader.Options mIconOptions;

    protected OldImageLoader mOldImageLoader;
    protected ImageView mIcon;
    private OldImageLoader.BindResult mCurrentIconBindResult;
    private OldImageLoader.Callback mImageLoaderCallback = new OldImageLoader.Callback() {
        @Override
        public void onImageError(ImageView view, String url, Throwable error) {
            mCurrentIconBindResult = OldImageLoader.BindResult.ERROR;
        }

        @Override
        public void onImageLoaded(ImageView view, String url) {
            mCurrentIconBindResult = OldImageLoader.BindResult.OK;
        }

    };

    public IconLayout(Context context) {
        this(context,null);
    }

    public IconLayout(Context context, @Nullable AttributeSet attributeSet) {
        super(context, attributeSet);

        if (mIconOptions == null) mIconOptions = OldImageLoader.Options.listFadeIn();
        mOldImageLoader = OldImageLoader.get(context);

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
            mCurrentIconBindResult = mOldImageLoader.bind(mIcon, iconUri, mImageLoaderCallback, mIconOptions);
            if (mCurrentIconBindResult != OldImageLoader.BindResult.OK){
                mIcon.setImageResource(getDefaultArtworkResId());
            }
        } else {
            mCurrentIconBindResult = OldImageLoader.BindResult.OK;
            mOldImageLoader.unbind(mIcon);
            mIcon.setImageResource(getDefaultArtworkResId());
        }
    }

    protected boolean lastImageLoadFailed() {
        return mCurrentIconBindResult == OldImageLoader.BindResult.ERROR;
    }

    abstract protected int getDefaultArtworkResId();

    public String getIconRemoteUri() {
        return "";
    }
}
