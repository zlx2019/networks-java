package com.zero.net.netty.packets.demo2;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.util.CharsetUtil;

/**
 * Netty粘包与拆包解决方案一
 * 加标记,使用特定符号作为数据包的结尾标识，如`%`
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
        // 创建Server Channel
        ChannelFuture channelFuture = new ServerBootstrap()
                .group(boss, worker)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline()
                                // 添加解码器，如使用%作为结尾标识符
                                .addLast(new DelimiterBasedFrameDecoder(1024, Unpooled.wrappedBuffer(new byte[]{'%'})))
                                // 将解析完的ByteBuf转换为String类型
                                .addLast(new StringDecoder(CharsetUtil.UTF_8))
                                // 添加事件处理器，这里直接使用SimpleChannelInboundHandler处理器，将数据包类型设置为String即可
                                // 因为经过了StringDecoder的处理，所以这里可以直接使用String类型作为参数
                                .addLast(new SimpleChannelInboundHandler<String>() {
                                    @Override
                                    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
                                        var message = "client send data: [" + msg + "]";
                                        System.out.println(message);

                                        // 将消息追加一个%，转为大写写回客户端
                                        String recvMsg = msg.concat("%").toUpperCase();
                                        // 将消息写回客户端
                                        ctx.channel().writeAndFlush(Unpooled.copiedBuffer(recvMsg.getBytes()));
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
