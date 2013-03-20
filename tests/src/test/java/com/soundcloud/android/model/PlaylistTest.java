package com.soundcloud.android.model;

import android.os.Parcel;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import static com.soundcloud.android.Expect.expect;

@RunWith(DefaultTestRunner.class)
public class PlaylistTest {
    @Test
    public void shouldDeserializePlaylist() throws Exception {
        Playlist p = TestHelper.getObjectMapper().readValue(
                getClass().getResourceAsStream("e1_playlist.json"),
                Playlist.class);

        expect(p.title).toEqual("PA600QT Demos");
        expect(p.description).toEqual("blah blah blah");
        expect(p.release_year).toEqual(1992);
        expect(p.user_id).toEqual(1196047l);
        expect(p.tracks.size()).toEqual(14);
    }

    @Test
    public void shouldParcelAndUnparcelCorrectly() throws Exception {
        Playlist playlist = TestHelper.getObjectMapper().readValue(
                getClass().getResourceAsStream("e1_playlist.json"),
                Playlist.class);

        Parcel p = Parcel.obtain();
        playlist.writeToParcel(p, 0);

        Playlist playlist2 = new Playlist(p);
        comparePlaylists(playlist, playlist2);
    }

    @Test
    public void shouldProvideCreateJson() throws Exception {
        Playlist playlist = TestHelper.getObjectMapper().readValue(
                getClass().getResourceAsStream("e1_playlist.json"),
                Playlist.class);

        //create mode
        Playlist.ApiCreateObject createObject = new Playlist.ApiCreateObject(playlist);
        expect(createObject.toJson(DefaultTestRunner.application.getMapper()))
                .toEqual("{\"playlist\":{\"title\":\"PA600QT Demos\",\"sharing\":\"public\",\"tracks\":[{\"id\":61363002},{\"id\":61363003},{\"id\":61363004},{\"id\":61363005},{\"id\":61363006},{\"id\":61363007},{\"id\":61363008},{\"id\":61363009},{\"id\":61363011},{\"id\":61363012},{\"id\":61363013},{\"id\":61363014},{\"id\":61363016},{\"id\":61363017}]}}");

    }

    @Test
    public void shouldProvideUpdateJson() throws Exception {
        Playlist playlist = TestHelper.getObjectMapper().readValue(
                getClass().getResourceAsStream("e1_playlist.json"),
                Playlist.class);

        List<Long> toAdd = new ArrayList<Long>();
        for (Track t : playlist.tracks){
            toAdd.add(t.id);
        }

        // update tracks mode
        Playlist.ApiUpdateObject updateObject = new Playlist.ApiUpdateObject(toAdd);
        expect(updateObject.toJson(DefaultTestRunner.application.getMapper()))
                .toEqual("{\"playlist\":{\"tracks\":[{\"id\":61363002},{\"id\":61363003},{\"id\":61363004},{\"id\":61363005},{\"id\":61363006},{\"id\":61363007},{\"id\":61363008},{\"id\":61363009},{\"id\":61363011},{\"id\":61363012},{\"id\":61363013},{\"id\":61363014},{\"id\":61363016},{\"id\":61363017}]}}");
    }

    private void comparePlaylists(Playlist playlist, Playlist playlist1) {
        expect(playlist1.id).toEqual(playlist.id);
        expect(playlist1.title).toEqual(playlist.title);
        expect(playlist1.permalink).toEqual(playlist.permalink);
        expect(playlist1.duration).toBeGreaterThan(0);
        expect(playlist1.duration).toEqual(playlist.duration);
        expect(playlist1.created_at).toEqual(playlist.created_at);
        expect(playlist1.tag_list).toEqual(playlist.tag_list);
        expect(playlist1.permalink_url).toEqual(playlist.permalink_url);
        expect(playlist1.artwork_url).toEqual(playlist.artwork_url);
        expect(playlist1.downloadable).toEqual(playlist.downloadable);
        expect(playlist1.streamable).toEqual(playlist.streamable);
        expect(playlist1.user_id).toEqual(playlist.user_id);
    }
}
