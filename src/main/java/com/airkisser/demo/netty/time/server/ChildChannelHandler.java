package com.airkisser.demo.netty.time.server;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;


/**
 * Created by AIR on 2016/8/2.
 */
public class ChildChannelHandler extends ChannelInitializer<SocketChannel> {
    protected void initChannel(SocketChannel channel) throws Exception {
        /*
            LineBasedFrameDecoder 和 StringDecoder组合解码原理：
            遍历ByteBuf中的可读字节中是否有“\n”和“\r\n”，
            如果有则将此位置作为结束为止从可读索引到结束位置区间的字节组成一行
         */
        channel.pipeline()
                .addLast(new LineBasedFrameDecoder(1024))
                .addLast(new StringDecoder())
                .addLast(new TimeServerHandler());
    }
}
