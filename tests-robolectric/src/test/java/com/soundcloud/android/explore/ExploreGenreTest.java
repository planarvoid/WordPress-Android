package com.soundcloud.android.explore;

import com.soundcloud.android.Expect;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.os.Parcel;

import java.util.HashMap;
import java.util.Map;

@RunWith(SoundCloudTestRunner.class)
public class ExploreGenreTest {

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

        ExploreGenre category = ExploreGenre.CREATOR.createFromParcel(parcel);
        Expect.expect(category.getTitle()).toEqual(this.category.getTitle());
        Expect.expect(category.getLinks().get("link1")).toEqual(this.category.getLinks().get("link1"));
        Expect.expect(category.getLinks().get("link2")).toEqual(this.category.getLinks().get("link2"));

    }

    @Test
    public void shouldParcelAndUnparcelIntoPopularMusicInstance() {
        category = ExploreGenre.POPULAR_MUSIC_CATEGORY;
        Parcel parcel = Parcel.obtain();
        category.writeToParcel(parcel, 0);

        ExploreGenre category = ExploreGenre.CREATOR.createFromParcel(parcel);
        Expect.expect(category).toBe(ExploreGenre.POPULAR_MUSIC_CATEGORY);

    }

    @Test
    public void shouldParcelAndUnparcelIntoPopularAudioInstance() {
        category = ExploreGenre.POPULAR_AUDIO_CATEGORY;
        Parcel parcel = Parcel.obtain();
        category.writeToParcel(parcel, 0);

        ExploreGenre category = ExploreGenre.CREATOR.createFromParcel(parcel);
        Expect.expect(category).toBe(ExploreGenre.POPULAR_AUDIO_CATEGORY);

    }

    @Test
    public void shouldGetSuggestedTracksPath(){
        category = new ExploreGenre("Title1");

        final Map<String, Link> links = new HashMap<>();
        final String href = "http://link1";
        links.put(ExploreGenre.SUGGESTED_TRACKS_LINK_REL, new Link(href));
        category.setLinks(links);

        Expect.expect(category.getSuggestedTracksPath()).toBe(href);
    }
}
