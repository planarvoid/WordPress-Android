package com.soundcloud.android.playlists;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.soundcloud.android.api.http.json.Views;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.model.Sharing;
import com.soundcloud.android.model.Track;
import com.soundcloud.api.Params;

import java.util.ArrayList;
import java.util.List;

@JsonRootName("playlist")
public class PlaylistApiCreateObject {

    @JsonView(Views.Full.class) String title;
    @JsonView(Views.Full.class) String sharing;
    @JsonView(Views.Full.class) public List<ScModel> tracks;
    public PlaylistApiCreateObject(Playlist p) {

        this.title = p.getTitle();
        this.sharing =  p.getSharing() == Sharing.PRIVATE ? Params.Track.PRIVATE : Params.Track.PUBLIC;

        // convert to ScModel as we only want to serialize the id
        this.tracks = new ArrayList<ScModel>();
        for (Track t : p.getTracks()) {
            tracks.add(new ScModel(t.getId()));
        }
    }

    public String toJson() throws JsonProcessingException {
        return new ObjectMapper().configure(SerializationFeature.WRAP_ROOT_VALUE, true).writeValueAsString(this);
    }
}
