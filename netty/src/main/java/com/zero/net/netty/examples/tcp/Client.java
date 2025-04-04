package com.zero.net.netty.examples.tcp;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.concurrent.TimeUnit;

/**
 * @author Zero.
 * <p> Created on 2025/4/4 12:45 </p>
 */
public class Client {
    public static void main(String[] args) throws InterruptedException {
        // 客户端只需要一个线程组
        NioEventLoopGroup works = new NioEventLoopGroup();
        // 客户端使用Bootstrap引导类
        Bootstrap app = new Bootstrap()
                .group(works)
                //客户端直接使用NioSocketChannel
                .channel(NioSocketChannel.class)
                // 设置channel事件处理
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new MyHandler());
                    }
                });
        // 连接Netty服务端
        ChannelFuture future = app.connect("127.0.0.1", 7979).sync()
                .addListener((ChannelFutureListener) channelFuture -> {
                    if (channelFuture.isSuccess()){
                        System.out.println("Netty Client Start Successful.");
                    }
                });

        // 阻塞连接服务端的channel关闭
        future.channel().closeFuture().sync();

        // 释放资源
        works.shutdownGracefully();
    }

    /**
     * Channel事件处理器
     */
    static class MyHandler extends ChannelInboundHandlerAdapter {

        /**
         * 读取服务端Channel数据
         */
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            // 读取数据
            ByteBuf buf = (ByteBuf) msg;
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            var message = new String(bytes);
            System.out.println(message);
        }

        /**
         *
         */
        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            System.out.println("服务端Channel关闭连接...");
        }

        /**
         * 当channel连接完成事件
         */
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            // 启动一个线程不停的向服务端写数据
            new Thread(()->{
                while (true){
                    if (!ctx.channel().isOpen()){
                        System.out.println("server channel is close...");
                        return;
                    }
                    if (ctx.channel().isOpen()){
                        ctx.writeAndFlush(Unpooled.copiedBuffer("Hello".getBytes()));
                    }
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }).start();
        }
    }
}
