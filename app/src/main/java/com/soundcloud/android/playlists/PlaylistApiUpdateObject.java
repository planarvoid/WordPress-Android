package com.soundcloud.android.playlists;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.soundcloud.android.api.legacy.json.Views;
import com.soundcloud.android.model.ScModel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@JsonRootName("playlist")
public class PlaylistApiUpdateObject {
    @JsonView(Views.Full.class)
    List<ScModel> tracks;

    public PlaylistApiUpdateObject(Collection<Long> toAdd) {
        this.tracks = new ArrayList<>(toAdd.size());
        for (Long id : toAdd){
            this.tracks.add(new ScModel(id));
        }
    }

    public String toJson() throws IOException {
        return new ObjectMapper().configure(SerializationFeature.WRAP_ROOT_VALUE, true).writeValueAsString(this);
    }
}
