package com.zero.net.netty.packets.demo5;

import com.zero.net.netty.packets.demo5.pb.Protocol;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 客户端事件处理器
 *
 * @author Zero.
 * @date 2023/8/15 9:16 PM
 */
public class ClientHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        new Thread(()->{
            while (true){
                for (int i = 0; i < 10; i++) {
                    Protocol.PayloadRequest request = Protocol.PayloadRequest.newBuilder()
                            .setId(i)
                            .setName(UUID.randomUUID().toString())
                            .setIsActive(i % 2 == 0)
                            .setSendTime(System.currentTimeMillis()).build();
                    ctx.writeAndFlush(request);
                }
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }
}
