package com.airkisser.demo.nio.time_client;

/**
 * Created by AIR on 2016/8/2.
 */
public class TimeClient {


    public static void main(String[] args) {
        int port = 8080;
        if(args != null && args.length > 0){
            try {
                port = Integer.valueOf(args[0]);
            }catch (NumberFormatException e){
                e.printStackTrace();
            }
        }
        new Thread(new TimeClientHandle(TimeClientHandle.LOCAL_HOST,port),"TimeClient-001").start();
    }

}
