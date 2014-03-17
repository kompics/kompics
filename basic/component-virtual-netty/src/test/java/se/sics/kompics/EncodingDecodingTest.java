/*6
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.sics.kompics;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import se.sics.gvod.common.msgs.DataMsg;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.common.VodDescriptor;
import se.sics.gvod.config.BaseCommandLineConfig;
import se.sics.gvod.bootstrap.msgs.BootstrapMsg;
import se.sics.gvod.bootstrap.msgs.BootstrapMsgFactory;
import se.sics.gvod.common.msgs.DataMsgFactory;
import se.sics.gvod.common.msgs.DataOfferMsg;
import se.sics.gvod.common.msgs.DataOfferMsgFactory;
import se.sics.gvod.common.msgs.Encodable;
import se.sics.gvod.common.msgs.LeaveMsg;
import se.sics.gvod.common.msgs.LeaveMsgFactory;
import se.sics.gvod.common.msgs.MessageDecodingException;
import se.sics.gvod.common.msgs.MessageEncodingException;
import se.sics.gvod.gradient.msgs.SetsExchangeMsg;
import se.sics.gvod.gradient.msgs.SetsExchangeMsgFactory;
import se.sics.gvod.common.msgs.UploadingRateMsg;
import se.sics.gvod.common.msgs.UploadingRateMsgFactory;
import se.sics.gvod.net.Nat;
import se.sics.gvod.address.Address;
import se.sics.gvod.bootstrap.msgs.MonitorMsg;
import se.sics.gvod.bootstrap.msgs.MonitorMsgFactory;
import se.sics.gvod.common.UtilityVod;
import se.sics.gvod.config.VodConfig;
import se.sics.gvod.interas.msgs.InterAsGossipMsg;
import se.sics.gvod.interas.msgs.InterAsGossipMsgFactory;
import se.sics.gvod.common.hp.HPMechanism;
import se.sics.gvod.common.hp.HPRole;
import se.sics.gvod.common.msgs.DirectMsgNetty;
import se.sics.gvod.common.msgs.DirectMsgNettyFactory;
import se.sics.gvod.common.msgs.NatReportMsgFactory;
import se.sics.gvod.common.msgs.RelayMsgNetty;
import se.sics.gvod.net.VodMsgFrameDecoder;
import se.sics.gvod.net.msgs.NatMsg;
import se.sics.gvod.net.util.UserTypesDecoderFactory;
import se.sics.gvod.net.util.UserTypesEncoderFactory;
import se.sics.gvod.net.util.VideoTypesDecoderFactory;
import se.sics.gvod.net.util.VideoTypesEncoderFactory;
import se.sics.gvod.timer.TimeoutId;
import se.sics.gvod.timer.UUID;
import se.sics.gvod.video.msgs.*;

/**
 *
 * @author jdowling
 */
public class EncodingDecodingTest {

    private static Address src, dest;
    private static InetSocketAddress inetSrc, inetDest;
    private static VodAddress gSrc, gDest;
    private static int overlay = 120;
    private static UtilityVod utility = new UtilityVod(1, 12, 123);
    private static TimeoutId id = UUID.nextUUID();
    private static int age = 200;
    private static int freshness = 100;
    private static int remoteClientId = 12123454;
    private static HPMechanism hpMechanism = HPMechanism.PRP_PRC;
    private static HPRole hpRole = HPRole.PRC_RESPONDER;
    private static Nat nat;
    private static VodDescriptor nodeDescriptor;
    private static List<VodDescriptor> descriptors = new ArrayList<VodDescriptor>();
    private static byte[] availableChunks = new byte[2031];
    private static byte[][] availablePieces = new byte[52][19];

