package com.zero.net.netty.examples.im.packet;

import com.google.protobuf.AbstractMessageLite;
import com.zero.net.netty.examples.im.pb.Protocol;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * @author Zero.
 * <p> Created on 2025/4/4 13:09 </p>
 */
public class Codec {

    /**
     * 自定义通用编码器器，将Protobuf对象序列化为字节数组，传递到{@link ProtobufPacker}处理器中;
     * 处理优先级必须高于{@link ProtobufPacker}
     */
    public static  class ProtobufEncoder extends ChannelOutboundHandlerAdapter {
        /**
         * @param ctx               上下文
         * @param msg               要写回的消息对象
         * @param promise           执行结果
         * @throws Exception        抛出的异常
         */
        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            if (msg instanceof AbstractMessageLite){
                byte[] bytes = ((AbstractMessageLite) msg).toByteArray();
                // 将序列化后的数据，传递给下一个处理器，进行报文打包处理;
                super.write(ctx,bytes,promise);
                //ctx.write(byteBuf,promise);
            }else {
                throw new RuntimeException("class type error.");
            }
        }
    }
    /**
     * 数据打包处理器，将消息体长度写到消息头部，解决粘包与拆包问题;
     * 报文格式: [消息体长度(8byte)][消息体]
     */
    public static class ProtobufPacker extends ChannelOutboundHandlerAdapter{
        /**
         * @param ctx           上下文
         * @param msg           {@link ProtobufEncoder} 处理器传递下来的消息体
         * @param promise       执行结果
         */
        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            // 消息体
            byte[] payload = (byte[]) msg;
            // 消息体长度
            int length = payload.length;
            // 定义缓冲区
            ByteBuf buf = Unpooled.buffer(8 + length);

            // 设置消息体长度
            buf.writeLong(length);
            // 设置消息体
            buf.writeBytes(payload);

            // 写入通道中
            ctx.write(buf,promise);
        }
    }



    /**
     * SendMessage解码器,用于解码客户端发送的数据
     */
    public static class SendMessageDecoder extends ByteToMessageDecoder {
        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
            // 无法获取消息头
            if (in.readableBytes() < 8){
                // 数据不足，等待更多数据
                return;
            }
            // 读取消息体长度
            long len = in.readLong();
            if (in.readableBytes() < len){
                // 数据不足，重置读取索引位置，等待更多数据
                in.resetReaderIndex();
                return;
            }
            byte[] buf = new byte[(int) len];
            // 读取消息体
            in.readBytes(buf);
            // 更新读取索引位置
            in.markReaderIndex();
            // 反序列为实体对象
            Protocol.SendMessage message = Protocol.SendMessage.parseFrom(buf);
            // 将对象放入out列表，作为处理器的channelRead方法中的参数;
            out.add(message);
        }
    }

    /**
     * ReplyMessageDecoder 解码器,用于解码服务端返回的数据
     */
    public static class ReplyMessageDecoder extends ByteToMessageDecoder{
        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
            // 无法获取消息头
            if (in.readableBytes() < 8){
                // 数据不足，等待更多数据
                return;
            }
            // 读取消息体长度
            long len = in.readLong();
            if (in.readableBytes() < len){
                // 数据不足，重置读取索引位置，等待更多数据
                in.resetReaderIndex();
                return;
            }
            byte[] buf = new byte[(int) len];
            // 读取消息体
            in.readBytes(buf);
            // 更新读取索引位置
            in.markReaderIndex();
            // 反序列为实体对象
            Protocol.ReplyMessage message = Protocol.ReplyMessage.parseFrom(buf);
            // 将对象放入out列表，作为处理器的channelRead方法中的参数;
            out.add(message);
        }
    }
}
