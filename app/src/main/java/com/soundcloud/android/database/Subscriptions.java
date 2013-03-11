package com.soundcloud.android.database;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.act.Activities;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.task.AsyncApiTask;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.util.functions.Func1;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Subscriptions {

    public static Observable<Activities> getActivities() {
        return Observable.create(new Func1<Observer<Activities>, Subscription>() {
            @Override
            public Subscription call(Observer<Activities> observer) {

                new ActivitiesApiTask(SoundCloudApplication.instance, observer).execute();

                return new Subscription() {
                    @Override
                    public void unsubscribe() {
                        // handle cleanup, cancel HTTP request, disconnect Context?
                    }
                };
            }
        });
    }

    public static Observable<Playlist> getPlaylist() {
        return Observable.create(new Func1<Observer<Playlist>, Subscription>() {
            @Override
            public Subscription call(Observer<Playlist> observer) {

                observer.onNext(new Playlist(1));

                return Observable.noOpSubscription();
            }
        });
    }

    public static Observable<List<Track>> getPlaylistTracks() {
        return Observable.create(new Func1<Observer<List<Track>>, Subscription>() {
            @Override
            public Subscription call(Observer<List<Track>> observer) {

                ArrayList<Track> tracks = new ArrayList<Track>();
                tracks.add(new Track(1));
                tracks.add(new Track(2));
                tracks.add(new Track(3));

                observer.onNext(tracks);

                return Observable.noOpSubscription();
            }
        });
    }

    public static Observable<List<Track>> getPlaylistTracksLocal() {
        return Observable.create(new Func1<Observer<List<Track>>, Subscription>() {
            @Override
            public Subscription call(Observer<List<Track>> observer) {

                ArrayList<Track> tracks = new ArrayList<Track>();
                tracks.add(new Track(1));
                tracks.add(new Track(2));
                tracks.add(new Track(3));

                observer.onNext(tracks);

                return Observable.noOpSubscription();
            }
        });
    }


    private static final class ActivitiesApiTask extends AsyncApiTask<Void, Void, Activities> {

        private Observer<Activities> mObserver;
        private Exception error;

        private ActivitiesApiTask(AndroidCloudAPI api, Observer<Activities> observer) {
            super(api);
            mObserver = observer;
        }

        @Override
        protected Activities doInBackground(Void... params) {
            Request request = Content.ME_SOUND_STREAM.request();
            try {
                HttpResponse response = SoundCloudApplication.instance.get(request);
                return SoundCloudApplication.MODEL_MANAGER.getActivitiesFromJson(response.getEntity().getContent());
            } catch (IOException e) {
                e.printStackTrace();
                error = e;
            }

            return null;
        }

        @Override
        protected void onPostExecute(Activities activities) {
            if (activities != null) {
                mObserver.onNext(activities);
            } else {
                mObserver.onError(error);
            }
            mObserver.onCompleted();
        }
    }
}
