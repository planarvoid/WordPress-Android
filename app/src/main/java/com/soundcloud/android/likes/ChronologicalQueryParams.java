package com.soundcloud.android.likes;

import com.soundcloud.android.commands.PagedQueryCommand;

public class ChronologicalQueryParams extends PagedQueryCommand.PageParams {

    private final long timestamp;

    public ChronologicalQueryParams(int limit, long timestamp) {
        super(limit);
        this.timestamp = timestamp;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
