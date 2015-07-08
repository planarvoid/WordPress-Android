package com.soundcloud.android.deeplinks;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.api.legacy.model.ScModelManager;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.net.Uri;

@RunWith(SoundCloudTestRunner.class)
public class UrnResolverTest {
    @Mock
    private ScModelManager modelManager;
    private UrnResolver resolver = new UrnResolver();

    @Test
    public void shouldResolveUserUrn() throws Exception {
        Uri uri = Uri.parse("soundcloud:users:123");
        expect(resolver.toUrn(uri)).toEqual(Urn.forUser(123L));
    }

    @Test
    public void shouldResolveTrackUrn() throws Exception {
        Uri uri = Uri.parse("soundcloud:tracks:123");
        expect(resolver.toUrn(uri)).toEqual(Urn.forTrack(123L));
    }

    @Test
    public void shouldResolveSoundUrn() throws Exception {
        Uri uri = Uri.parse("soundcloud:sounds:123");
        expect(resolver.toUrn(uri)).toEqual(Urn.forTrack(123L));
    }

    @Test
    public void shouldResolvePlaylistUrn() throws Exception {
        Uri uri = Uri.parse("soundcloud:playlists:123");
        expect(resolver.toUrn(uri)).toEqual(Urn.forPlaylist(123L));
    }

    @Test
    public void shouldResolveTwitterTrackDeepLink() throws Exception {
        Uri uri = Uri.parse("soundcloud://sounds:123/");
        expect(resolver.toUrn(uri)).toEqual(Urn.forTrack(123L));
    }

    @Test
    public void shouldResolveFacebookTrackDeepLink() throws Exception {
        Uri uri = Uri.parse("soundcloud://sounds:123/?target_url=https://soundcloud.com/manchesterorchestra/cope?utm_source=soundcloud&utm_campaign=share&utm_medium=facebook");
        expect(resolver.toUrn(uri)).toEqual(Urn.forTrack(123L));
    }

    @Test
    public void shouldResolveFacebookUserDeepLink() throws Exception {
        Uri uri = Uri.parse("soundcloud://users:123/?target_url=https://soundcloud.com/manchesterorchestra/cope?utm_source=soundcloud&utm_campaign=share&utm_medium=facebook");
        expect(resolver.toUrn(uri)).toEqual(Urn.forUser(123L));
    }

    @Test
    public void shouldResolveFacebookPlaylistDeepLink() throws Exception {
        Uri uri = Uri.parse("soundcloud://playlists:123/?target_url=https://soundcloud.com/manchesterorchestra/cope?utm_source=soundcloud&utm_campaign=share&utm_medium=facebook");
        expect(resolver.toUrn(uri)).toEqual(Urn.forPlaylist(123L));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfGivenUriIsNotParseableToUrn() throws Exception {
        resolver.toUrn(Uri.parse("http://123"));
    }
}