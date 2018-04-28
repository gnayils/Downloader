package org.gnayils.downloader2.channel;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SelectorThread implements Runnable {

    private boolean isRunning = false;

    private Thread thread;

    private Selector selector;
    private Map<SelectableChannel, Channel> channelMap = new HashMap<>();

    private Logger logger = Logger.getLogger(getClass().getName());

    private static SelectorThread instance;

    public SelectorThread() throws IOException {
        selector = Selector.open();
        thread = new Thread(this);
        thread.start();
    }

    public void register(Channel channel, int ops) throws ClosedChannelException {
        channel.getRealChannel().register(selector, ops);
        channelMap.put(channel.getRealChannel(), channel);
    }

    @Override
    public void run() {
        logger.log(Level.INFO, "selector thread start");
        isRunning = true;
        while(isRunning()) {
            try {
                selector.select();
            } catch (IOException e) {
                logger.log(Level.SEVERE, "selector select failed", e);
                isRunning = false;
                break;
            }
            Set<SelectionKey> keySet = selector.selectedKeys();
            Iterator<SelectionKey> iterator = keySet.iterator();
            while(iterator.hasNext()) {
                SelectionKey key = iterator.next();
                iterator.remove();
                logger.log(Level.FINEST, "dispatch key {0} to {1}", new Object[]{key.readyOps(), key.channel()});
                channelMap.get(key.channel()).onKeyAvailable(key);
            }
        }
        try {
            selector.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        isRunning = false;
        logger.log(Level.INFO, "selector thread stop");
    }

    private boolean isRunning() {
        return isRunning && ! Thread.currentThread().isInterrupted();
    }

    public void stop() {
        if(isRunning) {
            isRunning = false;
            thread.interrupt();
        }
    }

    public static SelectorThread getInstance() {
        if(instance == null) {
            synchronized (SelectorThread.class) {
                if(instance == null) {
                    try {
                        instance = new SelectorThread();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return instance;
    }

    public static void destroy() {
        if(instance != null) {
            instance.stop();
            instance = null;
        }
    }
}
