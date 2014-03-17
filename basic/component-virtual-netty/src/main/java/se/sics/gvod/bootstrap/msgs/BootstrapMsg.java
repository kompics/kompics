package se.sics.gvod.bootstrap.msgs;

import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Map;
import java.util.Set;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.common.VodDescriptor;
import se.sics.gvod.common.msgs.DirectMsgNetty;
import se.sics.gvod.common.msgs.MessageEncodingException;
import se.sics.gvod.net.Transport;
import se.sics.gvod.net.VodMsgFrameDecoder;
import se.sics.gvod.net.msgs.RewriteableMsg;
import se.sics.gvod.net.util.UserTypesEncoderFactory;
import se.sics.gvod.timer.TimeoutId;

/**
 *
 * @author jdowling
 */
public class BootstrapMsg {

    public static final class GetPeersRequest extends DirectMsgNetty.Request {

        private static final long serialVersionUID = 787143338863400423L;
        private final int overlay;
        private final int utility;

        public GetPeersRequest(VodAddress source, VodAddress destination,
                int overlay, int utility) {
            super(source, destination);
            this.overlay = overlay;
            this.utility = utility;
        }
        
        public int getOverlay() {
            return overlay;
        }

        public int getUtility() {
            return utility;
        }

        @Override
        public int getSize() {
            return super.getHeaderSize()
                    + 4 /* overlay */
                    + 4 /* utility */;
        }

        @Override
        public ByteBuf toByteArray() throws MessageEncodingException {
            ByteBuf buf = createChannelBufferWithHeader();
            buf.writeInt(overlay);
            buf.writeInt(utility);
            return buf;
        }

        @Override
        public byte getOpcode() {
            return VodMsgFrameDecoder.BOOTSTRAP_REQUEST;
        }

        @Override
        public RewriteableMsg copy() {
            GetPeersRequest gpr = new GetPeersRequest(vodSrc, vodDest, overlay, utility);
            gpr.setTimeoutId(timeoutId);
            return gpr;
        }
    }

    public static final class GetPeersResponse extends DirectMsgNetty.Response {

        private static final long serialVersionUID = -3661778191727187359L;
        private final int overlayId;
        private final List<VodDescriptor> peers;

        public GetPeersResponse(VodAddress source,
                VodAddress destination, TimeoutId timeoutId, int overlayId, 
                List<VodDescriptor> peers) {
            super(source, destination, Transport.UDP, timeoutId);
            this.overlayId = overlayId;
            this.peers = peers;
        }

        public List<VodDescriptor> getPeers() {
            return peers;
        }

        public int getOverlayId() {
            return overlayId;
        }
        
        @Override
        public int getSize() {
            return super.getHeaderSize()
                    + 4 /* overlayId */
                    + UserTypesEncoderFactory.getListVodNodeDescriptorSize(peers);
        }

        @Override
        public ByteBuf toByteArray() throws MessageEncodingException {
            ByteBuf buffer = createChannelBufferWithHeader();
            buffer.writeInt(overlayId);
            UserTypesEncoderFactory.writeListVodNodeDescriptors(buffer, peers);
            return buffer;
        }

        @Override
        public byte getOpcode() {
            return VodMsgFrameDecoder.BOOTSTRAP_RESPONSE;
        }

        @Override
        public RewriteableMsg copy() {
            return new GetPeersResponse(vodSrc, vodDest, timeoutId, overlayId, peers);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(getVodSource()).append(" - ")
                    .append(getVodDestination()).append(" - ")
                    .append(overlayId).append(" - ")
                    .append(peers.size());
            sb.append("( ");
            for (VodDescriptor vd : peers) {
                sb.append(vd).append(" , ");
            }
            sb.append(" )");
            
            return sb.toString();
        }
        
        
    }
    
    public static final class Heartbeat extends DirectMsgNetty.Oneway {

        private static final long serialVersionUID = -6543349790434L;
        private final short mtu;
        private final Set<Integer> seeders;
        private final Map<Integer, Integer> downloaders;

        public Heartbeat(VodAddress source, VodAddress destination,
                short mtu, Set<Integer> seeders, Map<Integer, Integer> downloaders) {
            super(source, destination);
            this.seeders = seeders;
            this.downloaders = downloaders;
            this.mtu = mtu;
        }

        public short getMtu() {
            return mtu;
        }

        public Set<Integer> getSeeders() {
            return seeders;
        }

        public Map<Integer, Integer> getDownloaders() {
            return downloaders;
        }
        
