package com.soundcloud.android.accounts;


import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;

import android.content.Context;
import android.os.Bundle;

import java.io.IOException;

public class AuthenticationManager {
    public String getGoogleAccountToken(Context context, String accountName, String scope, Bundle bundle) throws GoogleAuthException, IOException {
        return GoogleAuthUtil.getToken(context, accountName, scope, bundle);
    }

    public void invalidateGoogleAccountToken(Context context, String token){
        GoogleAuthUtil.invalidateToken(context, token);
    }
}
