package com.zero.net.netty.packets.demo5;

import com.google.protobuf.InvalidProtocolBufferException;
import com.zero.net.netty.packets.demo5.pb.Protocol;

/**
 * 测试用例
 *
 * @author Zero.
 * @date 2023/8/15 8:36 PM
 */
public class Tests {
    public static void main(String[] args) {

        // 创建Protobuf对象
        Protocol.PayloadRequest payload = Protocol.PayloadRequest.newBuilder()
                .setId(1001)
                .setName("zlx")
                .setIsActive(true).build();

        // 将Protobuf对象序列化为字节数组
        byte[] bytes = payload.toByteArray();
        System.out.println(bytes);

        // 将字节反序列化为Payload对象
        try {
            Protocol.PayloadRequest newPay = Protocol.PayloadRequest.parseFrom(bytes);
            System.out.println(newPay.getName());
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
    }
}
