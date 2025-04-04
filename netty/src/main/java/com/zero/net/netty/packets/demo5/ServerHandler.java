package com.zero.net.netty.packets.demo5;

import com.zero.net.netty.packets.demo5.pb.Protocol;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 服务端事件处理器
 *
 * @author Zero.
 * @date 2023/8/15 9:16 PM
 */
public class ServerHandler extends ChannelInboundHandlerAdapter {


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        var payload = (Protocol.PayloadRequest)msg;
        long id = payload.getId();
        String name = payload.getName();
        boolean active = payload.getIsActive();
        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(payload.getSendTime()));
        System.out.println("[" + id + name + active + date + "]");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        super.exceptionCaught(ctx, cause);
    }
}
