package org.gnayils.downloader;

import java.io.Serializable;

public class Packet implements Serializable {

    public static final int CONNECT_MASTER = 0;
    public static final int DOWNLOAD_PARAMS = 1;
    public static final int DOWNLOADED_DATA = 2;
    public static final int DOWNLOAD_COMPLETED = 3;
    public static final int DOWNLOAD_ERROR = 4;

    public int type;
    public Object payload;

    public Packet() {
        this(-1, null);
    }

    public Packet(int type) {
        this(type, null);
    }

    public Packet(int type, Object payload) {
        this.type = type;
        this.payload = payload;
    }
}
