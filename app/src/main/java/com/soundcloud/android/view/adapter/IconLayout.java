
package com.soundcloud.android.view.adapter;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
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

    protected ImageView mIcon;

    private DisplayImageOptions options = new DisplayImageOptions.Builder()
            .resetViewBeforeLoading(true)
            .showImageForEmptyUri(getDefaultArtworkResId())
            .showStubImage(getDefaultArtworkResId())
            .displayer(new FadeInBitmapDisplayer(200))
            .build();

    public IconLayout(Context context) {
        this(context,null);
    }

    public IconLayout(Context context, @Nullable AttributeSet attributeSet) {
        super(context, attributeSet);
        addContent(attributeSet);
        mIcon = (ImageView) findViewById(R.id.icon);
    }

    public long getCurrentUserId() {
        return SoundCloudApplication.getUserId();
    }

    protected abstract View addContent(AttributeSet attributeSet);

    protected void loadIcon() {
        ImageLoader.getInstance().displayImage(getIconRemoteUri(), mIcon, options);
    }

    abstract protected int getDefaultArtworkResId();

    public String getIconRemoteUri() {
        return "";
    }
}
