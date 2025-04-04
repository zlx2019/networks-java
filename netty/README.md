# Netty学习

Netty是由JBoss开源的一个网络服务框架
- 基于事件驱动
- 基于NIO模型


## IO 模型
- BIO (block IO)：

  同步阻塞IO，传统的IO模型;
- NIO (non block IO)：

  同步非阻塞IO，如select、pool、epool等都是NIO的实现
- AIO (Asynchronous IO)：异步非阻塞IO

  异步非阻塞IO，如io_uring;

## 为什么要学习Netty
传统的BIO通常是指一个线程对应一个连接，并且会一直阻塞式处理该连接，如果当前连接长时间没有读写操作，那么会造成巨大的资源浪费。

而NIO是基于IO多路复用模型实现，通常一个客户端连接在NIO中被称为`Channel`，而服务端中的一个线程对应一个`Selector`,
而一个`Select`对应n多个`Channel`，这样的设计使得一个一个线程可以同时处理n多个连接;

但是原生的NIO开发过于复杂，Netty针对于原生的NIO进行进一步封装优化，使得开发更为简便;

使用NIO完成一个最简单的TCP服务需要如下步骤:
1. 创建一个ServerSocketChannel，作为NIO服务端的Channel;
2. 绑定TCP服务，监听端口;
3. 创建Reactor线程(Selector);
4. 将服务端Channel注册到Selector中;
5. 开启Selector监听，接收Accept连接Channel;
6. 将连接Channel注册到Selector中;
7. Selector监听连接Channel的read|Write事件;
8. 读取数据，写回数据;

源码如下:
```java
package io.zero._02_nio;

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
 * NIO (non block IO)
 * 核心组件:
 * Channel: 对应了IO编程中的连接，有FileChannel、SocketChannel、ServerSocketChannel等实现，Channel中会产生Read、Write、Accept等事件;
 * <p>
 * Buffer: 缓冲区，用于从Channel中读取或者写入的组件，有ByteBuffer、CharBuffer等实现;
 * <p>
 * Selector: 多路复用器，通过该组件来监听多个Channel;
 * <p>
 * <p>
 * 使用原生NIO实现一个TCP服务器
 *
 * @author Zero.
 * @date 2023/8/11 11:42 AM
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

```

## Netty核心组件
###  Bootstrap
Bootstrap是Netty的引导类，负责启动Netty服务;
- Bootstrap是客户端的引导启动类
- ServerBootstrap是服务端的引导启动类

<hr>

### EventLoop与EventLoopGroup
EventLoopGroup是Netty中的线程组，可以理解为线程池，管理着工作线程;
- Bootstrap客户端需要一个EventLoopGroup;
- ServerBootstrap服务端需要两个EventLoopGroup，分别为Boos组和work组;

EventLoop可以理解为EventLoopGroup中的工作线程，每个EventLoop对应一个Selector，负责处理多个Channel上的事件;

Netty中具体的实现有如下几种:
- **DefaultEventLoop** + **DefaultEventLoopGroup**
- **NioEventLoop** + **NioEventLoopGroup**
- **EpollEventLoop** + **EpollEventLoopGroup**

通常我们会使用NioEventLoopGroup和NioEventLoop来搭配使用;<br>
而EpollEventLoop和EpollEventLoopGroup只能在Linux环境下使用;
<hr>

### Channel
Channel是Netty中一个网络连接通道，提供了各种方法来管理连接、处理数据和事件;

