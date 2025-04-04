package com.zero.net.netty.packets.demo5;

import com.zero.net.netty.packets.demo5.pb.Protocol;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * 协议结构: 消息头部(8byte) + 数据体
 * 消息头部结构: 数据体长度(8byte)
 *
 * 自定义编码器
 * @author Zero.
 * @date 2023/8/15 9:19 PM
 */
public class CustomEncoder extends MessageToByteEncoder<Protocol.PayloadRequest> {

    /**
     * 将消息实体序列化为ByteBuf
     * @param ctx           上下文
     * @param msg           要序列化的对象
     * @param out           缓冲区
     * @throws Exception    抛出的异常
     */
    @Override
    protected void encode(ChannelHandlerContext ctx, Protocol.PayloadRequest msg, ByteBuf out) throws Exception {
        // 获取数据体
        byte[] data = msg.toByteArray();
        // 设置消息头部分(数据体长度)
        out.writeLong(data.length);
        // 设置数据体部分
        out.writeBytes(data);
    }
}
