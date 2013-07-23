package com.soundcloud.android.tests;

public class TestUser {
    private final String username, email, password;

    public TestUser(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password= password;
    }

    public String getUsername() {
        return this.username;
    }

    public String getEmail() {
        return this.email;
    }

    public String getPassword() {
        return this.password;
    }


    public static final TestUser scAccount      = new TestUser("Steven Testowy",  "soundcloudtestuser@gmail.com", "s0undcl0ud");
    public static final TestUser scTestAccount  = new TestUser("android-testing", "",                             "android-testing");
    public static final TestUser noGPlusAccount = new TestUser("Steven Testowy",  "soundcloudtestuser@gmail.com", "s0undcl0ud");
    public static final TestUser GPlusAccount   = new TestUser("scandroidtest",   "sccloudandroid@gmail.com",     "s0undcl0ud");
    public static final TestUser Facebook       = new TestUser("Mike Smiechowy",  "ssmiechowy@gmail.com",         "passwordyeah");
}
