package com.soundcloud.android.onboarding;

import com.google.common.collect.Lists;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Genre;
import com.soundcloud.android.model.GenreBucket;
import com.soundcloud.android.model.User;
import com.soundcloud.android.rx.schedulers.ScheduledOperations;
import com.soundcloud.android.utils.Log;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Func1;

import android.content.res.Resources;
import android.os.SystemClock;

import java.util.List;

public class OnboardingOperations extends ScheduledOperations {

    static class FakeApi {
        public List<GenreBucket> getGenreBuckets() {
            List<GenreBucket> buckets = Lists.newArrayList();
            // bunch of dummy objects for now, will do the API call later
            User dummyUser1 = new User();
            dummyUser1.username = "Skrillex";
            User dummyUser2 = new User();
            dummyUser2.username = "Justin Bieber";
            User dummyUser3 = new User();
            dummyUser3.username = "Forss";

            List<User> users = Lists.newArrayList(dummyUser1, dummyUser2, dummyUser3);
            Resources resources = SoundCloudApplication.instance.getResources();
            final String[] musicGenreKeys = resources.getStringArray(R.array.music_genre_keys);
            final String[] musicGenreNames = resources.getStringArray(R.array.music_genre_names);

            for (int i=0; i < musicGenreKeys.length; i++) {
                Genre genre = new Genre();
                genre.setPermalink(musicGenreKeys[i]);
                genre.setGrouping(Genre.Grouping.MUSIC);
                genre.setName(musicGenreNames[i]);

                GenreBucket bucket = new GenreBucket(genre);
                bucket.setUsers(users);

                buckets.add(bucket);

                SystemClock.sleep(250);
            }

            return buckets;
        }
    }

    private FakeApi mApi;

    public OnboardingOperations() {
        this(new FakeApi()); //TODO replace with actual API facade
    }

    //TODO replace with actual API facade
    public OnboardingOperations(FakeApi api) {
        this.mApi = api;
    }

    public Observable<GenreBucket> getGenreBuckets() {
        return schedule(Observable.create(new Func1<Observer<GenreBucket>, Subscription>() {
            @Override
            public Subscription call(Observer<GenreBucket> observer) {

                Log.d(OnboardingOperations.this, "fetching user buckets...");

                List<GenreBucket> buckets = mApi.getGenreBuckets();

                for (GenreBucket bucket : buckets) {
                    Log.d(OnboardingOperations.this, "observable: onNext " + bucket);
                    observer.onNext(bucket);
                }

                Log.d(OnboardingOperations.this, "observable: onCompleted");
                observer.onCompleted();

                return Subscriptions.empty();
            }
        }));
    }

}
