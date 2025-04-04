package com.zero.net.netty.packets.demo1;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.util.CharsetUtil;

/**
 * Netty粘包与拆包解决方案一
 * 加标记,使用LineBasedFrameDecoder解码处理器，该处理器会对数据包根据`\n`或者`\r\n`标识符进行数据包读取;
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
                                // 添加解码器，LineBasedFrameDecoder解码器会根据`\n`或者`\r\n`进行数据包解析,负责解决粘包与拆包
                                // 数据包内必须要有\n或者\r\n标识，如果没有解码器将等待更多数据，直到遇到换行符或达到一定的最大限制。
                                .addLast(new LineBasedFrameDecoder(1024))
                                // 将LineBasedFrameDecoder解析完的ByteBuf转换为String类型
                                .addLast(new StringDecoder(CharsetUtil.UTF_8))
                                // 添加事件处理器，这里直接使用SimpleChannelInboundHandler处理器，将数据包类型设置为String即可
                                // 因为经过了StringDecoder的处理，所以这里可以直接使用String类型作为参数
                                .addLast(new SimpleChannelInboundHandler<String>() {
                                    @Override
                                    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
                                        var message = "client send data: [" + msg + "]";
                                        System.out.println(message);

                                        // 将消息追加一个\n，转为大写写回客户端
                                        // 必须要有\n或者\r\n标识符，否则客户端的LineBasedFrameDecoder解码器将无法解析数据包
                                        String recvMsg = msg.concat("\n").toUpperCase();
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