        @Override
        public int getSize() {
            return super.getHeaderSize()
                    + 2 /* mtu */
                    + UserTypesEncoderFactory.getCollectionIntsLength(seeders)
                    + UserTypesEncoderFactory.getMapIntIntsLength(downloaders)
                    ;
        }

        @Override
        public ByteBuf toByteArray() throws MessageEncodingException {
            ByteBuf buf = createChannelBufferWithHeader();
            buf.writeShort(mtu);
            UserTypesEncoderFactory.writeCollectionInts(buf, seeders);
            UserTypesEncoderFactory.writeMapIntInts(buf, downloaders);
            return buf;
        }

        @Override
        public byte getOpcode() {
            return VodMsgFrameDecoder.BOOTSTRAP_HEARTBEAT;
        }

        @Override
        public RewriteableMsg copy() {
            // neither seeders or downloaders will be modified, so this is safe.
            Heartbeat hb = new Heartbeat(vodSrc, vodDest, mtu, seeders, downloaders);
            hb.setTimeoutId(timeoutId);
            return hb;
        }
    }

    
    public static final class AddOverlayReq extends DirectMsgNetty.Request {

        private static final long serialVersionUID = -876591849790434L;
        private final String overlayName;
        private final int overlayId;
        private final String description;
        private final byte[] torrentData;
        private final String imageUrl;
        private final int part;
        private final int numParts;

        public AddOverlayReq(VodAddress source, VodAddress destination,
                String overlayName, int overlayId, String description,
                byte[] torrentData, String imageUrl, int part, int numParts) {
            super(source, destination);
            assert(overlayName != null);
            this.overlayName = overlayName;
            this.overlayId = overlayId;
            if (description == null) {
                this.description = "";
            } else {
                this.description = description;
            }
            
            this.torrentData = torrentData;
            if (imageUrl == null) {
              this.imageUrl = "";
            } else {
                this.imageUrl = imageUrl;
            }
            this.part = part;
            this.numParts = numParts;
        }

        @Override
        public int getSize() {
            return super.getHeaderSize()
                    + UserTypesEncoderFactory.getStringLength256(overlayName)
                    + 4 /* overlayId */
                    + UserTypesEncoderFactory.getStringLength65356(description)
                    + 1000 /* guess of UserTypesEncoderFactory.getBytesLength65356(torrentData) */
                    + UserTypesEncoderFactory.getStringLength256(imageUrl)
                    + 1 
                    + 1;
        }

        public String getOverlayName() {
            return overlayName;
        }

        public int getOverlayId() {
            return overlayId;
        }

        public String getDescription() {
            return description == null ? "" : description;
        }

        public byte[] getTorrentData() {
            return torrentData;
        }

        
        public String getImageUrl() {
            return imageUrl == null ? "" : imageUrl;
        }

        public int getPart() {
            return part;
        }

        public int getNumParts() {
            return numParts;
        }
        
        
        @Override
        public ByteBuf toByteArray() throws MessageEncodingException {
            ByteBuf buf =  createChannelBufferWithHeader();
            UserTypesEncoderFactory.writeStringLength256(buf, overlayName);
            buf.writeInt(overlayId);
            UserTypesEncoderFactory.writeStringLength256(buf, description);
            UserTypesEncoderFactory.writeBytesLength65536(buf, torrentData);
            UserTypesEncoderFactory.writeStringLength256(buf, imageUrl);
            UserTypesEncoderFactory.writeUnsignedintAsOneByte(buf, part);
            UserTypesEncoderFactory.writeUnsignedintAsOneByte(buf, numParts);
            return buf;
        }

        @Override
        public byte getOpcode() {
            return VodMsgFrameDecoder.BOOTSTRAP_ADD_OVERLAY_REQUEST;
        }

        @Override
        public RewriteableMsg copy() {
            AddOverlayReq copy = new AddOverlayReq(vodSrc, vodDest, overlayName, 
                    overlayId, description, torrentData, imageUrl, part, numParts);
            copy.setTimeoutId(timeoutId);
            return copy;
        }
    }
    
    
    
    public static final class AddOverlayResp extends DirectMsgNetty.Response {

        private static final long serialVersionUID = -333221149790434L;
        private final int overlayId;
        private final boolean success;
        private final boolean finished;

        public AddOverlayResp(VodAddress source, VodAddress destination,
                int overlayId, boolean success, boolean finished, TimeoutId timeoutId) {
            super(source, destination, timeoutId);
            this.overlayId = overlayId;
            this.success = success;
            this.finished = finished;
        }

