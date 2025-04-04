# 粘包与拆包
## 什么是粘包与拆包?
TCP是一种面向连接的、可靠的、基于字节流的传输层通信协议。 这种基于流的协议是没有明显边界的，TCP这种底层协议是不会理解上层的业务业务含义的，因此在通信过程中，发送数据流的时候，有可能出现一份完整的数据，被TCP拆分为多个数据包进行发送，当然也有可能将多个数据包合并为一个数据包进行发送;

<hr>

## 出现原因?
1. 客户端要发送的数据小于TCP发送缓冲区的大小，TCP为了提升效率，将多个写入缓冲区的数据包一次发送出去，多个数据包粘在一起，造成粘包;
2. 服务端的应用层没有及时处理接收缓冲区中的数据，再次进行读取时出现粘包问题;
3. 数据发送过快，数据包堆积导致缓冲区积压多个数据后才一次性发送出去;
4. 拆包一般由于一次发送的数据包太大，超过MSS的大小，那么这个数据包就会被拆成多个TCP报文分开进行传输;

<hr>

## 解决方案
### 加标记
加标记，比如FTP协议使用\n作为完结标识，接收端根据\n标识进行解析即可;
### 固定数据包大小
将每个数据包设定为固定大小，不够的话就通过补充特定的字符进行填充，接收端根据固定大小进行解析;
### 添加消息头
将数据包分为两部分，头部包含数据总大小，后面是具体的数据包，先解析头部得出大小，再将具体数据读取出来;

<hr>

## 解决案例实现
### 1. 加标记方式
接收端通过`LineBasedFrameDecoder` + `StringDecoder`解码处理器，解决粘包与拆包;

以`\n`或者`\r\n`作为数据包结尾标识符，接收端解析ByteBuf时，根据该标识符进行解析即可;
#### 服务端
```java
/**
 * Netty粘包与拆包解决方案一
 * 加标记,使用特定符号作为数据包的结尾标识，如`\n`
 * 接收端根据`\n`标识读取为一个完整的数据包
 *
 * 服务端
 *
 * @author Zero.
 * @date 2023/8/15 9:04 AM
 */
public class Server {
    public static void main(String[] args) throws InterruptedException {
        NioEventLoopGroup boss = new NioEventLoopGroup();
        NioEventLoopGroup worker = new NioEventLoopGroup();
        // 创建Server Channel
        ChannelFuture channelFuture = new ServerBootstrap()
                .group(boss, worker)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline()
                                // 添加解码器，LineBasedFrameDecoder解码器会根据`\n`或者`\r\n`进行数据包解析,负责解决粘包与拆包
                                // 数据包内必须要有\n或者\r\n标识，如果没有解码器将等待更多数据，直到遇到换行符或达到一定的最大限制。
                                .addLast(new LineBasedFrameDecoder(1024))
                                //
                                // 或者使用指定的标识符,如使用%作为结尾标识符，如下
                                // .addLast(new DelimiterBasedFrameDecoder(1024, Unpooled.wrappedBuffer(new byte[]{'%'})))
                                // 将解析完的ByteBuf转换为String类型
                                .addLast(new StringDecoder(CharsetUtil.UTF_8))
                                // 添加事件处理器，这里直接使用SimpleChannelInboundHandler处理器，将数据包类型设置为String即可
                                // 因为经过了StringDecoder的处理，所以这里可以直接使用String类型作为参数
                                .addLast(new SimpleChannelInboundHandler<String>() {
                                    @Override
                                    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
                                        var message = "client send data: [" + msg + "]";
                                        System.out.println(message);

                                        // 将消息追加一个\n，转为大写写回客户端
                                        // 必须要有\n或者\r\n标识符，否则客户端的LineBasedFrameDecoder解码器将无法解析数据包
                                        String recvMsg = msg.concat("\n").toUpperCase();
                                        // 将消息写回客户端
                                        ctx.channel().writeAndFlush(Unpooled.copiedBuffer(recvMsg.getBytes()));
                                    }
                                });
                    }
                }).bind(9696).sync();
        System.out.println("server running...");
        // 阻塞等待Channel关闭
        channelFuture.channel().closeFuture().sync();
        boss.shutdownGracefully();
        worker.shutdownGracefully();
    }
}
```
#### 客户端
```java
public class Client {
    public static void main(String[] args) throws InterruptedException {
        NioEventLoopGroup worker = new NioEventLoopGroup();
        ChannelFuture channelFuture = new Bootstrap()
                .group(worker)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline()
                                // 添加解码器，LineBasedFrameDecoder解码器会根据`\n`或者`\r\n`进行数据包解析,负责解决粘包与拆包
                                // 数据包内必须要有\n或者\r\n标识，如果没有解码器将等待更多数据，直到遇到换行符或达到一定的最大限制。
                                .addLast(new LineBasedFrameDecoder(10))
                                // 将解析完的ByteBuf转换为String类型
                                .addLast(new StringDecoder(CharsetUtil.UTF_8))
                                // 添加事件处理器，这里直接使用SimpleChannelInboundHandler处理器，将数据包类型设置为String即可
                                // 因为经过了StringDecoder的处理，所以这里可以直接使用String类型作为参数
                                .addLast(new SimpleChannelInboundHandler<String>() {
                                    @Override
                                    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
                                        System.out.println("server recv data: [" + msg + "]");
                                    }
                                    @Override
                                    public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                        new Thread(()->{
                                            for (int i = 0; i < 100; i++) {
                                                // 发送数据包，以\n作为结尾
                                                // 必须要有\n或者\r\n标识符，否则服务端的LineBasedFrameDecoder解码器将无法解析数据包
                                                ctx.channel().writeAndFlush(Unpooled.copiedBuffer("Hello\n".getBytes()));
                                            }
                                        }).start();
                                    }
                                });
                    }
                }).connect("127.0.0.1", 9696).sync();
        channelFuture.channel().closeFuture().sync();
        worker.shutdownGracefully();
    }
}
```
### 2. 固定长度
固定数据包长度大小，如果数据包太小则用指定字符填充，接收端根据固定长度进行读取即可,可以使用Netty提供的FixedLengthFrameDecoder解码器;
```java
public class Server {
    public static void main(String[] args) throws InterruptedException {
        NioEventLoopGroup boss = new NioEventLoopGroup();
        NioEventLoopGroup worker = new NioEventLoopGroup();
        ChannelFuture channelFuture = new ServerBootstrap()
                .group(boss, worker)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline()
                                // 添加解码器,以固定字节长度进行解析，以每20个byte解析为一个ByteBuf
                                .addLast(new FixedLengthFrameDecoder(20))
                                // 将FixedLengthFrameDecoder解析后的ByteBuf解析为String类型
                                .addLast(new StringDecoder(CharsetUtil.UTF_8))
                                // 事件处理
                                .addLast(new SimpleChannelInboundHandler<String>() {
                                    @Override
                                    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
                                        System.out.println(msg);
                                    }
                                });
                    }
                }).bind(9696).sync();
        System.out.println("server running...");
        // 阻塞等待Channel关闭
        channelFuture.channel().closeFuture().sync();
        boss.shutdownGracefully();
        worker.shutdownGracefully();
    }
}

```

