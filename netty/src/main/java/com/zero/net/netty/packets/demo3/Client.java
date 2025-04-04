package com.zero.net.netty.packets.demo3;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * Netty粘包与拆包解决方案二
 * 固定数据包长度大小，如果数据包太小则用指定字符填充;
 * 接收端根据固定长度进行读取即可,可以使用Netty提供的
 *
 * 客户端
 * @author Zero.
 * @date 2023/8/15 9:19 AM
 */
public class Client {
    public static void main(String[] args) throws InterruptedException {
        NioEventLoopGroup worker = new NioEventLoopGroup();
        ChannelFuture channelFuture = new Bootstrap()
                .group(worker)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new ChannelInboundHandlerAdapter(){
                            @Override
                            public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                new Thread(()->{
                                    for (int i = 0; i < 100; i++) {
                                        // 向服务单写数据
                                        String message = "hello" + i;
                                        // 判断是否需要填充后发送
                                        ctx.writeAndFlush(Unpooled.copiedBuffer(fillBytes(message).getBytes()));
                                    }
                                }).start();
                            }
                        });
                    }
                }).connect("127.0.0.1", 9696).sync();
        channelFuture.channel().closeFuture().sync();
        worker.shutdownGracefully();
    }

    /**
     * 数据填充，如果长度不足20，则填充
     * @param message 要发送的数据
     */
    static String fillBytes(String message){
        var fillData = "x".repeat(20);
        if (message.length() >= 20){
            return message;
        }
        var ns = message + fillData.substring(message.length());
        System.out.println(ns);
        return ns;
    }
}
