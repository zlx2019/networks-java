package com.zero.net.netty.packets.demo4;

import com.zero.net.netty.packets.demo5.pb.Protocol;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Netty粘包与拆包解决方案三
 * 使用Netty基于Protobuf数据序列化格式来进行通信
 * 直接使用内置的ProtobufVarint32FrameDecoder + ProtobufDecoder解码器来进行Protobuf的解码处理
 *
 * 服务端实现
 * @author Zero.
 * @date 2023/8/15 8:15 PM
 */
public class ServerUpgraded {
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
                                    // 使用Netty内置的Protobuf解码器
                                    // 优先读取前4个byte字节(int整数)，以读取到的int为后续要读取的长度来进行数据读取;
                                    .addLast(new ProtobufVarint32FrameDecoder())
                                    // 自动将解析出的数据，反序列化为PayloadRequest对象
                                    .addLast(new ProtobufDecoder(Protocol.PayloadRequest.getDefaultInstance()))
                                    // 事件处理器
                                    .addLast(new ChannelInboundHandlerAdapter(){
                                        @Override
                                        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                            // 这里直接强转为PayloadRequest即可
                                            var payload = (Protocol.PayloadRequest)msg;
                                            long id = payload.getId();
                                            String name = payload.getName();
                                            boolean active = payload.getIsActive();
                                            String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(payload.getSendTime()));
                                            System.out.println("[" + id + name + active + date + "]");
                                        }
                                    });
                        }
                    }).bind(9988).sync().addListener((ChannelFutureListener) channelFuture -> {
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
