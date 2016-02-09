package lt.msemys.esjc.tcp;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static io.netty.buffer.Unpooled.wrappedBuffer;

public class TcpPackageEncoder extends MessageToMessageEncoder<TcpPackage> {

    private static final Logger logger = LoggerFactory.getLogger(TcpPackageEncoder.class);

    @Override
    protected void encode(ChannelHandlerContext ctx, TcpPackage msg, List<Object> out) throws Exception {
        if (logger.isTraceEnabled()) {
            logger.trace(msg.toString());
        }

        out.add(wrappedBuffer(msg.toByteArray()));
    }

}
