package com.soundcloud.android.cast;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.framework.media.ImageHints;
import com.google.android.gms.cast.framework.media.ImagePicker;
import com.google.android.gms.common.images.WebImage;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.net.Uri;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CastImagePickerTest extends AndroidUnitTest {

    @Mock MediaMetadata mediaMetadata;
    @Mock ImageHints imageHints;
    private WebImage webImage = new WebImage(Uri.parse("http://www.fake.com"));

    private CastImagePicker imagePicker;

    @Before
    public void setUp() {
        imagePicker = new CastImagePicker();
    }

    @Test
    public void onPickImageReturnsNullIfTheImageListIsNullOnTheMediaMetadata() throws Exception {
        when(mediaMetadata.getImages()).thenReturn(null);

        assertThat(imagePicker.onPickImage(mediaMetadata, imageHints)).isNull();
    }

    @Test
    public void onPickImageReturnsNullIfThereArentImagesOnTheMediaMetadata() throws Exception {
        when(mediaMetadata.getImages()).thenReturn(Collections.emptyList());

        assertThat(imagePicker.onPickImage(mediaMetadata, imageHints)).isNull();
    }

    @Test
    public void onPickImageReturnsTheImageIfThatsTheOnlyOneOnTheMediaMetadata() throws Exception {
        when(mediaMetadata.getImages()).thenReturn(Collections.singletonList(webImage));
        when(mediaMetadata.hasImages()).thenReturn(true);

        assertThat(imagePicker.onPickImage(mediaMetadata, imageHints)).isEqualTo(webImage);
    }

    @Test
    public void onPickImageReturnsTheFirstImageOfTheNonSingletonListInCaseItIsHintedAsTheDialogBackgroundImage() {
        List<WebImage> webImages = webImageList(webImage, new WebImage(Uri.parse("http://www.fake2.com")));
        when(mediaMetadata.getImages()).thenReturn(webImages);
        when(mediaMetadata.hasImages()).thenReturn(true);
        when(imageHints.getType()).thenReturn(ImagePicker.IMAGE_TYPE_MEDIA_ROUTE_CONTROLLER_DIALOG_BACKGROUND);

        assertThat(imagePicker.onPickImage(mediaMetadata, imageHints)).isEqualTo(webImage);
    }

    @Test
    public void onPickImageReturnsTheSecondImageOfTheNonSingletonListInCaseItIsNotHintedAsTheDialogBackgroundImage() {
        WebImage webImage2 = new WebImage(Uri.parse("http://www.fake2.com"));
        List<WebImage> webImages = webImageList(webImage, webImage2);
        when(mediaMetadata.getImages()).thenReturn(webImages);
        when(mediaMetadata.hasImages()).thenReturn(true);
        when(imageHints.getType()).thenReturn(ImagePicker.IMAGE_TYPE_LOCK_SCREEN_BACKGROUND);

        assertThat(imagePicker.onPickImage(mediaMetadata, imageHints)).isEqualTo(webImage2);
    }

    private List<WebImage> webImageList(WebImage... webImages) {
        List<WebImage> list = new ArrayList<>(webImages.length);
        Collections.addAll(list, webImages);
        return list;
    }
}