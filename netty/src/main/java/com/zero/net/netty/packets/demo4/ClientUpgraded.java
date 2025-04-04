//package com.zero.net.netty.packets.demo4;
//
//import com.zero.net.netty.packets.demo5.pb.Protocol;
//import io.netty.bootstrap.Bootstrap;
//import io.netty.channel.*;
//import io.netty.channel.nio.NioEventLoopGroup;
//import io.netty.channel.socket.nio.NioSocketChannel;
//import io.netty.handler.codec.protobuf.ProtobufEncoder;
//import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
//
//import java.util.UUID;
//import java.util.concurrent.TimeUnit;
//
///**
// * Netty粘包与拆包解决方案三
// * 使用Netty基于Protobuf数据序列化格式来进行通信
// * 直接使用内置的ProtobufVarint32LengthFieldPrepender + ProtobufEncoder解码器来进行Protobuf的编码处理
// * <p>
// * 客户端
// *
// * @author Zero.
// * @date 2023/8/15 9:19 AM
// */
//public class ClientUpgraded {
//    public static void main(String[] args) {
//        NioEventLoopGroup worker = new NioEventLoopGroup();
//        try {
//            ChannelFuture channelFuture = new Bootstrap()
//                    .group(worker)
//                    .channel(NioSocketChannel.class)
//                    .handler(new ChannelInitializer<NioSocketChannel>() {
//                        @Override
//                        protected void initChannel(NioSocketChannel ch) throws Exception {
//                            ch.pipeline()
//                                    // 添加内置Protobuf编码器，获取数据的字节长度，写入缓冲区前4个byte位置
//                                    .addLast(new ProtobufVarint32LengthFieldPrepender())
//                                    // 添加Protobuf序列化处理器
//                                    .addLast(new ProtobufEncoder())
//                                    // 事件处理器
//                                    .addLast(new ChannelInboundHandlerAdapter() {
//                                        @Override
//                                        public void channelActive(ChannelHandlerContext ctx) throws Exception {
//                                            new Thread(() -> {
//                                                // 尝试发送100条数据给服务端
//                                                for (int i = 0; i < 100; i++) {
//                                                    Protocol.PayloadRequest request = Protocol.PayloadRequest.newBuilder()
//                                                            .setId(i)
//                                                            .setName(UUID.randomUUID().toString())
//                                                            .setIsActive(i % 2 == 0)
//                                                            .setSendTime(System.currentTimeMillis()).build();
//                                                    ctx.writeAndFlush(request);
//                                                }
//                                                try {
//                                                    TimeUnit.SECONDS.sleep(1);
//                                                } catch (InterruptedException e) {
//                                                    throw new RuntimeException(e);
//                                                }
//                                            }).start();
//                                        }
//                                    });
//                        }
//                    }).connect("127.0.0.1", 9988).sync()
//                    .addListener((ChannelFutureListener) channelFuture1 -> {
//                        if (channelFuture1.isSuccess()) {
//                            System.out.println("client running successful...");
//                        }
//                    });
//            channelFuture.channel().closeFuture().sync();
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            worker.shutdownGracefully();
//        }
//    }
//}
