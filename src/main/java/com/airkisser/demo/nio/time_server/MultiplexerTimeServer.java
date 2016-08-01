package com.airkisser.demo.nio.time_server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

/**
 * 处理多客户端异步查询时间的服务
 * Created by AIR on 2016/8/1.
 */
public class MultiplexerTimeServer implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(MultiplexerTimeServer.class);
    public static final String QUERY_TIME_ORDER = "QUERY TIME ORDER";
    public static final String CHARSET_NAME = "UTF-8";
    public static final String BAD_ORDER = "BAD ORDER";

    private Selector selector;//多路复用器

    private ServerSocketChannel servChannel;//通道

    private volatile boolean stop;

    /**
     * 初始化多路复用器，绑定端口
     *
     * @param port 端口
     */
    public MultiplexerTimeServer(int port) {
        try {
            //创建复用器
            selector = Selector.open();
            //打开ServerSocketChannel，用于监听客户端
            servChannel = ServerSocketChannel.open();
            //设置连接为非阻塞模式
            servChannel.configureBlocking(false);
            //绑定服务端监听端口
            servChannel.socket().bind(new InetSocketAddress(port), 1024);
            //将ServerSocketChannel注册到Selector多路复用器上，监听ACCEPT事件
            servChannel.register(selector, SelectionKey.OP_ACCEPT);
            logger.info("The Time Server is start in port: " + port);
        } catch (IOException e) {
            e.printStackTrace();
            //System.exit(0)是正常退出程序，而System.exit(1)或者说非0表示非正常退出程序
            System.exit(1);
        }
    }

    public void stop() {
        this.stop = true;
    }

    public void run() {
        // 多路复用器在线程run方法的无限循环体内轮询准备就绪的Key
        while (!stop) {
            try {
                //设置休眠时间为1秒
                selector.select(1000);
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> it = selectionKeys.iterator();
                SelectionKey key = null;
                while (it.hasNext()) {
                    key = it.next();
                    it.remove();
                    try {
                        handleInput(key);
                    } catch (Exception e) {
                        if (key != null) {
                            key.cancel();
                            if (key.channel() != null)
                                key.channel().close();
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // 多路复用器关闭以后，所有注册在上面的Channel和Pipe等资源都会被自动去注册并关闭，
        // 所以不需要重复释放资源
        if (selector != null) {
            try {
                selector.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleInput(SelectionKey key) throws IOException {
        if (key.isValid()) {
            //处理新接入的请求
            if (key.isAcceptable()) {
                // 接受新的连接
                ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
                SocketChannel sc = ssc.accept();
                // 设置客户端连接为非阻塞模式
                sc.configureBlocking(false);
                // 将新连接注册到多路复用器，监听读操作
                sc.register(selector, SelectionKey.OP_READ);
            }
            //读取数据
            if (key.isReadable()) {
                SocketChannel sc = (SocketChannel) key.channel();
                ByteBuffer readBuffer = ByteBuffer.allocate(1024);
                // 异步读取客户端消息到缓冲区
                int readBytes = sc.read(readBuffer);
                if (readBytes > 0) {
                    // flip()
                    // 将当前缓冲区的limit设置为position，position设置为0
                    readBuffer.flip();
                    byte[] bytes = new byte[readBuffer.remaining()];
                    readBuffer.get(bytes);
                    String body = new String(bytes, CHARSET_NAME);
                    logger.info("The Time Server receive order : " + body);
                    String currentTime = QUERY_TIME_ORDER.equalsIgnoreCase(body) ?
                            new Date(System.currentTimeMillis()).toString() : BAD_ORDER;
                    doWrite(sc, currentTime);
                } else if (readBytes < 0) {
                    // 对端链路关闭
                    key.cancel();
                    sc.close();
                } else {
                    ;//读到0字节，忽略
                }
            }
        }
    }

    private void doWrite(SocketChannel channel, String response) throws IOException {
        if (response != null && response.trim().length() > 0) {
            byte[] bytes = response.getBytes();
            ByteBuffer writeBuffer = ByteBuffer.allocate(bytes.length);
            writeBuffer.put(bytes);
            writeBuffer.flip();
            channel.write(writeBuffer);
        }
    }

}
