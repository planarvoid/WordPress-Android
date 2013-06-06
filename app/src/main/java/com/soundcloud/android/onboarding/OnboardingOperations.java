package com.soundcloud.android.onboarding;

import com.google.common.collect.Lists;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Category;
import com.soundcloud.android.model.CategoryGroup;
import com.soundcloud.android.model.SuggestedUser;
import com.soundcloud.android.rx.schedulers.ScheduledOperations;
import com.soundcloud.android.utils.Log;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Func1;

import android.content.res.Resources;
import android.os.SystemClock;

import java.util.ArrayList;
import java.util.List;

public class OnboardingOperations extends ScheduledOperations {

    static class FakeApi {
        public List<CategoryGroup> getCategoryGroups() {
            List<CategoryGroup> buckets = Lists.newArrayList();
            CategoryGroup facebook = new CategoryGroup(CategoryGroup.URN_FACEBOOK);
            buckets.add(facebook);

            CategoryGroup music = new CategoryGroup();
            music.setCategories(new ArrayList<Category>());
            music.setUrn(CategoryGroup.URN_MUSIC);

            Resources resources = SoundCloudApplication.instance.getResources();
            final String[] musicGenreKeys = resources.getStringArray(R.array.music_genre_keys);
            final String[] musicGenreNames = resources.getStringArray(R.array.music_genre_names);

            for (int i=0; i < musicGenreKeys.length; i++) {
                Category genre = new Category();
                genre.setName(musicGenreNames[i]);
                genre.setPermalink(musicGenreKeys[i]);
                genre.setUsers(getUsers(musicGenreKeys[i], 3));
                music.getCategories().add(genre);
                SystemClock.sleep(50);
            }
            buckets.add(music);


            CategoryGroup audio = new CategoryGroup();
            audio.setCategories(new ArrayList<Category>());
            audio.setUrn(CategoryGroup.URN_SPEECH_AND_SOUNDS);

            final String[] audioGenreKeys = resources.getStringArray(R.array.audio_genre_keys);
            final String[] audioGenreNames = resources.getStringArray(R.array.audio_genre_names);

            for (int i=0; i < audioGenreKeys.length; i++) {
                Category genre = new Category();
                genre.setName(audioGenreNames[i]);
                genre.setPermalink(audioGenreKeys[i]);
                genre.setUsers(getUsers(audioGenreKeys[i], 4));
                audio.getCategories().add(genre);
                SystemClock.sleep(50);
            }
            buckets.add(audio);
            return buckets;
        }

        private List<SuggestedUser> getUsers(String prefix, int count) {
            List<SuggestedUser> users = new ArrayList<SuggestedUser>(count);
            for (int i = 0; i < count; i++) {
                final long l = System.currentTimeMillis() + i;
                SuggestedUser user = new SuggestedUser("soundcloud:users:" + l);
                user.setUsername("u_" + prefix + "_" + i);
                users.add(user);
            }
            return users;
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

    public Observable<CategoryGroup> getCategoryGroups() {
        return schedule(Observable.create(new Func1<Observer<CategoryGroup>, Subscription>() {
            @Override
            public Subscription call(Observer<CategoryGroup> observer) {

                Log.d(OnboardingOperations.this, "fetching user buckets...");

                List<CategoryGroup> buckets = mApi.getCategoryGroups();

                for (CategoryGroup bucket : buckets) {
                    Log.d(OnboardingOperations.this, "observable: onNext " + bucket);
                    SystemClock.sleep(100);
                    observer.onNext(bucket);
                }

                Log.d(OnboardingOperations.this, "observable: onCompleted");
                observer.onCompleted();

                return Subscriptions.empty();
            }
        }));
    }

}
