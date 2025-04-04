package com.zero.net.netty.packets.demo5;

import com.zero.net.netty.packets.demo5.pb.Protocol;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * 协议结构: 消息头部(9byte) + 数据体
 * 消息头部结构: 协议标识(1byte) + 数据体长度(8byte)
 *
 * 自定义解码器
 *
 * @author Zero.
 * @date 2023/8/15 9:18 PM
 */
public class CustomDecoder extends ByteToMessageDecoder {

    /**
     * 读取ByteBuf数据，反序列化为对象实体
     * @param ctx           上下文
     * @param in            要反序列化的缓冲区
     * @param out           反序列化后的实体列表,因为可能会反序列出多个对象，列表内有几个对象，就会调用处理器的channelRead()方法几次;
     * @throws Exception    抛出的异常
     */
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // 协议的数据头部占9个byte，如果可读取的数据小于9个byte，则表示数据有问题
        if (in.readableBytes() < 9){
            return;
        }
        // 读取消息头，获取数据体的长度
        long length = in.readLong();
        // 如果此时可读取的数据小于数据体长度，说明还没有获取到完整的数据，需要等待再次读取TCP发送过来的数据
        if (in.readableBytes() < length){
            // 由于我们已经读取了消息头部，所以需要将读取索引重置回上一次执行markReaderIndex()方式时的位置，表示下一次依然要读取消息头部
            in.resetReaderIndex();
            return;
        }
        // 读取数据体部分
        // 创建缓冲区
        byte[] buf = new byte[(int) length];
        // 读取数据体到缓冲区
        in.readBytes(buf);

        // 读取完成，更新读取索引位置，方便下一次读取或者重置回当前位置
        in.markReaderIndex();

        // 反序列为实体
        Protocol.PayloadRequest payloadRequest = Protocol.PayloadRequest.parseFrom(buf);
        out.add(payloadRequest);
    }
}
