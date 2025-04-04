package com.zero.net.system.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

/**
 * NIO (non block IO) 核心组件:
 *  - Channel: 对应了IO编程中的连接，有FileChannel、SocketChannel、ServerSocketChannel等实现，Channel中会产生Read、Write、Accept等事件;
 *  - Buffer: 缓冲区，用于从Channel中读取或者写入的组件，有ByteBuffer、CharBuffer等实现;
 *  - Selector: 多路复用器，通过该组件来监听多个Channel;
 * 使用原生NIO实现一个TCP服务器
 *
 * @author Zero.
 * <p> Created on 2025/4/4 12:38 </p>
 */
public class Server {
    // 多路复用器(事件监听器)
    private Selector selector;
    // 缓冲区
    private final ByteBuffer buffer = ByteBuffer.allocate(1024);
    // 服务端口
    private final int port;

    public Server(int port) {
        this.port = port;
    }

    /**
     * 服务器运行
     */
    public void run() throws IOException {
        // 创建一个ServerSocketChannel通道
        ServerSocketChannel server = ServerSocketChannel.open();
        // 设置为非阻塞模式
        server.configureBlocking(false);
        // 将该通道作为一个NIO服务器,绑定端口
        server.socket().bind(new InetSocketAddress(port));

        // 初始化事件监听器
        this.selector = Selector.open();
        // 将NIO服务通道注册到该监听器,该通道只监听ACCEPT事件
        server.register(this.selector, SelectionKey.OP_ACCEPT);
        System.out.println("服务端启动成功...");
        // 监听事件
        this.select();
    }

    /**
     * 监听器事件处理
     * 负责监听ACCEPT、READ事件
     *
     * @throws IOException
     */
    private void select() throws IOException {
        // 循环处理事件
        while (true) {
            // 这里会阻塞住，直到有channel的事件就绪后会被唤醒
            selector.select();
            // 获取事件就绪的Channel列表,这里包装为SelectionKey
            Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
            // 处理所有就绪的Channel
            while (iter.hasNext()) {
                SelectionKey nexted = iter.next();
                iter.remove();// 移除
                if (nexted.isAcceptable()) {
                    // ACCEPT事件处理
                    this.doAccept(nexted);
                } else if (nexted.isReadable()) {
                    // READ 事件处理
                    this.doRead(nexted);
                }
            }
        }
    }


    /**
     * Channel ACCEPT 事件处理 -> 客户端连接事件
     * @param nexted
     * @throws IOException
     */
    private void doAccept(SelectionKey nexted) throws IOException {
        // 获取连接的channel
        SocketChannel channel = ((ServerSocketChannel) nexted.channel()).accept();
        if (channel == null) return;
        // 将连接的channel设置为非阻塞模式
        channel.configureBlocking(false);
        // 将连接的channel，注册到全局监听器,并且只监听READ读事件
        channel.register(this.selector, SelectionKey.OP_READ);

        // 响应客户端消息
        // 将消息写入缓冲区
        buffer.put("连接服务器成功\n".getBytes(StandardCharsets.UTF_8));
        // 切换读模式，让channel从缓冲区中读取
        buffer.flip();
        channel.write(buffer);
        // 清除缓冲区，方便下一次写入
        buffer.clear();

        System.out.printf("[%s] 连接服务器...\n", channel.getRemoteAddress().toString());
    }

    /**
     * Channel的READ事件处理 -> 客户端数据读取
     * @param nexted
     * @throws IOException
     */
    private void doRead(SelectionKey nexted) throws IOException {
        // 获取可以读取数据的channel
        SocketChannel channel = (SocketChannel) nexted.channel();
        // 将channel数据读取到缓冲区
        // TODO 处理并不优雅，如果数据量过大，会被拆分成多块数据
        int len = channel.read(buffer);
        // -1 表示客户端主动关闭
        if (len == -1) {
            System.out.printf("[%s] 关闭客户端... \n", channel.getRemoteAddress().toString());
            channel.close(); // 关闭通道
            nexted.cancel(); // 从selector中移除
            return;
        }
        // 转换为读模式
        buffer.flip();
        String message = StandardCharsets.UTF_8.decode(buffer).toString();
        // 清除缓冲区，方便下一次写入
        buffer.clear();
        System.out.println(message.trim());
    }

    public static void main(String[] args) throws IOException {
        new Server(8989).run();
    }
}
