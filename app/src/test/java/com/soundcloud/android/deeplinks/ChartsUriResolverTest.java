package com.soundcloud.android.deeplinks;

import static com.soundcloud.android.api.model.ChartCategory.NONE;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.R;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;

import android.net.Uri;

public class ChartsUriResolverTest extends AndroidUnitTest {

    private ChartsUriResolver chartsUriResolver;

    @Before
    public void setUp() throws Exception {
        chartsUriResolver = new ChartsUriResolver(context(), resources());
    }

    @Test(expected = IllegalArgumentException.class)
    public void resolveInvalidUriThrows() throws Exception {
        chartsUriResolver.resolveUri(Uri.parse("asdfasdfasdfasdf"));
    }

    @Test
    public void resolveExternalChartsUri() throws Exception {
        Uri uri = Uri.parse("soundcloud://charts");
        ChartDetails details = chartsUriResolver.resolveUri(uri);
        assertThat(details).isEqualTo(ChartDetails.create(ChartType.TRENDING, Urn.forGenre("all-music"), NONE, Optional.absent()));
    }

    @Test
    public void resolveExternalWebUri_TopGenre() throws Exception {
        Uri uri = Uri.parse("https://soundcloud.com/charts/top?genre=electronic");
        ChartDetails details = chartsUriResolver.resolveUri(uri);
        assertThat(details).isEqualTo(ChartDetails.create(ChartType.TOP, Urn.forGenre("electronic"), NONE, electronicHeader()));
    }

    @Test
    public void resolveExternalWebIntent_NewGenre() throws Exception {
        Uri uri = Uri.parse("https://soundcloud.com/charts/new?genre=electronic");
        ChartDetails details = chartsUriResolver.resolveUri(uri);
        assertThat(details).isEqualTo(ChartDetails.create(ChartType.TRENDING, Urn.forGenre("electronic"), NONE, electronicHeader()));
    }

    @Test
    public void resolveExternalWebIntent_TopAll() throws Exception {
        Uri uri = Uri.parse("https://soundcloud.com/charts/top?genre=all");
        ChartDetails details = chartsUriResolver.resolveUri(uri);
        assertThat(details).isEqualTo(ChartDetails.create(ChartType.TOP, Urn.forGenre("all-music"), NONE, Optional.absent()));
    }

    @Test
    public void resolveExternalWebIntent_NewAll() throws Exception {
        Uri uri = Uri.parse("https://soundcloud.com/charts/new?genre=all");
        ChartDetails details = chartsUriResolver.resolveUri(uri);
        assertThat(details).isEqualTo(ChartDetails.create(ChartType.TRENDING, Urn.forGenre("all-music"), NONE, Optional.absent()));
    }

    @Test
    public void resolveExternalWebIntent_NewAll_WithoutGenre() throws Exception {
        Uri uri = Uri.parse("https://soundcloud.com/charts/new");
        ChartDetails details = chartsUriResolver.resolveUri(uri);
        assertThat(details).isEqualTo(ChartDetails.create(ChartType.TRENDING, Urn.forGenre("all-music"), NONE, Optional.absent()));
    }

    @Test
    public void resolveExternalWebIntent_TopAll_WithoutGenre() throws Exception {
        Uri uri = Uri.parse("https://soundcloud.com/charts/top");
        ChartDetails details = chartsUriResolver.resolveUri(uri);
        assertThat(details).isEqualTo(ChartDetails.create(ChartType.TOP, Urn.forGenre("all-music"), NONE, Optional.absent()));
    }

    @Test
    public void resolveExternalSoundcloudIntent_TopGenre() throws Exception {
        Uri uri = Uri.parse("soundcloud://charts:top:electronic");
        ChartDetails details = chartsUriResolver.resolveUri(uri);
        assertThat(details).isEqualTo(ChartDetails.create(ChartType.TOP, Urn.forGenre("electronic"), NONE, electronicHeader()));
    }

    @Test
    public void resolveExternalSoundcloudIntent_NewGenre() throws Exception {
        Uri uri = Uri.parse("soundcloud://charts:new:electronic");
        ChartDetails details = chartsUriResolver.resolveUri(uri);
        assertThat(details).isEqualTo(ChartDetails.create(ChartType.TRENDING, Urn.forGenre("electronic"), NONE, electronicHeader()));
    }

    @Test
    public void resolveExternalSoundcloudIntent_TopAll() throws Exception {
        Uri uri = Uri.parse("soundcloud://charts:top:all");
        ChartDetails details = chartsUriResolver.resolveUri(uri);
        assertThat(details).isEqualTo(ChartDetails.create(ChartType.TOP, Urn.forGenre("all-music"), NONE, Optional.absent()));
    }

    @Test
    public void resolveExternalSoundcloudIntent_NewAll() throws Exception {
        Uri uri = Uri.parse("soundcloud://charts:new:all");
        ChartDetails details = chartsUriResolver.resolveUri(uri);
        assertThat(details).isEqualTo(ChartDetails.create(ChartType.TRENDING, Urn.forGenre("all-music"), NONE, Optional.absent()));
    }

    @Test
    public void resolveExternalSoundcloudIntent_NewAll_WithoutGenre() throws Exception {
        Uri uri = Uri.parse("soundcloud://charts:new");
        ChartDetails details = chartsUriResolver.resolveUri(uri);
        assertThat(details).isEqualTo(ChartDetails.create(ChartType.TRENDING, Urn.forGenre("all-music"), NONE, Optional.absent()));
    }

    @Test
    public void resolveExternalSoundcloudIntent_TopAll_WithoutGenre() throws Exception {
        Uri uri = Uri.parse("soundcloud://charts:top");
        ChartDetails details = chartsUriResolver.resolveUri(uri);
        assertThat(details).isEqualTo(ChartDetails.create(ChartType.TOP, Urn.forGenre("all-music"), NONE, Optional.absent()));
    }

    @Test
    public void resolveExternalSoundcloudIntent_TopElectronic_WithWebScheme() throws Exception {
        Uri uri = Uri.parse("soundcloud://charts/top?genre=electronic");
        ChartDetails details = chartsUriResolver.resolveUri(uri);
        assertThat(details).isEqualTo(ChartDetails.create(ChartType.TOP, Urn.forGenre("electronic"), NONE, electronicHeader()));
    }

    @Test
    public void resolveExternalSoundcloudIntent_NewElectronic_WithWebScheme() throws Exception {
        Uri uri = Uri.parse("soundcloud://charts/new?genre=electronic");
        ChartDetails details = chartsUriResolver.resolveUri(uri);
        assertThat(details).isEqualTo(ChartDetails.create(ChartType.TRENDING, Urn.forGenre("electronic"), NONE, electronicHeader()));
    }

    @Test
    public void resolveExternalSoundcloudIntent_NewAll_WithWebScheme() throws Exception {
        Uri uri = Uri.parse("soundcloud://charts/new");
        ChartDetails details = chartsUriResolver.resolveUri(uri);
        assertThat(details).isEqualTo(ChartDetails.create(ChartType.TRENDING, Urn.forGenre("all-music"), NONE, Optional.absent()));
    }

    private Optional<String> electronicHeader() {
        return Optional.of(context().getString(R.string.charts_page_header, context().getString(R.string.charts_electronic)));
    }
}