    public EncodingDecodingTest() {
        System.setProperty("java.net.preferIPv4Stack", "true");
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
//        InetAddress self = InetAddress.getLocalHost();
        InetAddress self = InetAddress.getByName("127.0.0.1");
        src = new Address(self, 58027, 123);
        dest = new Address(self, 65535, 123);
        inetSrc = new InetSocketAddress(self, 58027);
        inetDest = new InetSocketAddress(self, 65535);
        gSrc = new VodAddress(src, VodConfig.SYSTEM_OVERLAY_ID);
        gDest = new VodAddress(dest, VodConfig.SYSTEM_OVERLAY_ID);
        nodeDescriptor = new VodDescriptor(gSrc, utility,
                age, BaseCommandLineConfig.DEFAULT_MTU);
        descriptors.add(nodeDescriptor);
        nat = new Nat(Nat.Type.NAT,
                Nat.MappingPolicy.HOST_DEPENDENT,
                Nat.AllocationPolicy.PORT_PRESERVATION,
                Nat.FilteringPolicy.PORT_DEPENDENT,
                1,
                100 * 1000l);

        DirectMsgNettyFactory.Base.setMsgFrameDecoder(VodMsgFrameDecoder.class);

    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    @Test
    public void unsignedIntTwoBytesNetty() throws MessageEncodingException, MessageDecodingException {

        ByteBuf buffer = Unpooled.buffer(2);
        int t1 = 32231;
        UserTypesEncoderFactory.writeUnsignedintAsTwoBytes(buffer, t1);
        int t2 = UserTypesDecoderFactory.readUnsignedIntAsTwoBytes(buffer);
        assert (t1 == t2);
        t1 = 65535;
        UserTypesEncoderFactory.writeUnsignedintAsTwoBytes(buffer, t1);
        t2 = UserTypesDecoderFactory.readUnsignedIntAsTwoBytes(buffer);
        assert (t1 == t2);
    }

    @Test
    public void unsignedIntOneByteNetty() throws MessageEncodingException, MessageDecodingException {

        ByteBuf buffer = Unpooled.buffer(1);
        int t1 = 255;
        UserTypesEncoderFactory.writeUnsignedintAsOneByte(buffer, t1);
        int t2 = UserTypesDecoderFactory.readUnsignedIntAsOneByte(buffer);
        assert (t1 == t2);
        t1 = 0;
        UserTypesEncoderFactory.writeUnsignedintAsOneByte(buffer, t1);
        t2 = UserTypesDecoderFactory.readUnsignedIntAsOneByte(buffer);
        assert (t1 == t2);
        t1 = 1;
        UserTypesEncoderFactory.writeUnsignedintAsOneByte(buffer, t1);
        t2 = UserTypesDecoderFactory.readUnsignedIntAsOneByte(buffer);
        assert (t1 == t2);
    }

    @Test
    public void booleanNetty() throws MessageEncodingException, MessageDecodingException {
        boolean yes = true;
        ByteBuf buffer = Unpooled.buffer(1);
        UserTypesEncoderFactory.writeBoolean(buffer, yes);
        boolean id2 = UserTypesDecoderFactory.readBoolean(buffer);
        assert (yes == id2);
    }

    @Test
    public void stringNetty() throws MessageEncodingException, MessageDecodingException {
        String str = "Jim Dowling";
        ByteBuf buffer = Unpooled.buffer(str.length());
        UserTypesEncoderFactory.writeStringLength256(buffer, str);
        String str2 = UserTypesDecoderFactory.readStringLength256(buffer);
        assert (str.equals(str2));

        str = "Jim Dowling Jim Dowling Jim Dowling Jim Dowling Jim Dowling ";
        UserTypesEncoderFactory.writeStringLength256(buffer, str);
        str2 = UserTypesDecoderFactory.readStringLength256(buffer);
        assert (str.equals(str2));
    }

    private void opCodeCorrect(ByteBuf buffer, Encodable msg) {
        byte type = buffer.readByte();
        assert (type == msg.getOpcode());
    }

    @Test
    public void bootstrapRequest() {
        TimeoutId id = UUID.nextUUID();
        BootstrapMsg.GetPeersRequest msg = new BootstrapMsg.GetPeersRequest(gSrc, gDest,
                overlay, utility.getChunk());
        msg.setTimeoutId(id);
        try {
            ByteBuf buffer = msg.toByteArray();
            opCodeCorrect(buffer, msg);
            BootstrapMsg.GetPeersRequest res = BootstrapMsgFactory.GetPeersRequest.fromBuffer(buffer);
            assert (true);
        } catch (MessageDecodingException ex) {
            Logger.getLogger(EncodingDecodingTest.class.getName()).log(Level.SEVERE, null, ex);
            assert (false);
        } catch (MessageEncodingException ex) {
            Logger.getLogger(EncodingDecodingTest.class.getName()).log(Level.SEVERE, null, ex);
            assert (false);
        }
    }

    @Test
    public void bootstrapResponse() {
        TimeoutId id = UUID.nextUUID();
        BootstrapMsg.GetPeersResponse msg = new BootstrapMsg.GetPeersResponse(gSrc, gDest,
                id, 23, descriptors);
        try {
            ByteBuf buffer = msg.toByteArray();
            opCodeCorrect(buffer, msg);
            BootstrapMsg.GetPeersResponse res =
                    BootstrapMsgFactory.GetPeersResponse.fromBuffer(buffer);
            assert (true);
        } catch (MessageDecodingException ex) {
            Logger.getLogger(EncodingDecodingTest.class.getName()).log(Level.SEVERE, null, ex);
            assert (false);
        } catch (MessageEncodingException ex) {
            Logger.getLogger(EncodingDecodingTest.class.getName()).log(Level.SEVERE, null, ex);
            assert (false);
        }
    }

    @Test
    public void dataOffer() {
        List<VodAddress> listChildren = new ArrayList<VodAddress>();
        listChildren.add(gSrc);
        DataOfferMsg msg = new DataOfferMsg(gSrc, gDest, utility, availableChunks);
        try {
            ByteBuf buffer = msg.toByteArray();
            opCodeCorrect(buffer, msg);
            DataOfferMsg res = DataOfferMsgFactory.fromBuffer(buffer);
            assert (true);
        } catch (MessageDecodingException ex) {
            Logger.getLogger(EncodingDecodingTest.class.getName()).log(Level.SEVERE, null, ex);
            assert (false);
        } catch (MessageEncodingException ex) {
            Logger.getLogger(EncodingDecodingTest.class.getName()).log(Level.SEVERE, null, ex);
            assert (false);
        }
    }

    @Test
    public void leaveMsg() {
        LeaveMsg msg = new LeaveMsg(gSrc, gSrc);
        try {
            ByteBuf buffer = msg.toByteArray();
            opCodeCorrect(buffer, msg);
            LeaveMsg res = LeaveMsgFactory.fromBuffer(buffer);
            assert (true);
        } catch (MessageDecodingException ex) {
            Logger.getLogger(EncodingDecodingTest.class.getName()).log(Level.SEVERE, null, ex);
            assert (false);
        } catch (MessageEncodingException ex) {
            Logger.getLogger(EncodingDecodingTest.class.getName()).log(Level.SEVERE, null, ex);
            assert (false);
        }
    }

    @Test
    public void setsExchangeRequestMsg() {
        SetsExchangeMsg.Request msg = new SetsExchangeMsg.Request(
                gSrc, gSrc, gSrc.getId(), gSrc.getId(), id);
        try {
            ByteBuf buffer = msg.toByteArray();
            opCodeCorrect(buffer, msg);
            SetsExchangeMsg.Request res =
                    SetsExchangeMsgFactory.Request.fromBuffer(buffer);
            assert (true);
        } catch (MessageDecodingException ex) {
            Logger.getLogger(EncodingDecodingTest.class.getName()).log(Level.SEVERE, null, ex);
            assert (false);
        } catch (MessageEncodingException ex) {
            Logger.getLogger(EncodingDecodingTest.class.getName()).log(Level.SEVERE, null, ex);
            assert (false);
        }
    }

    @Test
    public void setsExchangeResponseMsg() {
        SetsExchangeMsg.Response msg = new SetsExchangeMsg.Response(
                gSrc, gSrc, gSrc.getId(), gSrc.getId(), gDest, id, descriptors, descriptors);
        try {
            ByteBuf buffer = msg.toByteArray();
            opCodeCorrect(buffer, msg);
            SetsExchangeMsg.Response res =
                    SetsExchangeMsgFactory.Response.fromBuffer(buffer);
            assert (true);
        } catch (MessageDecodingException ex) {
            Logger.getLogger(EncodingDecodingTest.class.getName()).log(Level.SEVERE, null, ex);
            assert (false);
        } catch (MessageEncodingException ex) {
            Logger.getLogger(EncodingDecodingTest.class.getName()).log(Level.SEVERE, null, ex);
            assert (false);
        }
    }

    @Test
    public void uploadingRateRequestMsg() {
        UploadingRateMsg.Request msg = new UploadingRateMsg.Request(
                gSrc, gDest, gSrc);
        msg.setTimeoutId(UUID.nextUUID());
        try {
            ByteBuf buffer = msg.toByteArray();
            opCodeCorrect(buffer, msg);
            UploadingRateMsg.Request res =
                    UploadingRateMsgFactory.Request.fromBuffer(buffer);
            compareNatMsgs(msg, res);
        } catch (MessageDecodingException ex) {
            Logger.getLogger(EncodingDecodingTest.class.getName()).log(Level.SEVERE, null, ex);
            assert (false);
        } catch (MessageEncodingException ex) {
            Logger.getLogger(EncodingDecodingTest.class.getName()).log(Level.SEVERE, null, ex);
            assert (false);
        }
    }

    @Test
    public void dataRequestMsg() {
        DataMsg.Request msg = new DataMsg.Request(gSrc, gSrc, id, 222, 12, 1000);
        // called by MsgRetryComponent
        msg.setTimeoutId(UUID.nextUUID());
        try {
            ByteBuf buffer = msg.toByteArray();
            opCodeCorrect(buffer, msg);
            DataMsg.Request res =
                    DataMsgFactory.Request.fromBuffer(buffer);
            assert (msg.getTimeoutId().equals(res.getTimeoutId()));
        } catch (MessageDecodingException ex) {
            Logger.getLogger(EncodingDecodingTest.class.getName()).log(Level.SEVERE, null, ex);
            assert (false);
        } catch (MessageEncodingException ex) {
            Logger.getLogger(EncodingDecodingTest.class.getName()).log(Level.SEVERE, null, ex);
            assert (false);
        }
    }

    @Test
    public void dataResponseMsg() {
        DataMsg.Response msg = new DataMsg.Response(gSrc, gSrc, id, id, availableChunks, 12, 2222,
                1000, 103);
        try {
            ByteBuf buffer = msg.toByteArray();
            opCodeCorrect(buffer, msg);
            DataMsg.Response res = DataMsgFactory.Response.fromBuffer(buffer);
            assert (true);
        } catch (MessageDecodingException ex) {
            Logger.getLogger(EncodingDecodingTest.class.getName()).log(Level.SEVERE, null, ex);
            assert (false);
        } catch (MessageEncodingException ex) {
            Logger.getLogger(EncodingDecodingTest.class.getName()).log(Level.SEVERE, null, ex);
            assert (false);
        }
    }

    @Test
    public void PieceNotAvailableMsg() {
        DataMsg.PieceNotAvailable msg = new DataMsg.PieceNotAvailable(gSrc, gSrc, availableChunks,
                utility, 1212, availablePieces);
        try {
            ByteBuf buffer = msg.toByteArray();
            opCodeCorrect(buffer, msg);
            DataMsg.PieceNotAvailable res = DataMsgFactory.PieceNotAvailable.fromBuffer(buffer);
            assert (true);
        } catch (MessageDecodingException ex) {
            Logger.getLogger(EncodingDecodingTest.class.getName()).log(Level.SEVERE, null, ex);
            assert (false);
        } catch (MessageEncodingException ex) {
            Logger.getLogger(EncodingDecodingTest.class.getName()).log(Level.SEVERE, null, ex);
            assert (false);
        }
    }

    @Test
    public void saturatedMsg() {
        DataMsg.Saturated msg = new DataMsg.Saturated(gSrc, gSrc, age, 23);
        try {
            ByteBuf buffer = msg.toByteArray();
            opCodeCorrect(buffer, msg);
            DataMsg.Saturated res = DataMsgFactory.Saturated.fromBuffer(buffer);
            assert (true);
        } catch (MessageDecodingException ex) {
            Logger.getLogger(EncodingDecodingTest.class.getName()).log(Level.SEVERE, null, ex);
            assert (false);
        } catch (MessageEncodingException ex) {
            Logger.getLogger(EncodingDecodingTest.class.getName()).log(Level.SEVERE, null, ex);
            assert (false);
        }
    }

    @Test
    public void hashRequestMsg() {
        DataMsg.HashRequest msg = new DataMsg.HashRequest(gSrc, gDest, 23);
        msg.setTimeoutId(UUID.nextUUID());
        try {
            ByteBuf buffer = msg.toByteArray();
            opCodeCorrect(buffer, msg);
            DataMsg.HashRequest res = DataMsgFactory.HashRequest.fromBuffer(buffer);
            compareNatMsgs(msg, res);
            assert (msg.getChunk() == res.getChunk());
            assert (msg.getPart() == res.getPart());
        } catch (MessageDecodingException ex) {
            Logger.getLogger(EncodingDecodingTest.class.getName()).log(Level.SEVERE, null, ex);
            assert (false);
        } catch (MessageEncodingException ex) {
            Logger.getLogger(EncodingDecodingTest.class.getName()).log(Level.SEVERE, null, ex);
            assert (false);
        }
    }

    @Test
    public void hashResponseMsg() {

        DataMsg.HashResponse msg = new DataMsg.HashResponse(gSrc, gSrc, id, 23,
                availableChunks, 0, 1);
        try {
            ByteBuf buffer = msg.toByteArray();
            opCodeCorrect(buffer, msg);
            DataMsg.HashRequest res = DataMsgFactory.HashRequest.fromBuffer(buffer);
            assert (true);
        } catch (MessageDecodingException ex) {
            Logger.getLogger(EncodingDecodingTest.class.getName()).log(Level.SEVERE, null, ex);
            assert (false);
        } catch (MessageEncodingException ex) {
            Logger.getLogger(EncodingDecodingTest.class.getName()).log(Level.SEVERE, null, ex);
            assert (false);
        }
    }

    @Test
    public void ackMsg() {
        DataMsg.Ack msg = new DataMsg.Ack(gSrc, gSrc, id, 23);
        try {
            ByteBuf buffer = msg.toByteArray();
            opCodeCorrect(buffer, msg);
            DataMsg.Ack res = DataMsgFactory.Ack.fromBuffer(buffer);
            assert (true);
        } catch (MessageDecodingException ex) {
            Logger.getLogger(EncodingDecodingTest.class.getName()).log(Level.SEVERE, null, ex);
            assert (false);
        } catch (MessageEncodingException ex) {
            Logger.getLogger(EncodingDecodingTest.class.getName()).log(Level.SEVERE, null, ex);
            assert (false);
        }
    }

    @Test
    public void addOverlayRequest() {
        BootstrapMsg.AddOverlayReq msg = new BootstrapMsg.AddOverlayReq(gSrc, gDest, "name", 12, "desc",
                new byte[]{'a', 'd'}, null, 0, 1);
        msg.setTimeoutId(UUID.nextUUID());
        try {
            ByteBuf buffer = msg.toByteArray();
            opCodeCorrect(buffer, msg);
            BootstrapMsg.AddOverlayReq res =
                    BootstrapMsgFactory.AddOverlayReq.fromBuffer(buffer);
            assert (true);
        } catch (MessageDecodingException ex) {
            Logger.getLogger(EncodingDecodingTest.class.getName()).log(Level.SEVERE, null, ex);
            assert (false);
        } catch (MessageEncodingException ex) {
            Logger.getLogger(EncodingDecodingTest.class.getName()).log(Level.SEVERE, null, ex);
            assert (false);
        }

    }

    @Test
    public void addOverlayResponse() {
        BootstrapMsg.AddOverlayResp msg = new BootstrapMsg.AddOverlayResp(gSrc, gDest,
                12, true, true, UUID.nextUUID());
        try {
            ByteBuf buffer = msg.toByteArray();
            opCodeCorrect(buffer, msg);
            BootstrapMsg.AddOverlayResp res =
                    BootstrapMsgFactory.AddOverlayResp.fromBuffer(buffer);
            assert (true);
        } catch (MessageDecodingException ex) {
            Logger.getLogger(EncodingDecodingTest.class.getName()).log(Level.SEVERE, null, ex);
            assert (false);
        } catch (MessageEncodingException ex) {
            Logger.getLogger(EncodingDecodingTest.class.getName()).log(Level.SEVERE, null, ex);
            assert (false);
        }

    }
    
    
   @Test
    public void helperHbMsgTest() {
        BootstrapMsg.HelperHeartbeat msg = new BootstrapMsg.HelperHeartbeat(gSrc, gSrc, true);
        try {
            ByteBuf buffer = msg.toByteArray();
            opCodeCorrect(buffer, msg);
            BootstrapMsg.HelperHeartbeat res =
                    BootstrapMsgFactory.HelperHeartbeat.fromBuffer(buffer);
            compareNatMsgs(msg, res);
            assert(msg.isSpace() == res.isSpace());
        } catch (MessageDecodingException ex) {
            Logger.getLogger(EncodingDecodingTest.class.getName()).log(Level.SEVERE, null, ex);
            assert (false);
        } catch (MessageEncodingException ex) {
            Logger.getLogger(EncodingDecodingTest.class.getName()).log(Level.SEVERE, null, ex);
            assert (false);
        }
    }
    
   @Test
    public void helperDownloadReqTest() {
        BootstrapMsg.HelperDownloadRequest msg = 
                new BootstrapMsg.HelperDownloadRequest(gSrc, gSrc, "gvod://myvideo/torrent.data");
        msg.setTimeoutId(UUID.nextUUID());
        try {
            ByteBuf buffer = msg.toByteArray();
            opCodeCorrect(buffer, msg);
            BootstrapMsg.HelperDownloadRequest res =
                    BootstrapMsgFactory.HelperDownloadRequest.fromBuffer(buffer);
            compareNatMsgs(msg, res);
            assert(msg.getUrl().compareTo(res.getUrl()) == 0);
        } catch (MessageDecodingException ex) {
            Logger.getLogger(EncodingDecodingTest.class.getName()).log(Level.SEVERE, null, ex);
            assert (false);
        } catch (MessageEncodingException ex) {
            Logger.getLogger(EncodingDecodingTest.class.getName()).log(Level.SEVERE, null, ex);
            assert (false);
        }
    }
    
   @Test
    public void helperDownloadRespTest() {
        BootstrapMsg.HelperDownloadResponse msg = 
                new BootstrapMsg.HelperDownloadResponse(gSrc, gSrc, true, UUID.nextUUID());
        try {
            ByteBuf buffer = msg.toByteArray();
            opCodeCorrect(buffer, msg);
            BootstrapMsg.HelperDownloadResponse res =
                    BootstrapMsgFactory.HelperDownloadResponse.fromBuffer(buffer);
            compareNatMsgs(msg, res);
            assert(msg.isSuccess() == res.isSuccess());
        } catch (MessageDecodingException ex) {
            Logger.getLogger(EncodingDecodingTest.class.getName()).log(Level.SEVERE, null, ex);
            assert (false);
        } catch (MessageEncodingException ex) {
            Logger.getLogger(EncodingDecodingTest.class.getName()).log(Level.SEVERE, null, ex);
            assert (false);
        }
    }
    
    

    @Test
    public void monitorMsg() {
        Map<String, String> attrs = new HashMap<String, String>();
        attrs.put("k", "v");
        attrs.put("y", "z");
        MonitorMsg msg = new MonitorMsg(gSrc, gDest, attrs);
        try {
            ByteBuf buffer = msg.toByteArray();
            opCodeCorrect(buffer, msg);
            MonitorMsg res = MonitorMsgFactory.fromBuffer(buffer);
            assert (res.getAttrValues().size() == attrs.size());
            assert (res.getAttrValues().keySet().iterator().next().equals(
                    attrs.keySet().iterator().next()));
        } catch (MessageDecodingException ex) {
            Logger.getLogger(EncodingDecodingTest.class.getName()).log(Level.SEVERE, null, ex);
            assert (false);
        } catch (MessageEncodingException ex) {
            Logger.getLogger(EncodingDecodingTest.class.getName()).log(Level.SEVERE, null, ex);
            assert (false);
        }
    }

    @Test
    public void interAsRequestMsg() {
        InterAsGossipMsg.Request msg = new InterAsGossipMsg.Request(
                gSrc, gSrc, id);
        try {
            ByteBuf buffer = msg.toByteArray();
            opCodeCorrect(buffer, msg);
            InterAsGossipMsg.Request req =
                    InterAsGossipMsgFactory.Request.fromBuffer(buffer);
            assert (true);
        } catch (MessageDecodingException ex) {
            Logger.getLogger(EncodingDecodingTest.class.getName()).log(Level.SEVERE, null, ex);
            assert (false);
        } catch (MessageEncodingException ex) {
            Logger.getLogger(EncodingDecodingTest.class.getName()).log(Level.SEVERE, null, ex);
            assert (false);
        }
    }

    @Test
    public void interAsResponseMsg() {
        InterAsGossipMsg.Response msg = new InterAsGossipMsg.Response(
                gSrc, gSrc, gDest, id, descriptors);
        try {
            ByteBuf buffer = msg.toByteArray();
            opCodeCorrect(buffer, msg);
            InterAsGossipMsg.Response res =
                    InterAsGossipMsgFactory.Response.fromBuffer(buffer);
            assert (true);
        } catch (MessageDecodingException ex) {
            Logger.getLogger(EncodingDecodingTest.class.getName()).log(Level.SEVERE, null, ex);
            assert (false);
        } catch (MessageEncodingException ex) {
            Logger.getLogger(EncodingDecodingTest.class.getName()).log(Level.SEVERE, null, ex);
            assert (false);
        }
    }

    @Test
    public void videoConnectionMsgRequest() {
        boolean randomRequest = false;
        VideoConnectionMsg.Request msg = new VideoConnectionMsg.Request(gSrc, gDest, randomRequest);
        msg.setTimeoutId(UUID.nextUUID());
        try {
            ByteBuf buffer = msg.toByteArray();
            opCodeCorrect(buffer, msg);
            VideoConnectionMsg.Request req =
                    VideoConnectionMsgFactory.Request.fromBuffer(buffer);
            assert (randomRequest == req.isRandomRequest());
            compareNatMsgs(msg, req);
        } catch (MessageDecodingException ex) {
            Logger.getLogger(EncodingDecodingTest.class.getName()).log(Level.SEVERE, null, ex);
            assert (false);
        } catch (MessageEncodingException ex) {
            Logger.getLogger(EncodingDecodingTest.class.getName()).log(Level.SEVERE, null, ex);
            assert (false);
        }
    }

    @Test
    public void videoConnectionMsgResponse() {
        boolean randomRequest = false;
        boolean acceptConnection = true;
        VideoConnectionMsg.Response msg = new VideoConnectionMsg.Response(gSrc, gDest,
                UUID.nextUUID(), randomRequest, acceptConnection);
        try {
            ByteBuf buffer = msg.toByteArray();
            opCodeCorrect(buffer, msg);
            VideoConnectionMsg.Response res =
                    VideoConnectionMsgFactory.Response.fromBuffer(buffer);
            assert (randomRequest == res.wasRandomRequest());
            assert (acceptConnection == res.connectionAccepted());
            assert (true);
        } catch (MessageDecodingException ex) {
            Logger.getLogger(EncodingDecodingTest.class.getName()).log(Level.SEVERE, null, ex);
            assert (false);
        } catch (MessageEncodingException ex) {
            Logger.getLogger(EncodingDecodingTest.class.getName()).log(Level.SEVERE, null, ex);
            assert (false);
        }
    }

    @Test
    public void longSet() {
        ByteBuf buffer = Unpooled.buffer(2);
        Set<Long> longs = new HashSet<Long>();
        longs.add(1L);

        Set<Long> processedLongs = null;

        try {
            UserTypesEncoderFactory.writeLongSet(buffer, longs);
            processedLongs = UserTypesDecoderFactory.readLongSet(buffer);

        } catch (MessageEncodingException ex) {
            Logger.getLogger(EncodingDecodingTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        assert (longs.size() == processedLongs.size());
        Iterator<Long> longsIt = longs.iterator();
        Iterator<Long> processedLongsIt = processedLongs.iterator();
        while (longsIt.hasNext() || processedLongsIt.hasNext()) {
            assert (longsIt.next() == processedLongsIt.next());
        }
    }

    @Test
    public void videoPieceMsgAdvertisement() {
        Set<Integer> pieceIds = new HashSet<Integer>();
        for (int i = 0; i < 100; i++) {
            pieceIds.add(i * 17);
        }
        VideoPieceMsg.Advertisement msg = new VideoPieceMsg.Advertisement(gSrc, gDest, pieceIds);
        try {
            ByteBuf buffer = msg.toByteArray();
            opCodeCorrect(buffer, msg);
            VideoPieceMsg.Advertisement adv =
                    VideoPieceMsgFactory.Advertisement.fromBuffer(buffer);
            assert (true);
            assert (pieceIds.size() == adv.getAdvertisedPiecesIds().size());
            assert (adv.getAdvertisedPiecesIds().containsAll(pieceIds));
        } catch (MessageDecodingException ex) {
            Logger.getLogger(EncodingDecodingTest.class.getName()).log(Level.SEVERE, null, ex);
            assert (false);
        } catch (MessageEncodingException ex) {
            Logger.getLogger(EncodingDecodingTest.class.getName()).log(Level.SEVERE, null, ex);
            assert (false);
        }
    }

    @Test
    public void videoPieceMsgRequest() {
        Set<Integer> pieceIds = new HashSet<Integer>();
        for (int i = 0; i < 100; i++) {
            pieceIds.add(i * 13);
        }
        VideoPieceMsg.Request msg = new VideoPieceMsg.Request(gSrc, gDest, pieceIds);
        msg.setTimeoutId(UUID.nextUUID());
        try {
            ByteBuf buffer = msg.toByteArray();
            opCodeCorrect(buffer, msg);
            VideoPieceMsg.Request request =
                    VideoPieceMsgFactory.Request.fromBuffer(buffer);
            compareNatMsgs(msg, request);
            assert (pieceIds.size() == request.getPiecesIds().size());
            assert (request.getPiecesIds().containsAll(pieceIds));
        } catch (MessageDecodingException ex) {
            Logger.getLogger(EncodingDecodingTest.class.getName()).log(Level.SEVERE, null, ex);
            assert (false);
        } catch (MessageEncodingException ex) {
            Logger.getLogger(EncodingDecodingTest.class.getName()).log(Level.SEVERE, null, ex);
            assert (false);
        }
    }

    @Test
    public void encodedSubPieceSet() {
        int globalIndex = 0;
        ByteBuf buffer = Unpooled.buffer(3 * 16 * 1024);
        Set<EncodedSubPiece> pieces = new HashSet<EncodedSubPiece>();
        Random random = new Random();
        for (int i = 0; i < 3; i++) {
            byte[] data = new byte[1316];
            random.nextBytes(data);
            EncodedSubPiece piece = new EncodedSubPiece(globalIndex++, i, data, 1);
            pieces.add(piece);
        }

        Set<EncodedSubPiece> processedPieces = null;
        try {
            VideoTypesEncoderFactory.writeEncodedSubPieceSet(buffer, pieces);
            processedPieces = VideoTypesDecoderFactory.readEncodedSubPieceSet(buffer);
        } catch (MessageEncodingException ex) {
            Logger.getLogger(EncodingDecodingTest.class.getName()).log(Level.SEVERE, null, ex);
        } catch (MessageDecodingException ex) {
            Logger.getLogger(EncodingDecodingTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        assert (pieces.size() == processedPieces.size());
        Map<Integer, EncodedSubPiece> processedPieceMap = new HashMap<Integer, EncodedSubPiece>();

        for (EncodedSubPiece p : processedPieces) {
            processedPieceMap.put(p.getEncodedIndex(), p);
        }

        for (EncodedSubPiece p : pieces) {
            EncodedSubPiece processedPiece = processedPieceMap.get(p.getEncodedIndex());
            assert (processedPiece != null);
            assert (p.getEncodedIndex() == processedPiece.getEncodedIndex());
            assert (p.getData().length == processedPiece.getData().length);
            for (int i = 0; i < p.getData().length; i++) {
                assert (p.getData()[i] == processedPiece.getData()[i]);
            }
        }
    }

    @Test
    public void videoPieceMsgResponse() {
        Random random = new Random();
        byte[] data = new byte[1316];
        random.nextBytes(data);
        EncodedSubPiece esp = new EncodedSubPiece(1, 1, data, 0);

        VideoPieceMsg.Response msg = new VideoPieceMsg.Response(gSrc, gDest, UUID.nextUUID(), esp);
        try {
            ByteBuf buffer = msg.toByteArray();
            opCodeCorrect(buffer, msg);
            VideoPieceMsg.Response response =
                    VideoPieceMsgFactory.Response.fromBuffer(buffer);
            assert (true);
            assert (response.getEncodedSubPiece().equals(esp));
        } catch (MessageDecodingException ex) {
            Logger.getLogger(EncodingDecodingTest.class.getName()).log(Level.SEVERE, null, ex);
            assert (false);
        } catch (MessageEncodingException ex) {
            Logger.getLogger(EncodingDecodingTest.class.getName()).log(Level.SEVERE, null, ex);
            assert (false);
        }
    }

    private void compareNatMsgs(NatMsg a, NatMsg b) {
        if (a instanceof DirectMsgNetty.Oneway == false
                && a instanceof RelayMsgNetty.Oneway == false
                && a instanceof DirectMsgNetty.SystemOneway == false) {
            assert (a.getTimeoutId().equals(b.getTimeoutId()));
        }
        assert (a.getVodSource().equals(b.getVodSource()));
        assert (a.getVodDestination().equals(b.getVodDestination()));
        // Note, we don't compare Address objects, as they have                                                                                                          
        // their ip and port set by the NettyHandler object.                                                                                                             
        // ip is null and port is 0 after the factory deserializes                                                                                                       
        // objects. It is up to NettyHandler to set ip and port.                                                                                                         
    }
}