        @Override
        public int getSize() {
            return super.getHeaderSize()
                    + 4 /* overlayId */
                    + 1 /* success */
                    + 1 /* finished*/
                    ;
        }

        public boolean isSuccess() {
            return success;
        }

        public int getOverlayId() {
            return overlayId;
        }
        
        @Override
        public ByteBuf toByteArray() throws MessageEncodingException {
            ByteBuf buf = createChannelBufferWithHeader();
            buf.writeInt(overlayId);
            UserTypesEncoderFactory.writeBoolean(buf, success);
            UserTypesEncoderFactory.writeBoolean(buf, finished);
            return buf;
        }

        @Override
        public byte getOpcode() {
            return VodMsgFrameDecoder.BOOTSTRAP_ADD_OVERLAY_RESPONSE;
        }

        @Override
        public RewriteableMsg copy() {
            return new AddOverlayResp(vodSrc, vodDest, overlayId, success, finished, timeoutId);
        }
    }
    

    
    public static final class HelperHeartbeat extends DirectMsgNetty.Oneway {

        private static final long serialVersionUID = 433455863400423L;
        private final boolean space;
        
        public HelperHeartbeat(VodAddress source, VodAddress destination, boolean space) {
            super(source, destination);
            this.space = space;
        }
        
        @Override
        public int getSize() {
            return super.getHeaderSize()
                    + 1 // space
                    ;
        }

        @Override
        public ByteBuf toByteArray() throws MessageEncodingException {
            ByteBuf buf = createChannelBufferWithHeader();
            UserTypesEncoderFactory.writeBoolean(buf, space);
            return buf;
        }

        public boolean isSpace() {
            return space;
        }

        @Override
        public byte getOpcode() {
            return VodMsgFrameDecoder.BOOTSTRAP_CLOUD_HELPER_HB;
        }

        @Override
        public RewriteableMsg copy() {
            HelperHeartbeat gpr = new HelperHeartbeat(vodSrc, vodDest, space);
            return gpr;
        }
    }    
    
    
    
    public static final class HelperDownloadRequest extends DirectMsgNetty.Request {

        private static final long serialVersionUID = -333221149790434L;
        private final String url;

        public HelperDownloadRequest(VodAddress source, VodAddress destination, String url) {
            super(source, destination);
            if (url == null) {
                throw new NullPointerException("Url cannot be null for video to download");
            }
            this.url = url;
        }

        @Override
        public int getSize() {
            return super.getHeaderSize() 
                    + UserTypesEncoderFactory.getStringLength256(url)
                    ;
        }

        public String getUrl() {
            return url;
        }

        @Override
        public ByteBuf toByteArray() throws MessageEncodingException {
            ByteBuf buf = createChannelBufferWithHeader();
            UserTypesEncoderFactory.writeStringLength256(buf, url);
            return buf;
        }

        @Override
        public byte getOpcode() {
            return VodMsgFrameDecoder.BOOTSTRAP_CLOUD_HELPER_DOWNLOAD_REQUEST;
        }

        @Override
        public RewriteableMsg copy() {
            HelperDownloadRequest copy = new HelperDownloadRequest(vodSrc, vodDest, url);
            copy.setTimeoutId(timeoutId);
            return copy;
            
        }
    }    
    
    public static final class HelperDownloadResponse extends DirectMsgNetty.Response {

        private static final long serialVersionUID = -74747474149790434L;
        final boolean success;
        
        public HelperDownloadResponse(VodAddress source, VodAddress destination,
                boolean success, TimeoutId timeoutId) {
            super(source, destination, timeoutId);
            this.success = success;
        }

        @Override
        public int getSize() {
            return super.getHeaderSize()
                    + 1 /* success */
                    ;
        }

        public boolean isSuccess() {
            return success;
        }

        @Override
        public ByteBuf toByteArray() throws MessageEncodingException {
            ByteBuf buf = createChannelBufferWithHeader();
            UserTypesEncoderFactory.writeBoolean(buf, success);
            return buf;
        }

        @Override
        public byte getOpcode() {
            return VodMsgFrameDecoder.BOOTSTRAP_CLOUD_HELPER_DOWNLOAD_RESPONSE;
        }

        @Override
        public RewriteableMsg copy() {
            return new HelperDownloadResponse(vodSrc, vodDest, success, timeoutId);
        }
    }    
}
