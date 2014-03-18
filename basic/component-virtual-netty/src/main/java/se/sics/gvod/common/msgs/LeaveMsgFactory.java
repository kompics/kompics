package se.sics.gvod.common.msgs;

import io.netty.buffer.ByteBuf;

public class LeaveMsgFactory extends DirectMsgNettyFactory.Oneway {

    private LeaveMsgFactory() {
    }

    public static LeaveMsg fromBuffer(ByteBuf buffer)
                
            throws MessageDecodingException {
        return (LeaveMsg) new LeaveMsgFactory().decode(buffer);
    }

    @Override
    protected LeaveMsg process(ByteBuf buffer) throws MessageDecodingException {

        return new LeaveMsg(vodSrc, vodDest);
    }
};
