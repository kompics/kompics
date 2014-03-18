package se.sics.gvod.common.msgs;

import io.netty.buffer.ByteBuf;
import se.sics.gvod.common.UtilityVod;
import se.sics.gvod.net.util.UserTypesDecoderFactory;

public class DataOfferMsgFactory extends DirectMsgNettyFactory.Oneway {


    public static DataOfferMsg fromBuffer(ByteBuf buffer)
                
            throws MessageDecodingException {
        return (DataOfferMsg)
                new DataOfferMsgFactory().decode(buffer);
    }

    @Override
    protected DataOfferMsg process(ByteBuf buffer) throws MessageDecodingException {
        UtilityVod utility = (UtilityVod) UserTypesDecoderFactory.readUtility(buffer);
        byte[] chunks = UserTypesDecoderFactory.readArrayBytes(buffer);
        byte[][] availablePieces = UserTypesDecoderFactory.readArrayArrayBytes(buffer);
        return new DataOfferMsg(vodSrc, vodDest, utility, chunks,
                availablePieces);
    }
};
