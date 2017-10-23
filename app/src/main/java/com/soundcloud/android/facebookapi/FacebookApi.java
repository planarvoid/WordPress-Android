package com.soundcloud.android.facebookapi;

import static com.soundcloud.android.ApplicationModule.RX_HIGH_PRIORITY;

import com.soundcloud.android.utils.ErrorUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import io.reactivex.Observable;
import io.reactivex.Scheduler;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FacebookApi {

    private final FacebookApiHelper facebookApiHelper;
    private final Scheduler scheduler;

    @Inject
    public FacebookApi(FacebookApiHelper facebookApiHelper, @Named(RX_HIGH_PRIORITY) Scheduler scheduler) {
        this.facebookApiHelper = facebookApiHelper;
        this.scheduler = scheduler;
    }

    public Observable<List<String>> friendPictureUrls() {
        if (!facebookApiHelper.hasAccessToken()) {
            return Observable.just(Collections.<String>emptyList());
        }

        return Observable
                .fromCallable(() -> {
                    final FacebookApiResponse response = facebookApiHelper.graphRequest(FacebookApiEndpoints.ME_FRIEND_PICTURES);
                    return extractFriendPictureUrls(response);
                })
                .subscribeOn(scheduler);
    }

    private List<String> extractPictureUrls(JSONObject jsonResponse) {
        List<String> friendPictureUrls = new ArrayList<>();

        try {
            JSONArray jsonFriends = jsonResponse.getJSONArray("data");

            for (int i = 0; i < jsonFriends.length(); i++) {
                JSONObject picture = jsonFriends
                        .getJSONObject(i)
                        .getJSONObject("picture")
                        .getJSONObject("data");

                if (!picture.optBoolean("is_silhouette", false)) {
                    friendPictureUrls.add(picture.getString("url"));
                }
            }
        } catch (JSONException ex) {
            ErrorUtils.handleSilentException(ex);
        }

        return friendPictureUrls;
    }

    private List<String> extractFriendPictureUrls(FacebookApiResponse response) {
        if (response.isSuccess()) {
            return extractPictureUrls(response.getJSONObject());
        } else {
            return Collections.emptyList();
        }
    }

}
