package com.zero.net.netty.examples.websocket;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;

/**
 * 基于Netty实现WebSocket服务
 *
 * WebSocket服务端
 *
 * @author Zero.
 * @date 2023/8/16 12:52 PM
 */
public class Server {
    public static void main(String[] args) {
        // 创建事件循环组
        NioEventLoopGroup boss = new NioEventLoopGroup(1);
        NioEventLoopGroup worker = new NioEventLoopGroup(1);
        try {
            // 创建服务端引导类
            ServerBootstrap server = new ServerBootstrap();
            server.group(boss,worker)
                    .channel(NioServerSocketChannel.class)
                    .childOption(ChannelOption.SO_KEEPALIVE,true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            // WebSocket基于HTTP协议，所以需要设置HTTP协议解码编码处理器
                            // HttpServerCodec是HttpRequestDecoder和HttpResponseEncoder处理器的结合，作用如下
                            // 将字节流解码为HttpRequest对象
                            // 将HttpResponse对象编码为字节流
                            pipeline.addLast(new HttpServerCodec());

                            // 客户端发送的 HTTP 请求可能包含多个部分，例如请求行、请求头和请求体等。
                            // 在 Netty 中，这些部分会被解析成不同类型的对象，例如 HttpRequest 和 HttpContent。
                            // HttpObjectAggregator 会将这些部分聚合成一个完整的 FullHttpRequest 对象;
                            pipeline.addLast(new HttpObjectAggregator(65536));

                            // 数据压缩处理器，提高性能和效率。
                            pipeline.addLast(new WebSocketServerCompressionHandler());

                            // WebSocket预处理器
                            // 1. 负责http协议握手校验、一旦校验成功，升级为WebSocket通道;
                            // 2. 控制帧处理，解析后的二进制帧数据，传递到下一个处理器中;
                            // 3. 设置websocket协议path
                            pipeline.addLast(new WebSocketServerProtocolHandler("/ws",null,true));

                            // 设置自定义事件处理器
                            pipeline.addLast(new ServerHandler());
                        }
                    });
            // 阻塞启动服务端
            ChannelFuture serverChannelFuture = server.bind(19001).addListener((ChannelFutureListener) cf -> {
                if (cf.isSuccess()){
                    System.out.println("websocket server running successful...");
                }
            }).sync();
            // 阻塞等待服务通道关闭
            serverChannelFuture.channel().closeFuture().sync();
        }catch (Exception e){
            e.printStackTrace();
        }finally{
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }
}
