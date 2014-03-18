/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.sics.gvod.net;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.gvod.bootstrap.msgs.BootstrapMsgFactory;
import se.sics.gvod.common.msgs.DataMsgFactory;
import se.sics.gvod.common.msgs.DataOfferMsgFactory;
import se.sics.gvod.common.msgs.LeaveMsgFactory;
import se.sics.gvod.common.msgs.MessageDecodingException;
import se.sics.gvod.common.msgs.ReferencesMsgFactory;
import se.sics.gvod.common.msgs.UploadingRateMsgFactory;
import se.sics.gvod.interas.msgs.InterAsGossipMsgFactory;
import se.sics.gvod.net.msgs.RewriteableMsg;
import se.sics.gvod.video.msgs.VideoConnectionMsgFactory;
import se.sics.gvod.video.msgs.VideoPieceMsgFactory;

/**
 *
 * @author jdowling
 */
public class VodMsgFrameDecoder extends BaseMsgFrameDecoder {

    private static final Logger logger = LoggerFactory.getLogger(VodMsgFrameDecoder.class);
    public static final byte DATAOFFER = 0x05;
    public static final byte LEAVE = 0x06;
    public static final byte REFERENCES_REQUEST = 0x0b;
    public static final byte REFERENCES_RESPONSE = 0x0c;
    public static final byte UPLOADING_RATE_REQUEST = 0x0d;
    public static final byte UPLOADING_RATE_RESPONSE = 0x0e;
    public static final byte D_REQUEST = 0x0f;
    public static final byte D_RESPONSE = 0x10;
    public static final byte PIECE_NOT_AVAILABLE = 0x11;
    public static final byte SATURATED = 0x12;
    public static final byte ACK = 0x13;
    public static final byte HASH_REQUEST = 0x14;
    public static final byte HASH_RESPONSE = 0x15;
    public static final byte DOWNLOAD_COMPLETED = 0x5a;
    // BOOTSTRAP MSGS
    public static final byte BOOTSTRAP_REQUEST = 0x1c;
    public static final byte BOOTSTRAP_RESPONSE = 0x1d;
    public static final byte BOOTSTRAP_HEARTBEAT = 0x1e;
    public static final byte BOOTSTRAP_ADD_OVERLAY_REQUEST = 0x1f;
    public static final byte BOOTSTRAP_ADD_OVERLAY_RESPONSE = 0x09;
    public static final byte BOOTSTRAP_CLOUD_HELPER_HB = 0x59;
    public static final byte BOOTSTRAP_CLOUD_HELPER_DOWNLOAD_REQUEST = 0x5a;
    public static final byte BOOTSTRAP_CLOUD_HELPER_DOWNLOAD_RESPONSE = 0x5b;
    
    // MONITOR MSGS
    public static final byte MONITOR_MSG = 0x0a;
    // VIDEO MSGS
    public static final byte VIDEO_CONNECTION_REQUEST = 0x70;
    public static final byte VIDEO_CONNECTION_RESPONSE = 0x71;
    public static final byte VIDEO_CONNECTION_DISCONNECT = 0x72;
    public static final byte VIDEO_PIECES_ADVERTISEMENT = 0x73;
    public static final byte VIDEO_PIECES_REQUEST = 0x74;
    public static final byte VIDEO_PIECES_RESPONSE = 0x75;
    // INTER-AS
    public static final byte INTER_AS_GOSSIP_REQUEST = 0x76;
    public static final byte INTER_AS_GOSSIP_RESPONSE = 0x77;

    // NB: RANGE OF +VE BYTES ENDS AT 0x7F
    public VodMsgFrameDecoder() {
        super();
    }

