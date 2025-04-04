package com.zero.net.netty.examples.websocket;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;

/**
 * WebSocket服务端事件处理
 *
 * @author Zero.
 * @date 2023/8/16 1:20 PM
 */
public class ServerHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    /**
     * 读取事件处理
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
        // 解析消息
        String message = msg.text();
        System.out.println(message);
        // 响应消息
        TextWebSocketFrame frame = new TextWebSocketFrame(Unpooled.copiedBuffer("服务端已收到信息...".getBytes()));
        ctx.channel().writeAndFlush(frame);
    }

    /**
     * 建立WebSocket连接事件
     */
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        System.out.println("建立连接...");
        super.handlerAdded(ctx);
    }

    /**
     * 断开WebSocket连接事件
     */
    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        System.out.println("断开连接...");
        super.handlerRemoved(ctx);
    }

    /**
     * 用户自定义事件触发事件
     * @param evt 触发的事件
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete){
            System.out.println("握手成功...");
            // 握手成功事件
            WebSocketServerProtocolHandler.HandshakeComplete event = (WebSocketServerProtocolHandler.HandshakeComplete) evt;
            // 握手请求uri
            String uri = event.requestUri();
            // 握手请求头
            HttpHeaders headers = event.requestHeaders();
        }
        super.userEventTriggered(ctx,evt);
    }
}
