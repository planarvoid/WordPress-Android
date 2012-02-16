package com.soundcloud.android;

import com.soundcloud.android.model.User;
import com.soundcloud.android.tracking.Click;
import com.soundcloud.android.tracking.Page;
import com.soundcloud.api.Env;
import com.soundcloud.api.Token;

import android.accounts.Account;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestApplication extends SoundCloudApplication {
    public Account account;
    public final Map<String, String> accountData = new HashMap<String, String>();
    public final Token token;
    public final List<Page> trackedPages = new ArrayList<Page>();
    public final List<Click> trackedClicks = new ArrayList<Click>();

    public TestApplication() {
        this(new Token("access", null, Token.SCOPE_NON_EXPIRING));
    }

    public TestApplication(Token token) {
        this.token = token;
        mCloudApi = new Wrapper(null, "id", "secret", null, token, Env.LIVE);
    }


    @Override
    public Account getAccount() {
        return account;
    }

    @Override
    public Token useAccount(Account account) {
        this.account = account;
        return token;
    }

    @Override
    public boolean setAccountData(String key, String value) {
        return accountData.put(key, value) != null;
    }

    @Override
    public String getAccountData(String key) {
        return accountData.get(key);
    }

    public void setCurrentUserId(long id) {
        setAccountData(User.DataKeys.USER_ID, id);
    }

    @Override
    public void track(Page page, Object... args) {
        trackedPages.add(page);
    }

    @Override
    public void track(Click click, Object... args) {
        trackedClicks.add(click);
    }
}
