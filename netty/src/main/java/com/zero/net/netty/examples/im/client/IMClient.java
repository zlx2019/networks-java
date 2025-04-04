package com.zero.net.netty.examples.im.client;

import com.google.protobuf.ByteString;
import com.zero.net.netty.examples.im.packet.Codec;
import com.zero.net.netty.examples.im.pb.Protocol;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.CharsetUtil;

import java.util.Scanner;

/**
 * Netty IM Client
 *
 * @author Zero.
 * <p> Created on 2025/4/4 13:12 </p>
 */
public class IMClient {
    public static void main(String[] args) {
        NioEventLoopGroup worker = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap()
                    .group(worker)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<NioSocketChannel>() {
                        @Override
                        protected void initChannel(NioSocketChannel ch) throws Exception {
                            ch.pipeline()
                                    // 设置解码处理器(入站)，用于解码将读取到的数据解码为ReplyMessage对象
                                    .addLast(new Codec.ReplyMessageDecoder())
                                    // 设置事件处理器(入站)
                                    .addLast(new IMClientHandler())
                                    // 入站事件会按照处理器添加顺序执行
                                    // 先执行ReplyMessageDecoder解码，再执行ClientHandler处理器;

                                    // 设置编码处理器(出站)，用于将字节数组打包为自定义协议
                                    .addLast(new Codec.ProtobufPacker())
                                    // 设置编码处理器(出站)，用于将对象序列化为字节数组
                                    .addLast(new Codec.ProtobufEncoder());
                            // 出站逻辑会以相反的顺序进行执行
                            // 所以会先执行ProtobufEncoder再执行ProtobufPacker
                        }
                    });
            ChannelFuture future = bootstrap.connect("127.0.0.1", 19002).sync();
            Scanner scanner = new Scanner(System.in);
            Protocol.SendMessage.Builder builder = Protocol.SendMessage.newBuilder();
            while (true) {
                System.out.print("请输入: ");
                String line = scanner.nextLine();
                builder.setId(1001).setName("zero").setGender(Protocol.Gender.BOY)
                        .setSendTime(System.currentTimeMillis())
                        .setPayload(ByteString.copyFrom(line, CharsetUtil.UTF_8));
                future.channel().writeAndFlush(builder.build());
            }
//            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            worker.shutdownGracefully();
        }
    }
}
