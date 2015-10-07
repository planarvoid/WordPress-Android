package com.soundcloud.android.events;

import com.soundcloud.android.model.Urn;
import org.jetbrains.annotations.NotNull;

public class OfflineSyncEvent extends TrackingEvent {

    public static final String KIND_SYNC = "sync";
    public static final String KIND_DESYNC = "desync";

    public static final String STAGE_START = "start";
    public static final String STAGE_FAIL = "fail";
    public static final String STAGE_COMPLETE = "complete";

    private Urn track;
    private Urn creator;
    private boolean inLikes;
    private boolean inPlaylist;
    private String stage;

    protected OfflineSyncEvent(@NotNull String kind) {
        super(kind, System.currentTimeMillis());
    }

    public static OfflineSyncEvent fromDesync(Urn trackUrn, Urn creatorUrn, boolean inLikes, boolean inPlaylist) {
        return new OfflineSyncEvent(KIND_DESYNC)
                .track(trackUrn)
                .creator(creatorUrn)
                .inLikes(inLikes)
                .inPlaylist(inPlaylist)
                .eventStage(STAGE_COMPLETE);
    }

    private OfflineSyncEvent eventStage(String stage) {
        this.stage = stage;
        return this;
    }

    private OfflineSyncEvent inPlaylist(boolean inPlaylist) {
        this.inPlaylist = inPlaylist;
        return this;
    }

    private OfflineSyncEvent track(Urn track) {
        this.track = track;
        return this;
    }

    private OfflineSyncEvent creator(Urn creator) {
        this.creator = creator;
        return this;
    }

    private OfflineSyncEvent inLikes(boolean inLikes) {
        this.inLikes = inLikes;
        return this;
    }

    public Urn getTrackUrn() {
        return track;
    }

    public Urn getTrackOwner() {
        return creator;
    }

    public boolean inPlaylist() {
        return inPlaylist;
    }

    public boolean inLikes() {
        return inLikes;
    }

    public String getStage() {
        return stage;
    }
}
