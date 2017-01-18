package com.soundcloud.android.cast;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.Lists;

import java.util.List;

public class CastPlayQueue {

    private String revision;
    private List<RemoteTrack> queue;
    private int currentIndex;
    private String source = "";
    private String version = "1.0.0";
    private CastCredentials credentials;

    public CastPlayQueue() {
        /* For Deserialization */
    }

    public CastPlayQueue(Urn currentUrn, List<Urn> urns) {
        queue = Lists.transform(urns, RemoteTrack::new);
        currentIndex = urns.indexOf(currentUrn);
    }

    CastPlayQueue(String revision, Urn currentTrackUrn, List<Urn> tracks) {
        this(currentTrackUrn, tracks);
        this.revision = revision;
    }

    CastPlayQueue(CastPlayQueue original) {
        revision = original.revision;
        queue = original.queue;
        currentIndex = original.currentIndex;
        source = original.source;
        version = original.version;
        credentials = original.credentials;
    }

    public static CastPlayQueue forUpdate(Urn currentUrn, CastPlayQueue original) {
        CastPlayQueue castPlayQueue = new CastPlayQueue(original);
        castPlayQueue.currentIndex = original.getQueueUrns().indexOf(currentUrn);
        return castPlayQueue;
    }

    public String getRevision() {
        return revision;
    }

    public List<RemoteTrack> getQueue() {
        return queue;
    }

    @JsonProperty("current_index")
    public int getCurrentIndex() {
        return currentIndex;
    }

    public String getSource() {
        return source;
    }

    public String getVersion() {
        return version;
    }

    @JsonIgnore
    public Urn getCurrentTrackUrn() {
        int currentTrackIndex = getCurrentIndex();
        boolean isWithinRange = currentTrackIndex >= 0 && currentTrackIndex < getQueue().size();
        return isWithinRange ? new Urn(getQueue().get(currentTrackIndex).getUrn()) : Urn.NOT_SET;
    }

    @JsonIgnore
    public List<Urn> getQueueUrns() {
        return Lists.transform(queue, input -> new Urn(input.getUrn()));
    }

    @JsonIgnore
    public boolean contains(Urn urn) {
        return getQueueUrns().contains(urn);
    }

    @JsonIgnore
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    @JsonIgnore
    public boolean hasSameTracks(List<Urn> tracks) {
        return !isEmpty() && tracks != null && tracks.equals(getQueueUrns());
    }

    public void setCredentials(CastCredentials credentials) {
        this.credentials = credentials;
    }

    @JsonProperty("credentials")
    public CastCredentials getCredentials() {
        return credentials;
    }

    @Override
    public String toString() {
        return "CastPlayQueue{" +
                "revision='" + revision + '\'' +
                ", queue=" + queue +
                ", currentIndex=" + currentIndex +
                ", source='" + source + '\'' +
                ", version='" + version + '\'' +
                ", credentials=" + credentials +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CastPlayQueue that = (CastPlayQueue) o;

        if (currentIndex != that.currentIndex) return false;
        if (revision != null ? !revision.equals(that.revision) : that.revision != null) return false;
        if (queue != null ? !queue.equals(that.queue) : that.queue != null) return false;
        if (source != null ? !source.equals(that.source) : that.source != null) return false;
        return version != null ? version.equals(that.version) : that.version == null;
    }

    @Override
    public int hashCode() {
        int result = revision != null ? revision.hashCode() : 0;
        result = 31 * result + (queue != null ? queue.hashCode() : 0);
        result = 31 * result + currentIndex;
        result = 31 * result + (source != null ? source.hashCode() : 0);
        result = 31 * result + (version != null ? version.hashCode() : 0);
        return result;
    }
}
