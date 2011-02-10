package com.soundcloud.android.exception;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

public class SoundCloudAPIException extends Exception {
    private Exception root;

    public SoundCloudAPIException (Exception e) {
        super(e.getMessage(),e.getCause());
        root = e;
    }

    public Exception getRoot () {
        return root;
    }

// Exception ================================================================
    public String toString () {
        StringBuffer sb = new StringBuffer();

        sb.append("[");
        sb.append(this.getCause());
        sb.append("][");
        sb.append(getMessage());
        sb.append("]");
        sb.append("ROOT CAUSE:");

        Writer write = new StringWriter();
        PrintWriter pw = new PrintWriter(write);
        root.printStackTrace(pw);
        pw.close();
        try {
            write.close();
        } catch (IOException ioe) {/**/}
        sb.append(write);

        return sb.toString();
    }   
}