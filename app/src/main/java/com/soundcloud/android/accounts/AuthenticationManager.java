package com.soundcloud.android.accounts;


import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;

import android.content.Context;

import java.io.IOException;

public class AuthenticationManager {

    public String getGoogleAccountToken(Context context, String accountName, String scope) throws GoogleAuthException, IOException {
        return GoogleAuthUtil.getToken(context, accountName, scope);
    }

    public void invalidateGoogleAccountToken(Context context, String token){
        GoogleAuthUtil.invalidateToken(context, token);
    }
}
