package com.zero.net.netty.examples.tcp;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;

import java.nio.channels.ServerSocketChannel;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Netty 实现 TCP协议双端服务。
 * TCP服务端，使用组件：
 *  - NioEventLoop:          事件循环
 *  - NioEventLoopGroup:     事件循环组
 *  - NioSocketChannel:      Netty 对 {@link SocketChannel} 的封装
 *  - NioServerSocketChannel Netty 对 {@link ServerSocketChannel} 的封装
 *
 * @author Zero.
 * <p> Created on 2025/4/4 12:45 </p>
 */
public class Server {
    public static void main(String[] args) throws InterruptedException {
        // 创建boss线程组,负责连接事件的处理
        EventLoopGroup boss = new NioEventLoopGroup();
        // 创建worker线程组，负责IO事件
        EventLoopGroup worker = new NioEventLoopGroup();

        // 创建Netty Server服务
        ServerBootstrap app = new ServerBootstrap();
        // 设置线程组
        app.group(boss, worker);

        // 设置使用的Channel类型，会将连接都封装为该类型
        // 该案例是TCP服务端，所以使用NioServerSocketChannel
        app.channel(NioServerSocketChannel.class);

        // 设置Channel的处理器
        app.childHandler(new MyChannelInit());

        // 绑定端口
        ChannelFuture channelFuture = app.bind(7979).sync();

        // 设置启动是否成功监听器
        channelFuture.addListener((ChannelFutureListener) cf -> {
            if (cf.isSuccess()){
                System.out.println("Netty Server Start Successful...");
            }
        });
        // 启动服务 阻塞...
        channelFuture.channel().closeFuture().sync();

        // 释放资源
        boss.shutdownGracefully();
        worker.shutdownGracefully();
    }

    /**
     * Channel初始化接口实现，用于Channel初始化时的回调接口
     * 通常用于Channel初始化时为Channel设置Pipeline处理链。
     */
    static class MyChannelInit extends ChannelInitializer<SocketChannel> {
        /**
         * 当有新的客户端连接被接受时，会为每个新连接创建一个对应的NioSocketChannel。
         * 在这个NioSocketChannel被创建后，会调用ChannelInitializer的initChannel()方法,以便对这个新连接进行初始化设置。
         *
         * @param ch    建立连接的channel
         */
        @Override
        protected void initChannel(SocketChannel ch) throws Exception {
            // 为创建完成的Channel设置处理器链
            // 处理链会按照顺序依次执行处理
            ChannelPipeline pipeline = ch.pipeline();

            //TODO 添加其他处理器，如编码解码器...

            // 添加Channel的入站事件处理器(核心)
            ch.pipeline().addLast(new MyChannelInHanlder());
            // 添加Channel的出站事件处理器(核心)
            ch.pipeline().addLast(new MyChannelOutHandler());
            ch.pipeline()
                    // 添加Channel空闲状态事件，如果连接空闲时长超过指定时长(30s)会触发一个IdleStateEvent事件
                    .addLast(new IdleStateHandler(0,0,30, TimeUnit.SECONDS))
                    // 添加空闲超时处理器
                    .addLast(new MyChannelIdleHandler());
        }
    }

    /**
     * 自定义Channel入站事件的处理器实现类
     */
    static class MyChannelInHanlder extends ChannelInboundHandlerAdapter {

        /**
         * 当Channel连接建立时被调用
         * @param ctx   上下文
         */
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            System.out.printf("[%s]建立连接完成... \n",ctx.channel().remoteAddress().toString());

            // 调用下一个ChannelInboundHandlerAdapter处理器的channelActive()方法
            super.channelActive(ctx);
        }

        /**
         * 当Channel连接关闭时被调用
         * @param ctx   上下文
         */
        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            System.out.printf("[%s] 关闭连接... \n",ctx.channel().remoteAddress().toString());
            super.channelInactive(ctx);
        }

        /**
         * 当Channel有数据可读时被调用
         * @param ctx   上下文
         * @param msg   可读取的数据
         */
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            // 读取数据
            ByteBuf buf = (ByteBuf) msg;
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            var message = new String(bytes);
            System.out.println(message.trim());
            ctx.channel().writeAndFlush(Unpooled.copiedBuffer(message.getBytes()));

            // 模拟主动关闭channel
            if ("cnm".equals(message.trim())){
                // 写回数据
                buf.clear();
                buf.writeBytes("出口不逊，滚蛋吧!".getBytes());
                ctx.channel().writeAndFlush(buf);

                // 关闭channel
                ctx.channel().close();
                return;
            }
            // 传递到下一个处理器的channelRead方法
            super.channelRead(ctx, msg);
        }

        /**
         * 当处理过程中捕获到异常时被调用
         * @param ctx   上下文
         * @param cause 捕获到的异常
         */
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            super.exceptionCaught(ctx, cause);
        }
    }

    /**
     * 自定义Channel出站事件的处理器实现类
     */
    static class MyChannelOutHandler extends ChannelOutboundHandlerAdapter{

        /**
         * 调用channel的close()方法时被调用,用于释放资源或执行其他必要的关闭操作。
         *
         * @param ctx   上下文
         * @param promise
         */
        @Override
        public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
            System.out.printf("[%s] 强制关闭该连接... \n",ctx.channel().remoteAddress().toString());
            super.close(ctx, promise);
        }

        /**
         * 掉调用channel的write()方法后调用，用于将数据写入到Channel中，这个方法会在数据写入之前被调用。
         *
         * @param ctx   上下文
         * @param msg   写入的数据
         * @param promise
         */
        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            super.write(ctx, msg, promise);
        }

        /**
         * 当执行channel的flush()方法后调用，将所有待发送的数据刷新到远程节点，并且在完成后释放相应的资源。
         *
         * @param ctx   上下文
         */
        @Override
        public void flush(ChannelHandlerContext ctx) throws Exception {
            super.flush(ctx);
        }
    }

    /**
     * 自定义Channel针对于IdleStateEvent事件的处理器
     */
    static class MyChannelIdleHandler extends ChannelInboundHandlerAdapter{
        // 常量心跳包,不会被释放
        private final ByteBuf HEART_PACK = Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("heartbeat", Charset.defaultCharset()));

        /**
         * 当channel状态事件发生时被调用
         *
         * @param ctx   上下文
         * @param evt   触发的事件
         */
        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            // 如果是IdleStateEvent事件，则表示当前channel空闲超时，直接close关闭channel
            if (evt instanceof IdleStateEvent){
                // 关闭channel
                ctx.channel().close();
//                System.out.println("idel");
//                ctx.writeAndFlush(HEART_PACK.duplicate())
//                        // 如果发生异常，则关闭当前连接
//                        .addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            }else {
                super.userEventTriggered(ctx, evt);
            }
        }
    }
}
