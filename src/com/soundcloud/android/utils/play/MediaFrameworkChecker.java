
package com.soundcloud.android.utils.play;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class MediaFrameworkChecker extends Thread {

    volatile int socketPort;

    private ServerSocket serverSocket;

    private Socket client;

    private Boolean isStagefright = false;

    public MediaFrameworkChecker() {
        try {
            serverSocket = new ServerSocket(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
        socketPort = serverSocket.getLocalPort();
    }

    @Override
    public void run() {
        if (serverSocket == null)
            return;
        try {
            client = serverSocket.accept();
            InputStream is = client.getInputStream();

            byte[] temp = new byte[2048];
            int bsize = -1;
            while (bsize <= 0) {
                bsize = is.read(temp);
            }

            String res = new String(temp, 0, bsize);
            if (res.indexOf("User-Agent: stagefright") >= 0) {
                isStagefright = true;
            }

            serverSocket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public Boolean isStagefright() {
        return isStagefright;
    }

    public int getSocketPort() {
        return socketPort;
    }

}
