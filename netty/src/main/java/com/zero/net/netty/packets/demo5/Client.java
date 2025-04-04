package com.zero.net.netty.packets.demo5;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * Netty粘包与拆包解决方案三
 * 使用Netty基于Protobuf数据序列化格式来进行通信，并且自定义解码编码器，解决粘包和拆包;
 * <p>
 * 客户端
 *
 * @author Zero.
 * @date 2023/8/15 9:19 AM
 */
public class Client {
    public static void main(String[] args) {
        NioEventLoopGroup worker = new NioEventLoopGroup();
        try {
            ChannelFuture channelFuture = new Bootstrap()
                    .group(worker)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<NioSocketChannel>() {
                        @Override
                        protected void initChannel(NioSocketChannel ch) throws Exception {
                            ch.pipeline()
                                    // 设置自定义编码处理器
                                    // 将Protocol.PayloadRequest对象根据自定义协议格式，写入到ByteBuf中
                                    .addLast(new CustomEncoder())
                                    // 设置事件处理器
                                    .addLast(new ClientHandler());
                        }
                    }).connect("127.0.0.1", 9987).sync()
                        .addListener((ChannelFutureListener) channelFuture1 -> {
                        if (channelFuture1.isSuccess()){
                            System.out.println("client running successful...");
                        }
                    });
            channelFuture.channel().closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            worker.shutdownGracefully();
        }
    }
}
