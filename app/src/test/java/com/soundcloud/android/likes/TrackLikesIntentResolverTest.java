package com.soundcloud.android.likes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Actions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import android.content.Intent;

@RunWith(MockitoJUnitRunner.class)
public class TrackLikesIntentResolverTest {

    @Mock Intent intent;

    private TrackLikesIntentResolver resolver;

    @Before
    public void setUp() throws Exception {
        resolver = new TrackLikesIntentResolver();
    }

    @Test
    public void checkDefaultFalse() throws Exception {
        assertThat(resolver.consumePlaybackRequest()).isFalse();
    }

    @Test
    public void nullIntent() throws Exception {
        resolver.onIntent(null);

        assertThat(resolver.consumePlaybackRequest()).isFalse();
    }

    @Test
    public void intentWithMissingExtra() throws Exception {
        when(intent.hasExtra(anyString())).thenReturn(false);
        resolver.onIntent(intent);

        assertThat(resolver.consumePlaybackRequest()).isFalse();
    }

    @Test
    public void intentWithWrongAction() throws Exception {
        Intent sourceIntent = mock(Intent.class);
        when(intent.hasExtra(anyString())).thenReturn(true);
        when(intent.getParcelableExtra(anyString())).thenReturn(sourceIntent);
        when(sourceIntent.getAction()).thenReturn(Actions.ACCOUNT_ADDED);

        resolver.onIntent(intent);

        assertThat(resolver.consumePlaybackRequest()).isFalse();
    }

    @Test
    public void intentWithCorrectExtra() throws Exception {
        Intent sourceIntent = mock(Intent.class);
        when(intent.hasExtra(anyString())).thenReturn(true);
        when(intent.getParcelableExtra(anyString())).thenReturn(sourceIntent);
        when(sourceIntent.getAction()).thenReturn(Actions.SHORTCUT_PLAY_LIKES);

        resolver.onIntent(intent);

        assertThat(resolver.consumePlaybackRequest()).isTrue();
        assertThat(resolver.consumePlaybackRequest()).isFalse();
    }
}
