package com.zero.net.system.bio;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

/**
 * 客户端连接会话，即对{@link java.net.Socket}连接的封装.
 *
 * @author Zero.
 * <p> Created on 2025/4/4 12:34 </p>
 */
public class Session extends Thread{
    private final Socket conn;
    public Session(Socket socket) {
        this.conn = socket;
    }
    @Override
    public void run() {
        System.out.printf("[%s] 连接服务器... \n", this.conn.getRemoteSocketAddress().toString());
        InputStream in = null;
        byte[] buf = new byte[1024];
        try {
            // 获取连接的输入流
            in = this.conn.getInputStream();
            while (true) {
                // 阻塞读取客户端数据
                // 将客户端数据读取到缓冲区
                int len = in.read(buf);
                if (len == -1){
                    // 客户端关闭
                    System.out.printf("[%s] 断开连接... \n", conn.getRemoteSocketAddress().toString());
                    this.conn.close();
                    return;
                }
                // 去除\n符
                String line = new String(buf, 0, len - 1);
                // 输出数据
                System.out.println(line);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
