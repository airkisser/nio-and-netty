package com.airkisser.demo.netty.time_server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * Created by AIR on 2016/8/2.
 */
public class TimeServer {

    public void bind(int port) throws Exception{
        // 配置NIO服务端的线程组
        // 第一个线程组用于服务端接受客户端连接，第二个线程组用于进行SocketChannel的读写
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try{
            // 用于启动NIO服务端的辅助启动类，目的是降低服务端的开发复杂度
            ServerBootstrap bootstrap = new ServerBootstrap();
            // 配置NioServerSocketChannel的TCP参数
            bootstrap.group(bossGroup,workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG,1024)
                    //绑定I/O事件的处理类，Handler类用于处理网络I/O事件，例如对事件进行编解码
                    .childHandler(new ChildChannelHandler());
            // bind绑定监听端口，sync同步阻塞等待绑定操作完成
            // ChannelFuture用于异步操作的通知回调
            ChannelFuture future = bootstrap.bind(port).sync();
            // 使用以下方法进行阻塞，等待服务端链路关闭之后main函数才推出
            future.channel().closeFuture().sync();
        }finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {
        int port = 8080;
        if(args != null && args.length > 0){
            try{
                port = Integer.valueOf(args[0]);
            }catch (NumberFormatException e){
                e.printStackTrace();
            }
        }
        new TimeServer().bind(port);
    }

}
