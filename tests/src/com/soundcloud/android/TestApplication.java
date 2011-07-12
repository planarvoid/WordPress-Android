package com.soundcloud.android;

import com.soundcloud.api.Env;

import android.accounts.Account;

import java.util.HashMap;
import java.util.Map;

public class TestApplication extends SoundCloudApplication {
    public Account account;
    public Map<String, String> accountData = new HashMap<String, String>();

    public TestApplication() {
        mCloudApi = new Wrapper(null, "id", "secret", null, null, Env.LIVE);
    }

    @Override
    public Account getAccount() {
        return account;
    }

    @Override
    public void useAccount(Account account) {
        this.account = account;
    }

    @Override
    public boolean setAccountData(String key, String value) {
        return accountData.put(key, value) != null;
    }

    @Override
    public String getAccountData(String key) {
        return accountData.get(key);
    }
}
