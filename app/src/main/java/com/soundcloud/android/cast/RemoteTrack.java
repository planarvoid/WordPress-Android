package com.soundcloud.android.cast;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.soundcloud.android.model.Urn;

class RemoteTrack {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String id;
    private Urn urn;

    public RemoteTrack() {
        /* For deserialization */
    }

    public RemoteTrack(Urn urn) {
        this.urn = urn;
    }

    public String getUrn() {
        return urn.toString();
    }

    public void setUrn(String urn) {
        this.urn = new Urn(urn);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "RemoteTrack{" +
                "id='" + id + '\'' +
                ", urn=" + urn +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RemoteTrack that = (RemoteTrack) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        return urn != null ? urn.equals(that.urn) : that.urn == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (urn != null ? urn.hashCode() : 0);
        return result;
    }
}
