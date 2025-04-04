package com.zero.net.netty.examples.im.client;

import com.zero.net.netty.examples.im.pb.Protocol;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 事件处理器，处理来自于服务端连接的各种事件
 *
 *
 * @author Zero.
 * <p> Created on 2025/4/4 13:13 </p>
 */
public class IMClientHandler extends ChannelInboundHandlerAdapter {


    /**
     * 与服务端连接建立完成事件
     *
     * @param ctx 连接上下文
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("与服务端已建立连接...");
        super.channelActive(ctx);
    }

    /**
     * 服务端连接关闭事件
     *
     * @param ctx 连接上下文
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("服务端已关闭...");
        super.channelInactive(ctx);
    }


    /**
     * 连接读取事件
     *
     * @param ctx 连接上下文
     * @param msg 读取到的消息,解码处理器中处理后得出的消息
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof Protocol.ReplyMessage replyMessage) {
            Protocol.SendMessage message = replyMessage.getSender();
            String name = message.getName();
            String time = new SimpleDateFormat("HH:mm:ss").format(new Date(message.getSendTime()));
            String payload = message.getPayload().toString("utf-8");
            System.out.printf("[%s] %s: %s \n", time, name, payload);
        }
    }
}