<hr>

### 3. 定义数据头
#### 3.1 Netty + Protobuf协议
请参考`packets/demo4`案例，基于Netty + Protobuf + 内置的处理器;


#### 3.2. 自定义协议
请参考`packets/demo5`案例，基于Netty + Protobuf + 自定义协议解决粘包与拆包问题;

自定义编码器
```java
public class CustomEncoder extends MessageToByteEncoder<Object> {
    /**
     * 将一个对象进行编码序列化，写入到ByteBuf中
     * @param ctx           上下文
     * @param msg           要编码的对象
     * @param out           缓冲区
     * @throws Exception    异常
     */
    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {

    }
}
```
自定义解码器
```java
public class CustomDecoder extends ByteToMessageDecoder {
    /**
     * 自定义解码器，从ByteBuf中读取数据，解析为指定的数据类型，放入到out中;
     *
     * @param ctx 上下文
     * @param in  要解析的ByteBuf
     * @param out 输出列表
     * @throws Exception 异常
     */
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {

    }
}
```
或者同时实现解码与编码
```java
public class CustomEncoder extends ByteToMessageCodec<Object> {
    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
    }
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
    }
}

```

<hr>

### 常用的编码解码处理器
#### LineBasedFrameDecoder
换行符解码器，以`\n`或者`\r\n`作为结尾标识进行数据包的解析，解析成一个完整的数据包;
```java
// 添加解码器，LineBasedFrameDecoder解码器会根据`\n`或者`\r\n`进行数据包解析,负责解决粘包与拆包
// 数据包内必须要有\n或者\r\n标识，如果没有解码器将等待更多数据，直到遇到换行符或达到一定的最大限制。
ch.pipeline().addLast(new LineBasedFrameDecoder(1024));
```
使用这个解码器需要注意，如果数据包内没有`\n`或者`\r\n`标识位，则会认为一个数据包还未解析完整，会一直等待更多的数据，直到遇到标识符或者到达最大限制抛出`TooLongFrameException`异常;

<hr>

#### DelimiterBasedFrameDecoder 分隔符解码器
以自定义的字符作为结尾标识进行数据包的解析;
```java
ch.pipeline().addLast(new DelimiterBasedFrameDecoder(1024, Unpooled.wrappedBuffer(new byte[]{'%'})))
```
如果未解析到指定的标识符，超出限制后一样抛出`TooLongFrameException`异常;
<hr>

#### StringDecoder

将ByteBuf解码为String类型
```java
// 将解析完的ByteBuf转换为String类型
ch.pipeline()
        .addLast(new StringDecoder(CharsetUtil.UTF_8))
        // 添加事件处理器，这里直接使用SimpleChannelInboundHandler处理器，将数据包类型设置为String即可
        // 因为经过了StringDecoder的处理，所以这里可以直接使用String类型作为参数
        .addLast(new SimpleChannelInboundHandler<String>() {
              @Override
              protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
                  
            }
        });
```

#### FixedLengthFrameDecoder
以固定字节长度进行解析，每解析指定的长度后，作为一个ByteBuf;
```java
// 添加解码器,以固定字节长度进行解析，以每20个byte解析为一个ByteBuf
ch.pipeline().addLast(new FixedLengthFrameDecoder(20))
```

<hr>

#### ProtobufVarint32LengthFieldPrepender
获取数据的字节长度，写入缓冲区前4个byte位置

<hr>

#### ProtobufVarint32FrameDecoder
优先读取前4个byte字节(int整数)，以读取到的int为后续要读取的长度来进行数据读取;

