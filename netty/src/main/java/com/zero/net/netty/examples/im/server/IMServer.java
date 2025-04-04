package com.zero.net.netty.examples.im.server;

import com.zero.net.netty.examples.im.packet.Codec;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * Netty IM Server
 *
 * @author Zero.
 * <p> Created on 2025/4/4 12:52 </p>
 */
public class IMServer {
    public static void main(String[] args) {
        NioEventLoopGroup boss = new NioEventLoopGroup(1);
        NioEventLoopGroup worker = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap()
                    .group(boss, worker)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<NioSocketChannel>() {
                        @Override
                        protected void initChannel(NioSocketChannel ch) throws Exception {
                            ch.pipeline()
                                    // 设置解码处理器(入站)，用于解码将读取到的数据解码为SendMessage对象
                                    .addLast(new Codec.SendMessageDecoder())
                                    // 设置事件处理器(入站)
                                    .addLast(new IMServerHandler())
                                    // 入站事件会按照处理器添加顺序执行
                                    // 先执行SendMessageDecoder解码，再执行ClientHandler处理器;

                                    // 设置编码处理器(出站)，用于将字节数组打包为自定义协议
                                    .addLast(new Codec.ProtobufPacker())
                                    // 设置编码处理器(出站)，用于将对象序列化为字节数组
                                    .addLast(new Codec.ProtobufEncoder());
                            // 出站逻辑会以相反的顺序进行执行
                            // 所以需要先执行ProtobufEncoder再执行ProtobufPacker
                        }
                    });
            // 异步启动服务
            ChannelFuture serverFuture = bootstrap.bind(19002).sync();
            // 阻塞等待服务关闭
            serverFuture.channel().closeFuture().sync();
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }
}
