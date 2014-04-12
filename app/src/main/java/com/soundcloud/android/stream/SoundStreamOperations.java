package com.soundcloud.android.stream;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.PropertySet;
import rx.Observable;
import rx.functions.Func1;

import android.os.Parcel;
import android.os.Parcelable;

import javax.inject.Inject;

class SoundStreamOperations {

    private final SoundStreamStorage soundStreamStorage;

    @Inject
    SoundStreamOperations(SoundStreamStorage soundStreamStorage) {
        this.soundStreamStorage = soundStreamStorage;
    }

    public Observable<StreamItemModel> getStreamItems() {
        final Urn currentUserUrn = Urn.forUser(123); // TODO
        return soundStreamStorage.loadStreamItemsAsync(currentUserUrn).map(new Func1<PropertySet, StreamItemModel>() {
            @Override
            public StreamItemModel call(PropertySet propertySet) {
                return new StreamItemModel(propertySet);
            }
        });
    }

    static final class StreamItemModel implements Parcelable {

        final Urn soundUrn;
        final String trackTitle;
        final boolean isRepost;

        private StreamItemModel(PropertySet propertySet) {
            this.soundUrn = Urn.parse(propertySet.get(StreamItemProperty.SOUND_URN));
            this.trackTitle = propertySet.get(StreamItemProperty.SOUND_TITLE);
            this.isRepost = propertySet.get(StreamItemProperty.REPOSTED);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {

        }
    }

}
