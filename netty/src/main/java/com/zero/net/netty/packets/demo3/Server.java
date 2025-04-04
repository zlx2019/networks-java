package com.zero.net.netty.packets.demo3;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.FixedLengthFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.util.CharsetUtil;

/**
 * Netty粘包与拆包解决方案二
 * 固定数据包长度大小，如果数据包太小则用指定字符填充;
 * 接收端根据固定长度进行读取即可,可以使用Netty提供的FixedLengthFrameDecoder解码器
 *
 * 服务端
 *
 * @author Zero.
 * @date 2023/8/15 9:04 AM
 */
public class Server {
    public static void main(String[] args) throws InterruptedException {
        NioEventLoopGroup boss = new NioEventLoopGroup();
        NioEventLoopGroup worker = new NioEventLoopGroup();
        ChannelFuture channelFuture = new ServerBootstrap()
                .group(boss, worker)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline()
                                // 添加解码器,以固定字节长度进行解析，以每20个byte解析为一个ByteBuf
                                .addLast(new FixedLengthFrameDecoder(20))
                                // 将FixedLengthFrameDecoder解析后的ByteBuf解析为String类型
                                .addLast(new StringDecoder(CharsetUtil.UTF_8))
                                // 事件处理
                                .addLast(new SimpleChannelInboundHandler<String>() {
                                    @Override
                                    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
                                        System.out.println(msg);
                                    }
                                });
                    }
                }).bind(9696).sync();
        System.out.println("server running...");
        // 阻塞等待Channel关闭
        channelFuture.channel().closeFuture().sync();
        boss.shutdownGracefully();
        worker.shutdownGracefully();
    }
}
