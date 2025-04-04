package com.zero.net.netty.packets.demo5;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * Netty粘包与拆包解决方案三
 * 使用Netty基于Protobuf数据序列化格式来进行通信，并且自定义解码编码器，解决粘包和拆包;
 *
 * 服务端实现
 *
 * @author Zero.
 * @date 2023/8/15 8:15 PM
 */
public class Server {
    public static void main(String[] args) {
        NioEventLoopGroup boss = new NioEventLoopGroup(1);
        NioEventLoopGroup worker = new NioEventLoopGroup();
        try {
            ChannelFuture future = new ServerBootstrap()
                    .group(boss, worker)
                    .channel(NioServerSocketChannel.class)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(new ChannelInitializer<NioSocketChannel>() {
                        @Override
                        protected void initChannel(NioSocketChannel ch) throws Exception {
                            ch.pipeline()
                                    // 设置自定义解码处理器
                                    // 将接收到的数据，根据自定义协议格式，解码为Protocol.PayloadRequest实体
                                    .addLast(new CustomDecoder())
                                    // 设置事件处理器
                                    .addLast(new ServerHandler());
                        }
                    }).bind(9987).sync().addListener((ChannelFutureListener) channelFuture -> {
                        if (channelFuture.isSuccess()){
                            System.out.println("server running successful...");
                        }
                    });
            future.channel().closeFuture().sync();
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }

    }
}
