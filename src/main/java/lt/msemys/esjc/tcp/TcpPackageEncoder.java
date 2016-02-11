package lt.msemys.esjc.tcp;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;

import static io.netty.buffer.Unpooled.wrappedBuffer;

public class TcpPackageEncoder extends MessageToMessageEncoder<TcpPackage> {

    @Override
    protected void encode(ChannelHandlerContext ctx, TcpPackage msg, List<Object> out) throws Exception {
        out.add(wrappedBuffer(msg.toByteArray()));
    }

}
