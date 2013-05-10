package com.soundcloud.android.accounts;


import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;

import android.content.Context;
import android.os.Bundle;

import java.io.IOException;

public class AuthenticationManager {

    public String getGoogleAccountToken(Context context, String accountName, String scope) throws GoogleAuthException, IOException {
        Bundle bundle = new Bundle();
        bundle.putString(GoogleAuthUtil.KEY_REQUEST_VISIBLE_ACTIVITIES,
                "http://schemas.google.com/AddActivity http://schemas.google.com/CreateActivity http://schemas.google.com/ListenActivity");
        return GoogleAuthUtil.getToken(context, accountName, scope, bundle);
    }

    public void invalidateGoogleAccountToken(Context context, String token){
        GoogleAuthUtil.invalidateToken(context, token);
    }
}