Channel常用的核心方法有如下:
- `id()`: 获取Channel的id;
- `eventLoop()`: 获取当前channel所注册的EventLoop;
- `parent()`: 获取当前channel的父channel;
- `config()`: 获取channel的配置，包括各种设置和选项;
- `isOpen()`: 当前channel是否处于打开状态，用于检查通道是否可继续使用，如果channel被关闭则会返回false;
- `isRegistered()`: 当前channel是否已经注册到EventLoop中;
- `isActive()`: 当前channel是否处于active。active表示channel已打开，并且已经注册，可以和selector正常通信;
- `metadata()`: 获取channel的元数据;
- `localAddress()`: 获取服务器IP地址;
- `remoteAddress()`: 获取客户端IP地址;
- `closeFuture()`: 获取channel的ChannelFuture;
- `isWritable()`: 当前channel是否可写入;
- `bytesBeforeUnwritable()`: 获取当前channel目前可以写入的字节数;
- `pipeline()`: 获取channel的处理器链;

在Netty中提供了不同类型的Channel的实现:
- **NioSocketChannel**:

  基于Java NIO的Channel的实现，用于处理TCP连接，通过Selector管理，大多数TCP应用会使用该通道;


- **NioSocketServerChannel**

  类似于NioSocketChannel，但它用于表示服务器端监听的Channel。它会接受客户端的连接请求，并将每个新连接分配给一个新的NioSocketChannel实例。


- **EpollSocketChannel** 与 **EpollServerSocketChannel**

  这些Channel基于Linux的epoll机制，提供了更高效的非阻塞I/O。它们适用于在支持epoll的平台上实现更高性能的网络应用。


- **OioSocketChannel** 与 **OioServerSocketChannel**

  基于传统的阻塞IO实现的Channel，不推荐使用，在高版本中已经移除;


- **LocalChannel** 和 **LocalServerChannel**

  这些Channel实现用于在同一台机器上的进程之间进行通信，使用Unix域套接字（Unix Domain Socket）来实现。

通常Channel要搭配指定类型的EventLoop和EventLoopGroup来使用;

如NioSocketChannel + NioServerSocketChannel 搭配 NioEventLoop + NioEventLoopGroup来使用，这也是我们最常使用的方案;

#### ChannelHandler
Channel的事件处理器，用于处理Channel中回调的各种事件;

常用的ChannelHandler实现有如下:
- `ChannelInitializer`: Channel的初始化处理器，通常用于设置channel的处理器链;
- `ChannelInboundHandlerAdapter`: Channel的入站事件处理，如可读、连接、关闭连接等事件;
- `ChannelOutboundHandlerAdapter`: Channel的出站事件处理，如写回、close事件;
- `MessageToMessageEncoder`: 数据的编码处理器;
- `MessageToMessageDecoder`: 数据的解码处理器;

#### ChannelPipeline
Channel的事件处理链，在Netty中，我们可以定义多个ChannelHandler，事件会经过一系列的Hanlder处理器的处理，就像责任链模式一样，并且写回数据也会经过一系列的处理器处理;

ChannelPipeline可以理解成条管道，里面有多个Handler，事件会流经该管道里面的每一个Handler进行处理，只有当消息经过所有的Handler处理之后服务器才能接收到客户端发送过来的消息

#### ChannelHandlerContext
ChannelHandlerContext保存着Channel的上下文，同时关联着一个ChannelHandler，通过ChannelHadlerContext，ChannelHandler才能与ChannelPipeline或者其他的ChannelHandler进行交互;

#### ChannelFuture
Netty中的IO都是异步的，那么异步的话就会返回将来用来获取返回值的对象，也就是Future，通过ChannelFuture我们可以知道IO是否操作成功，是否已经完成，是否已取消等等，比如服务端是否创建成功;

#### ChannelOption
ChannelOption保存了很多参数，可以让我们更好的配置Channel,更多的配置在ChannelOption.class类中配置

<hr>

### Selector
Netty基于java.nio.channels.Selector对象实现IO多路复用，通过Selector一个线程可以监听多个连接的Channel事件。当向一个Selector中注册Channel后，Selector内部的机制就可以自动不断的Select这些注册的Channel是否有就绪的IO事件（可读、可写、网络连接完成等）。

<hr>

### ByteBuf
由于NIO的ByteBuffer过于复杂，所以Netty自己封装了一个缓冲区组件;
