package org.gnayils.downloader2.channel;

import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;

public interface Channel {

   void onKeyAvailable(SelectionKey key);

    SelectableChannel getRealChannel();
}
