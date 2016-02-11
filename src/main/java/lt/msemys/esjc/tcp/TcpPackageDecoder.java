package lt.msemys.esjc.tcp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.util.List;

public class TcpPackageDecoder extends MessageToMessageDecoder<ByteBuf> {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        final TcpPackage tcpPackage;

        if (msg.hasArray()) {
            tcpPackage = TcpPackage.of(msg.array());
        } else {
            int length = msg.readableBytes();
            byte[] array = new byte[length];

            msg.getBytes(msg.readerIndex(), array, 0, length);

            tcpPackage = TcpPackage.of(array);
        }

        out.add(tcpPackage);
    }

}
