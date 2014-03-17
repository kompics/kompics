package se.sics.gvod.bootstrap.msgs;

import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;
import se.sics.gvod.common.msgs.MessageDecodingException;
import se.sics.gvod.common.msgs.DirectMsgNettyFactory;
import se.sics.gvod.net.msgs.DirectMsg;
import se.sics.gvod.net.util.UserTypesDecoderFactory;

public class MonitorMsgFactory extends DirectMsgNettyFactory.Oneway {

    private MonitorMsgFactory() {
    }

    public static MonitorMsg fromBuffer(ByteBuf buffer)
            throws MessageDecodingException {
        return (MonitorMsg) new MonitorMsgFactory().decode(buffer);
    }

    @Override
    protected MonitorMsg process(ByteBuf buffer) throws MessageDecodingException {
        int size = UserTypesDecoderFactory.readUnsignedIntAsOneByte(buffer);
        Map<String, String> attrs = new HashMap<String, String>();
        for (int i = 0; i < size; i++) {
            String key = UserTypesDecoderFactory.readStringLength256(buffer);
            String val = UserTypesDecoderFactory.readStringLength256(buffer);
            attrs.put(key, val);
        }
        return new MonitorMsg(vodSrc, vodDest, attrs);
    }
};
