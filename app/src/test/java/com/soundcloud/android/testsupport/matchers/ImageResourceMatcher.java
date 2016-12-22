package com.soundcloud.android.testsupport.matchers;

import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

public class ImageResourceMatcher extends BaseMatcher<ImageResource> {

    private final Urn urn;
    private final Optional<String> imageUrlTemplate;

    public ImageResourceMatcher(Urn urn, Optional<String> imageUrlTemplate) {
        this.urn = urn;
        this.imageUrlTemplate = imageUrlTemplate;
    }

    @Override
    public boolean matches(Object o) {
        if (o instanceof ImageResource) {
            ImageResource imageResource = (ImageResource) o;
            return imageResource.getUrn().equals(urn) &&
                    imageResource.getImageUrlTemplate().equals(imageUrlTemplate);
        }
        return false;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("Expected to match ImageResource");
    }

    public static ImageResourceMatcher isImageResourceFor(Urn urn, Optional<String> imageUrlTemplate) {
        return new ImageResourceMatcher(urn, imageUrlTemplate);
    }

    public static ImageResourceMatcher isImageResourceFor(PropertySet entityProperties) {
        return new ImageResourceMatcher(entityProperties.get(EntityProperty.URN),
                                        entityProperties.get(EntityProperty.IMAGE_URL_TEMPLATE));
    }
}
