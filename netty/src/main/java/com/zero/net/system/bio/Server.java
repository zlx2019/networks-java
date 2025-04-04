package com.zero.net.system.bio;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * BIO（Blocking IO）同步阻塞IO，即Java原生类库提供的IO。
 * 基于 BIO 实现服务端与客户端心跳机制
 *
 * @author Zero.
 * <p> Created on 2025/4/4 12:34 </p>
 */
public class Server {
    private final int port;
    public Server(int port){
        this.port = port;
    }

    /**
     * 服务运行
     */
    public void run() throws IOException {
        // 创建一个BIO网络服务
        ServerSocket serverSocket = new ServerSocket(this.port);
        System.out.println("服务器启动...");
        // 循环处理连接
        while (true){
            // 阻塞等待连接...
            Socket socket = serverSocket.accept();
            // 开启一个线程，处理连接
            new Session(socket).start();
        }
    }
    public static void main(String[] args) throws IOException {
        new Server(8090).run();
    }
}
