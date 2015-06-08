package com.soundcloud.android.profile;

import rx.Observable;

interface ProfileUserProvider {

    Observable<ProfileUser> user();

    Observable<ProfileUser> refreshUser();
}
