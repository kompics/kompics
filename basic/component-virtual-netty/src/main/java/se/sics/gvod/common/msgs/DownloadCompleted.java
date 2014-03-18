package se.sics.gvod.common.msgs;

import io.netty.buffer.ByteBuf;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.net.VodMsgFrameDecoder;
import se.sics.gvod.net.msgs.RewriteableMsg;

/**
 * The <code>DownloadCompleted</code> class.
 *
 */
public final class DownloadCompleted extends DirectMsgNetty.Oneway {

    private final long downloadTime;

    public DownloadCompleted(VodAddress source, VodAddress destination,
            long downloadTime) {
        super(source, destination);
        this.downloadTime = downloadTime;
    }

    public long getDownloadTime() {
        return downloadTime;
    }

 
    @Override
    public int getSize() {
        return super.getHeaderSize()
                + 8 /* download time*/
                ;
    }

    @Override
    public byte getOpcode() {
        return VodMsgFrameDecoder.DOWNLOAD_COMPLETED;
    }

    @Override
    public ByteBuf toByteArray() throws MessageEncodingException {
        ByteBuf buffer = createChannelBufferWithHeader();
        buffer.writeLong(downloadTime);
        return buffer;
    }

    @Override
    public RewriteableMsg copy() {
        DownloadCompleted copy = new DownloadCompleted(vodSrc, vodDest, downloadTime);
        copy.setTimeoutId(timeoutId);
        return copy;
    }
}
