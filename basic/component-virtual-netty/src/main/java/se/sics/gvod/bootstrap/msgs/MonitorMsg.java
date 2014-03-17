/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.sics.gvod.bootstrap.msgs;

import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import se.sics.gvod.common.msgs.DirectMsgNetty;
import se.sics.gvod.common.msgs.MessageEncodingException;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.net.VodMsgFrameDecoder;
import se.sics.gvod.net.msgs.RewriteableMsg;
import se.sics.gvod.net.util.UserTypesEncoderFactory;

/**
 *
 * @author Jim Dowling<jdowling@sics.se>
 */
public final class MonitorMsg extends DirectMsgNetty.Oneway {

    private static final long serialVersionUID = 787143338863400423L;
    private final Map<String, String> attrValues;

    public MonitorMsg(VodAddress source, VodAddress destination,
            Map<String, String> attrValues) {
        super(source, destination);
        assert (attrValues != null);
        this.attrValues = attrValues;
    }

    public Map<String, String> getAttrValues() {
        return attrValues;
    }

    @Override
    public int getSize() {
        return super.getHeaderSize()
                + attrValues.size() * 4 /*
                 * guess at size
                 */;
    }

    @Override
    public ByteBuf toByteArray() throws MessageEncodingException {
        ByteBuf buf = createChannelBufferWithHeader();
        UserTypesEncoderFactory.writeUnsignedintAsOneByte(buf, attrValues.size());
        for (Entry<String, String> pair : attrValues.entrySet()) {
            UserTypesEncoderFactory.writeStringLength256(buf, pair.getKey());
            UserTypesEncoderFactory.writeStringLength256(buf, pair.getValue());
        }
        return buf;
    }

    @Override
    public byte getOpcode() {
        return VodMsgFrameDecoder.MONITOR_MSG;
    }

    @Override
    public RewriteableMsg copy() {
        Map<String, String> attrs = new HashMap<String, String>();
        attrs.putAll(attrValues);
        return new MonitorMsg(vodSrc, vodDest, attrs);
    }
}