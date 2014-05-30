
package com.soundcloud.android.collections.views;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
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

    protected ImageView icon;
    protected final ImageOperations imageOperations;

    public IconLayout(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, SoundCloudApplication.fromContext(context).getImageOperations());
    }

    public IconLayout(Context context, ImageOperations imageOperations) {
        this(context, null, imageOperations);
    }

    public IconLayout(Context context, @Nullable AttributeSet attributeSet, ImageOperations imageOperations) {
        super(context, attributeSet);
        addContent(attributeSet);
        icon = (ImageView) findViewById(R.id.icon);
        this.imageOperations = imageOperations;
    }

    protected abstract View addContent(AttributeSet attributeSet);

    protected void loadIcon() {
        imageOperations.displayInAdapterView(getResourceUrn(),
                ApiImageSize.getListItemImageSize(getContext()),
                icon);
    }

    @Nullable
    public Urn getResourceUrn() {
        return null;
    }

}
