package com.soundcloud.android.explore;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Test;

import android.os.Parcel;

import java.util.HashMap;
import java.util.Map;

public class ExploreGenreTest extends AndroidUnitTest {

    private ExploreGenre category;

    @Test
    public void shouldBeParcelable() {
        category = new ExploreGenre("Title1");

        final Map<String, Link> links = new HashMap<>();
        links.put("link1", new Link("http://link1"));
        links.put("link2", new Link("http://link2"));
        category.setLinks(links);

        Parcel parcel = Parcel.obtain();
        category.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        ExploreGenre category = ExploreGenre.CREATOR.createFromParcel(parcel);
        assertThat(category.getTitle()).isEqualTo(this.category.getTitle());
        assertThat(category.getLinks().get("link1")).isEqualTo(this.category.getLinks().get("link1"));
        assertThat(category.getLinks().get("link2")).isEqualTo(this.category.getLinks().get("link2"));
    }

    @Test
    public void shouldParcelAndUnparcelIntoPopularMusicInstance() {
        category = ExploreGenre.POPULAR_MUSIC_CATEGORY;
        Parcel parcel = Parcel.obtain();
        category.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        ExploreGenre category = ExploreGenre.CREATOR.createFromParcel(parcel);
        assertThat(category).isEqualTo(ExploreGenre.POPULAR_MUSIC_CATEGORY);
    }

    @Test
    public void shouldParcelAndUnparcelIntoPopularAudioInstance() {
        category = ExploreGenre.POPULAR_AUDIO_CATEGORY;
        Parcel parcel = Parcel.obtain();
        category.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        ExploreGenre category = ExploreGenre.CREATOR.createFromParcel(parcel);
        assertThat(category).isEqualTo(ExploreGenre.POPULAR_AUDIO_CATEGORY);
    }

    @Test
    public void shouldGetSuggestedTracksPath(){
        category = new ExploreGenre("Title1");

        final Map<String, Link> links = new HashMap<>();
        final String href = "http://link1";
        links.put(ExploreGenre.SUGGESTED_TRACKS_LINK_REL, new Link(href));
        category.setLinks(links);

        assertThat(category.getSuggestedTracksPath()).isEqualTo(href);
    }
}
