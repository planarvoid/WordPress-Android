package com.soundcloud.android.tests;

import java.util.HashMap;

public class UsersHelper {
    private HashMap<String, String> User;
    private final static HashMap<String, HashMap<String,String>> Users = new HashMap<String, HashMap<String, String>>();
    static {

        Users.put("scAccount",   new HashMap<String, String>(){
            {
              put("username",   "Steven Testowy");
              put("email",      "soundcloudtestuser@gmail.com");
              put("pass",       "s0undcl0ud");

            }
        });

        // Used for testing so far
        Users.put("scTestAccount",   new HashMap<String, String>(){
            {
              put("username",   "android-testing");
              put("email",      "");
              put("pass",       "android-testing");
            }
        });

        // No g+ account
        Users.put("noGPlusAccount",   new HashMap<String, String>(){
            {
                put("username",   "Steven Testowy");
                put("email",      "soundcloudtestuser@gmail.com");
                put("pass",       "s0undcl0ud");
            }
        });

        Users.put("GPlusAccount",   new HashMap<String, String>(){
            {
                put("username",   "scandroidtest");
                put("email",      "sccloudandroid@gmail.com");
                put("pass",       "s0undcl0ud");
            }
        });

    }

    public UsersHelper(String userType) {
        User = Users.get(userType);
    }

    public String login() {
        return User.get("username");
    }

    public String password() {
        return User.get("pass");
    }

    public String email() {
        return User.get("email");
    }
}
