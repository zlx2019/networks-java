package com.zero.net.system.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Scanner;

/**
 * NIO 客户端
 *
 * @author Zero.
 * <p> Created on 2025/4/4 12:38 </p>
 */
public class Client {
    // 创建两个缓冲区，分别用于发送消息和接收消息
    private final ByteBuffer sendBuf = ByteBuffer.allocate(1024);
    private final ByteBuffer recvBuf = ByteBuffer.allocate(1024);


    // 运行客户端
    private void run() throws IOException, InterruptedException {
        // 创建一个网络通道,作为客户端
        SocketChannel clientChannel = SocketChannel.open();
        // 使用通道作为一个客户端去连接服务端
        clientChannel.connect(new InetSocketAddress(8989));
        clientChannel.configureBlocking(false);
        // 创建一个监听器,监听该通道的读写事件
        Selector selector = Selector.open();
        // 将要被监听的通道，注册到监听器中
        clientChannel.register(selector, SelectionKey.OP_READ);
        System.out.println("客户端启动成功");
        Scanner in = new Scanner(System.in);
        new Thread(()->{
            while (true){
                try {
                    // 阻塞等待事件
                    int i = selector.select();
                    // 获取触发的事件
                    Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
                    while (iter.hasNext()){
                        SelectionKey nexted = iter.next();
                        // 读事件处理
                        if (nexted.isReadable()){
                            // 获取channel
                            SocketChannel channel = (SocketChannel) nexted.channel();
                            // 读取数据
                            int len = channel.read(recvBuf);
                            // 切换读模式
                            recvBuf.flip();
                            String message = StandardCharsets.UTF_8.decode(recvBuf).toString();
                            System.out.println(message);
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();

        while (true){
            // 向服务端写数据
            String line = in.nextLine();
            // 将数据写入缓冲区
            sendBuf.put(line.getBytes(StandardCharsets.UTF_8));
            // 切换为读模式
            sendBuf.flip();
            // 将消息写入通道
            clientChannel.write(sendBuf);
            sendBuf.clear();
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        new Client().run();
    }
}