    /**
     * Subclasses should call super() on their first line, and if a msg is
     * returned, then return, else test msgs in this class.
     *
     * @param ctx
     * @param channel
     * @param buffer
     * @return
     * @throws MessageDecodingException
     */
    @Override
    protected RewriteableMsg decodeMsg(ChannelHandlerContext ctx,
            ByteBuf buffer) throws MessageDecodingException {

        // See if msg is part of parent project, if yes then return it.
        // Otherwise decode the msg here.
        RewriteableMsg msg = super.decodeMsg(ctx, buffer);
        if (msg != null) {
            return msg;
        }

        switch (opKod) {
            case DATAOFFER:
                return DataOfferMsgFactory.fromBuffer(buffer);
            case LEAVE:
                return LeaveMsgFactory.fromBuffer(buffer);
            case REFERENCES_REQUEST:
                return ReferencesMsgFactory.Request.fromBuffer(buffer);
            case REFERENCES_RESPONSE:
                return ReferencesMsgFactory.Response.fromBuffer(buffer);
            case UPLOADING_RATE_REQUEST:
                return UploadingRateMsgFactory.Request.fromBuffer(buffer);
            case UPLOADING_RATE_RESPONSE:
                return UploadingRateMsgFactory.Response.fromBuffer(buffer);
            case D_REQUEST:
                return DataMsgFactory.Request.fromBuffer(buffer);
            case D_RESPONSE:
                return DataMsgFactory.Response.fromBuffer(buffer);
            case PIECE_NOT_AVAILABLE:
                return DataMsgFactory.PieceNotAvailable.fromBuffer(buffer);
            case SATURATED:
                return DataMsgFactory.Saturated.fromBuffer(buffer);
            case ACK:
                return DataMsgFactory.Ack.fromBuffer(buffer);
            case HASH_REQUEST:
                return DataMsgFactory.HashRequest.fromBuffer(buffer);
            case HASH_RESPONSE:
                return DataMsgFactory.HashResponse.fromBuffer(buffer);
            // BOOTSTRAP MSGS
            case BOOTSTRAP_REQUEST:
                return BootstrapMsgFactory.GetPeersRequest.fromBuffer(buffer);
            case BOOTSTRAP_RESPONSE:
                return BootstrapMsgFactory.GetPeersResponse.fromBuffer(buffer);
            case BOOTSTRAP_HEARTBEAT:
                return BootstrapMsgFactory.Heartbeat.fromBuffer(buffer);
            case BOOTSTRAP_ADD_OVERLAY_REQUEST:
                return BootstrapMsgFactory.AddOverlayReq.fromBuffer(buffer);
            case BOOTSTRAP_ADD_OVERLAY_RESPONSE:
                return BootstrapMsgFactory.AddOverlayResp.fromBuffer(buffer);
            case BOOTSTRAP_CLOUD_HELPER_HB:
                return BootstrapMsgFactory.HelperHeartbeat.fromBuffer(buffer);
            case BOOTSTRAP_CLOUD_HELPER_DOWNLOAD_REQUEST:
                return BootstrapMsgFactory.HelperDownloadRequest.fromBuffer(buffer);
            case BOOTSTRAP_CLOUD_HELPER_DOWNLOAD_RESPONSE:
                return BootstrapMsgFactory.HelperDownloadResponse.fromBuffer(buffer);
                
            // VIDEO MSGS
            case VIDEO_CONNECTION_REQUEST:
                return VideoConnectionMsgFactory.Request.fromBuffer(buffer);
            case VIDEO_CONNECTION_RESPONSE:
                return VideoConnectionMsgFactory.Response.fromBuffer(buffer);
            case VIDEO_CONNECTION_DISCONNECT:
                return VideoConnectionMsgFactory.Disconnect.fromBuffer(buffer);
            case VIDEO_PIECES_ADVERTISEMENT:
                return VideoPieceMsgFactory.Advertisement.fromBuffer(buffer);
            case VIDEO_PIECES_REQUEST:
                return VideoPieceMsgFactory.Request.fromBuffer(buffer);
            case VIDEO_PIECES_RESPONSE:
                return VideoPieceMsgFactory.Response.fromBuffer(buffer);
            // INTER-AS
            case INTER_AS_GOSSIP_REQUEST:
                return InterAsGossipMsgFactory.Request.fromBuffer(buffer);
            case INTER_AS_GOSSIP_RESPONSE:
                return InterAsGossipMsgFactory.Response.fromBuffer(buffer);
            default:
                break;
        }

        return null;
    }
}
