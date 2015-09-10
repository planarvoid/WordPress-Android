package com.soundcloud.android.facebookapi;

import com.soundcloud.android.utils.ErrorUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import rx.Observable;
import rx.Subscriber;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FacebookApi {

    private final FacebookApiHelper facebookApiHelper;

    @Inject
    public FacebookApi(FacebookApiHelper facebookApiHelper) {
        this.facebookApiHelper = facebookApiHelper;
    }

    public Observable<List<String>> friendPictureUrls() {
        if (!facebookApiHelper.hasAccessToken()) {
            return Observable.just(Collections.<String>emptyList());
        }

        return Observable.create(new Observable.OnSubscribe<List<String>>() {
            @Override
            public void call(final Subscriber<? super List<String>> subscriber) {
                FacebookApiResponse response = facebookApiHelper.graphRequest(FacebookApiEndpoints.ME_FRIEND_PICTURES);
                List<String> pictureUrls = extractFriendPictureUrls(response);

                subscriber.onNext(pictureUrls);
                subscriber.onCompleted();
            }
        });
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
