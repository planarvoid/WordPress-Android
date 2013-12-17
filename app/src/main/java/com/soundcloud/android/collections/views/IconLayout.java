
package com.soundcloud.android.collections.views;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.image.ImageOperations;
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

    private ImageOperations mImageOperations = SoundCloudApplication.getImageOperations();

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
        mImageOperations.displayInAdapterView(getIconRemoteUri(), mIcon, getDefaultArtworkResId());
    }

    abstract protected int getDefaultArtworkResId();

    public String getIconRemoteUri() {
        return "";
    }

}
