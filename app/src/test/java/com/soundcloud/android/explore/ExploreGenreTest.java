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

    private ExploreGenre mCategory;

    @Test
    public void shouldBeParcelable() {
        mCategory = new ExploreGenre("Title1");

        final Map<String, Link> links = new HashMap<String, Link>();
        links.put("link1", new Link("http://link1"));
        links.put("link2", new Link("http://link2"));
        mCategory.setLinks(links);

        Parcel parcel = Parcel.obtain();
        mCategory.writeToParcel(parcel, 0);

        ExploreGenre category = ExploreGenre.CREATOR.createFromParcel(parcel);
        Expect.expect(category.getTitle()).toEqual(mCategory.getTitle());
        Expect.expect(category.getLinks().get("link1")).toEqual(mCategory.getLinks().get("link1"));
        Expect.expect(category.getLinks().get("link2")).toEqual(mCategory.getLinks().get("link2"));

    }

    @Test
    public void shouldParcelAndUnparcelIntoPopularMusicInstance() {
        mCategory = ExploreGenre.POPULAR_MUSIC_CATEGORY;
        Parcel parcel = Parcel.obtain();
        mCategory.writeToParcel(parcel, 0);

        ExploreGenre category = ExploreGenre.CREATOR.createFromParcel(parcel);
        Expect.expect(category).toBe(ExploreGenre.POPULAR_MUSIC_CATEGORY);

    }

    @Test
    public void shouldParcelAndUnparcelIntoPopularAudioInstance() {
        mCategory = ExploreGenre.POPULAR_AUDIO_CATEGORY;
        Parcel parcel = Parcel.obtain();
        mCategory.writeToParcel(parcel, 0);

        ExploreGenre category = ExploreGenre.CREATOR.createFromParcel(parcel);
        Expect.expect(category).toBe(ExploreGenre.POPULAR_AUDIO_CATEGORY);

    }

    @Test
    public void shouldGetSuggestedTracksPath(){
        mCategory = new ExploreGenre("Title1");

        final Map<String, Link> links = new HashMap<String, Link>();
        final String href = "http://link1";
        links.put(ExploreGenre.SUGGESTED_TRACKS_LINK_REL, new Link(href));
        mCategory.setLinks(links);

        Expect.expect(mCategory.getSuggestedTracksPath()).toBe(href);
    }
}
