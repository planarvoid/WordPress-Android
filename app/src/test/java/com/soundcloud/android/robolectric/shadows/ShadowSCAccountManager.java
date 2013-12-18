package com.soundcloud.android.robolectric.shadows;

import com.google.common.collect.Maps;
import com.xtremelabs.robolectric.internal.Implementation;
import com.xtremelabs.robolectric.internal.Implements;
import com.xtremelabs.robolectric.shadows.ShadowAccountManager;

import android.accounts.Account;
import android.accounts.AccountManager;

import java.util.Map;

@Implements(AccountManager.class)
public class ShadowSCAccountManager extends ShadowAccountManager{
    private Map<Account, Map<String,String>> data = Maps.newHashMap();

    @Implementation
    public void setUserData(Account account, String key, String value){
        if(!data.containsKey(account)){
            data.put(account, Maps.<String,String>newHashMap());
        }
        data.get(account).put(key,value);
    }

    @Implementation
    public String getUserData(Account account, String key){
        return data.get(account).get(key);
    }
}
