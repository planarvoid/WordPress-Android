package com.soundcloud.android.image;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ImageStyleTest {
    @Test
    public void findsNothingWhenNullImageStyleIdentifier() {
        assertThat(ImageStyle.fromIdentifier(null)).isNull();
    }

    @Test
    public void findsNothingWhenUnknownImageStyleIdentifier() {
        assertThat(ImageStyle.fromIdentifier("unknown")).isNull();
    }

    @Test
    public void findsKnownImageStyleByIdentifier() {
        assertThat(ImageStyle.fromIdentifier("circular")).isEqualTo(ImageStyle.CIRCULAR);
        assertThat(ImageStyle.fromIdentifier("square")).isEqualTo(ImageStyle.SQUARE);
        assertThat(ImageStyle.fromIdentifier("station")).isEqualTo(ImageStyle.STATION);
    }

    @Test
    public void itemTypesExposeIdentifier() {
        assertThat(ImageStyle.CIRCULAR.toIdentifier()).isEqualTo("circular");
        assertThat(ImageStyle.SQUARE.toIdentifier()).isEqualTo("square");
        assertThat(ImageStyle.STATION.toIdentifier()).isEqualTo("station");
    }
}
