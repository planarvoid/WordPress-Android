package com.soundcloud.android.likes;

import com.soundcloud.android.commands.PagedQueryCommand;
import com.soundcloud.android.playlists.LikedPlaylistMapper;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.query.Query;

import javax.inject.Inject;

public class LoadLikedPlaylistsCommand extends PagedQueryCommand<ChronologicalQueryParams> {

    @Inject
    LoadLikedPlaylistsCommand(PropellerDatabase database) {
        super(database, new LikedPlaylistMapper());
    }

    @Override
    protected Query buildQuery(ChronologicalQueryParams input) {
        return LoadLikedPlaylistCommand.playlistLikeQuery()
                .whereLt(Table.Likes + "." + TableColumns.Likes.CREATED_AT, input.getTimestamp())
                .order(Table.Likes + "." + TableColumns.Likes.CREATED_AT, Query.ORDER_DESC);
    }


}
