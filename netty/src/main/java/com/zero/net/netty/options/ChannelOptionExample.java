package com.zero.net.netty.options;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.ChannelOption;
import io.netty.channel.DefaultMaxBytesRecvByteBufAllocator;

/**
 * ChannelOption用于设置Channel的相关配置，下面针对Channel的各种配置进行解释。
 *
 * @author Zero.
 * <p> Created on 2025/4/4 12:43 </p>
 */
public class ChannelOptionExample {
    public static void main(String[] args) {
        ServerBootstrap app = new ServerBootstrap();

        // 客户端连接超时时间，如果超时则抛出timeout异常，单位为毫秒，默认为30s; 适用于boss->option
        app.option(ChannelOption.CONNECT_TIMEOUT_MILLIS,1000);

        // 客户端连接请求是顺序处理的，同时时间只能处理一个连接请求，连接请求过多的话会放入等待队列中等待处理
        // 该参数指定了等待队列的大小，默认为50; 适用于boss->option
        app.option(ChannelOption.SO_BACKLOG,50);

        // 是否允许重复使用本地地址和端口; 适用于boss->option
        app.option(ChannelOption.SO_REUSEADDR,true);

        // 是否启用TCP的Keep-alive，默认为false; 适用于boss->option
        // 开启后，会自动探测空闲连接的存活性，默认每隔2小时探测一次;
        app.option(ChannelOption.SO_KEEPALIVE,true);

        // 设置缓冲区的分配器; boss与worker都可用
        // 默认为 UnpooledByteBufAllocator.DEFAULT
        app.option(ChannelOption.ALLOCATOR, UnpooledByteBufAllocator.DEFAULT);
        // 4.0默认为UnpooledByteBufAllocator.DEFAULT
        app.childOption(ChannelOption.ALLOCATOR,UnpooledByteBufAllocator.DEFAULT);
        // 4.1 默认为PooledByteBufAllocator.DEFAULT
        app.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);

        // 配置接收缓冲区的分配策略，默认为AdaptiveRecvByteBufAllocator.DEFAULT，它会根据当前的网络情况动态地调整缓冲区的大小，以优化性能
        // 可选方案有如下:
        // FixedRecvByteBufAllocator: 这是一个固定缓冲区大小的分配器，通过构造函数指定固定的缓冲区大小，这可能适用于一些特定的场景，如果你已经了解你的应用所需的缓冲区大小。
        // DefaultMaxBytesRecvByteBufAllocator: 根据不同的网络情况和负载动态地调整接收缓冲区的大小，以达到更好的性能。
        app.option(ChannelOption.RCVBUF_ALLOCATOR, new DefaultMaxBytesRecvByteBufAllocator());

        // 设置I/O操作的超时时间
        app.childOption(ChannelOption.SO_TIMEOUT,1000);
    }
}
