package com.soundcloud.android.deeplinks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ChartCategory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import android.net.Uri;

@RunWith(MockitoJUnitRunner.class)
public class AllGenresUriResolverTest {

    @Mock Uri uri;

    @Test
    public void resolveWithMusicUri() throws Exception {
        when(uri.toString()).thenReturn("soundcloud://charts:music");
        ChartCategory category = AllGenresUriResolver.resolveUri(uri);

        assertThat(category).isEqualTo(ChartCategory.MUSIC);
    }

    @Test
    public void resolveWithAudioUri() throws Exception {
        when(uri.toString()).thenReturn("soundcloud://charts:audio");

        ChartCategory category = AllGenresUriResolver.resolveUri(uri);

        assertThat(category).isEqualTo(ChartCategory.AUDIO);
    }

    @Test
    public void resolveWithInvalidUri() throws Exception {
        when(uri.toString()).thenReturn("soundcloud://ihavenoideawhatimdoing");

        ChartCategory category = AllGenresUriResolver.resolveUri(uri);

        assertThat(category).isEqualTo(ChartCategory.NONE);
    }
}
