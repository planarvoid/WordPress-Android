package com.soundcloud.android.framework.helpers.networkmanager;


import android.os.Message;

public class Response {
    private final int id;
    private final String response;
    private final int what;

    private final String command;

    public static final Response EMPTY = new Response(Message.obtain());

    public Response(Message msg) {
        id = msg.getData().getInt("Id");
        response = msg.getData().getString("Response");
        what = msg.what;
        command = msg.getData().getString("Command");
    }

    public int getId() {
        return id;
    }

    public String getCommand() {
        return command;
    }

    public String getResponse() {
        return response;
    }

    public int getWhat() {
        return what;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) return false;
        if (other == this) return true;
        if (!(other instanceof Response)) return false;

        Response otherMyClass = (Response)other;
        if(otherMyClass.id == id) return true;

        return false;
    }
}
