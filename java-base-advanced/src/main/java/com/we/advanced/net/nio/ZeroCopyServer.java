package com.we.advanced.net.nio;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * 基于零拷贝实现数据传输
 * @author we
 * @date 2021-05-15 11:36
 **/
public class ZeroCopyServer {
    public static void main(String[] args) {

        try {
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.socket().bind(new InetSocketAddress(8080));
            // 暂时先使用阻塞模式，获取客户端连接
            SocketChannel socketChannel = serverSocketChannel.accept();

            // 文件传输时，很少情况下，能够一次性拷贝完的；我们这里设定2048,但是当文件大小大于2048时，就会出问题
            ByteBuffer byteBuffer = ByteBuffer.allocate(2048);

            FileChannel fileChannel = new FileOutputStream("D:/GPlayer_windows_v1.0.9_cp.zip").getChannel();
            int r = 0;
            while (r!=-1){
                r = socketChannel.read(byteBuffer);
                // 读模式转为写模式
                byteBuffer.flip();

                fileChannel.write(byteBuffer);
                // 写模式转为读模式
                byteBuffer.clear();
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally{
            // 关闭流
        }
    }
}
