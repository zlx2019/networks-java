package com.zero.net.netty.examples.im.server;

import com.zero.net.netty.examples.im.pb.Protocol;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 事件处理器，处理来自于客户端连接的各种事件
 *
 * @author Zero.
 * <p> Created on 2025/4/4 13:12 </p>
 */
public class IMServerHandler extends ChannelInboundHandlerAdapter {
    // channel容器
    private DefaultChannelGroup channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    private ConcurrentHashMap<String, Channel> container = new ConcurrentHashMap<>(16);

    /**
     * 连接建立完成事件
     * @param ctx       连接上下文
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        container.put(ctx.channel().remoteAddress().toString(),ctx.channel());
        System.out.println(ctx.channel().remoteAddress().toString() + "已连接...");
        System.out.println("当前连接数: " + container.size());
        container.forEach((k,v)-> System.out.println(k));
        super.channelActive(ctx);
    }

    /**
     * 连接关闭事件
     *
     * @param ctx       连接上下文
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println(ctx.channel().remoteAddress().toString() + "关闭连接...");
        container.remove(ctx.channel().remoteAddress().toString());
        System.out.println("当前连接数: " + container.size());
        super.channelInactive(ctx);
    }

    /**
     * 连接读取事件
     *
     * @param ctx       连接上下文
     * @param msg       读取到的消息,解码处理器中处理后得出的消息
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof Protocol.SendMessage message){
            String name = message.getName();
            String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(message.getSendTime()));
            String payload = message.getPayload().toString("utf-8");
            System.out.printf("[%s] %s: %s \n",time,name,payload);

            // 广播消息
            Protocol.ReplyMessage replyMessage = Protocol.ReplyMessage.newBuilder()
                    .setSuccess(true)
                    .setReplyTime(System.currentTimeMillis())
                    .setSender(message).build();
            container.forEach((k,v)-> v.writeAndFlush(replyMessage));
//            channelGroup.writeAndFlush(replyMessage);
        }
    }
}
