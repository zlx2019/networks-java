package com.zero.net.system.nio;

import java.nio.IntBuffer;
import java.util.Arrays;

/**
 * {@link java.nio.Buffer} 是NIO中的缓冲区,在NIO模型中有着至关重要的作用。
 * 核心方法如下:
 * clear(): 清空缓冲区，将位置（position）设为 0，限制（limit）设为容量（capacity）。
 * flip(): 切换缓冲区为读模式。将限制（limit）设置为当前位置（position），然后将位置（position）重置为 0。
 * rewind(): 倒回缓冲区，将位置（position）设置为 0，限制（limit）保持不变。
 * mark(): 标记当前位置（position），可以通过 reset() 方法回到这个位置。
 * reset(): 将位置（position）回滚到最后一次调用 mark() 方法时的位置。
 * hasRemaining(): 检查是否还有剩余元素（position < limit）。
 * remaining(): 返回剩余元素的数量（limit - position）。
 * position(): 返回当前位置（position）。
 * position(int newPosition): 设置当前位置（position）。
 * limit(): 返回限制（limit）。
 * limit(int newLimit): 设置限制（limit），不能超过容量（capacity）。
 * capacity(): 返回容量（capacity）。
 * isReadOnly(): 检查缓冲区是否为只读。
 * flip(): 切换为写模式，将限制（limit）设置为当前位置（position），然后将位置（position）重置为 0。
 * compact(): 压缩缓冲区，将未读的数据移动到缓冲区的开始，位置（position）设置为移动的字节数。
 * duplicate(): 复制一个与原缓冲区共享数据的新缓冲区，但具有独立的位置、限制和标记。
 * slice(): 创建一个新的缓冲区，其内容是原缓冲区的一个子序列。
 * asReadOnlyBuffer(): 创建一个只读的缓冲区，与原缓冲区共享数据，但不允许写入。
 * order(): 返回此缓冲区的字节顺序。
 * order(ByteOrder bo): 设置此缓冲区的字节顺序。
 *
 * @author Zero.
 * <p> Created on 2025/4/4 12:41 </p>
 */
public class BufferUsageExample {
    public static void main(String[] args) {
        // 创建一个缓冲区，cap和limit均为10
        IntBuffer buffer = IntBuffer.allocate(10);
        System.out.printf("limit: %d position: %d cap: %d \n",buffer.limit(),buffer.position(),buffer.capacity());
        // 将缓冲区写满，直到position==limit
        int i = 0;
        while (buffer.hasRemaining()){
            buffer.put(i++);
        }
        System.out.printf("limit: %d position: %d cap: %d \n",buffer.limit(),buffer.position(),buffer.capacity());
        // 切换读写模式，将limit设置为当前position，position设置为0，表示可以从0读取到切换模式之前的position位置;
        buffer.flip();
        // 获取 limit - position的差
        // 读模式表示获取 还有多少可以读的元素
        // 写模式标识获取 还有多少可以写的位置
        System.out.println(buffer.remaining());

        // 将数据读取到数组中
        int[] des = new int[buffer.remaining()];
        buffer.get(des);
        System.out.println(Arrays.toString(des));
        System.out.printf("limit: %d position: %d cap: %d \n",buffer.limit(),buffer.position(),buffer.capacity());

        // 转换模式，pos重置为0
        buffer.flip();
        System.out.printf("limit: %d position: %d cap: %d \n",buffer.limit(),buffer.position(),buffer.capacity());

        // 再次写入数据
        buffer.put(1);
        buffer.put(2);
        buffer.put(3);
        buffer.put(4);
        buffer.put(5);
        System.out.printf("limit: %d position: %d cap: %d \n",buffer.limit(),buffer.position(),buffer.capacity());

        // 切换为读模式
        buffer.flip();
        System.out.printf("limit: %d position: %d cap: %d \n",buffer.limit(),buffer.position(),buffer.capacity());

        // 创建一个数组,用于从读取缓冲区中的数据
        int[] buf = new int[5];
        // 从缓冲区中读取2个元素，写入到buf数组中，从索引为0的位置开始写入
        buffer.get(buf,0,0);
        System.out.println(Arrays.toString(buf));
        System.out.printf("limit: %d position: %d cap: %d \n",buffer.limit(),buffer.position(),buffer.capacity());

        // 从缓冲区中读取2个元素，写入到buf数组中，从索引为3的位置开始写入
        buffer.get(buf,3,2);
        System.out.println(Arrays.toString(buf));
        System.out.printf("limit: %d position: %d cap: %d \n",buffer.limit(),buffer.position(),buffer.capacity());

        // 清空缓冲区,pos设置为0，limit设置为cap
        buffer.clear();
        System.out.printf("limit: %d position: %d cap: %d \n",buffer.limit(),buffer.position(),buffer.capacity());
    }
}
