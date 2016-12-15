package com.soundcloud.android.cast;

import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.framework.media.ImageHints;
import com.google.android.gms.cast.framework.media.ImagePicker;
import com.google.android.gms.common.images.WebImage;

import android.support.annotation.NonNull;

import java.util.List;

public class CastImagePicker extends ImagePicker {

    @Override
    public WebImage onPickImage(MediaMetadata mediaMetadata, @NonNull ImageHints imageHints) {
        if ((mediaMetadata == null) || !mediaMetadata.hasImages()) {
            return null;
        }
        List<WebImage> images = mediaMetadata.getImages();
        if (images.size() == 1) {
            return images.get(0);
        } else {
            if (imageHints.getType() == ImagePicker.IMAGE_TYPE_MEDIA_ROUTE_CONTROLLER_DIALOG_BACKGROUND) {
                return images.get(0);
            } else {
                return images.get(1);
            }
        }
    }
}
