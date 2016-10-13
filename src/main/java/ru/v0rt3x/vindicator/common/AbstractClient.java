package ru.v0rt3x.vindicator.common;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbstractClient implements Runnable {

    private String serverHost;
    private int serverPort;

    private ChannelHandler handler;
    private Channel channel;

    private EventLoopGroup client = new NioEventLoopGroup();

    private OnConnectCallBack onConnect;
    private boolean connected = false;

    private static Logger logger = LoggerFactory.getLogger(AbstractClient.class);

    public AbstractClient(String bindHost, int bindPort) {
        serverHost = bindHost;
        serverPort = bindPort;
    }

    public interface OnConnectCallBack {
        void onConnect() throws InterruptedException;
    }

    @Override
    public void run() {
        try {
            Bootstrap b = new Bootstrap();
            b.group(client)
                .channel(NioSocketChannel.class)
                .handler(handler);

            ChannelFuture f = b.connect(serverHost, serverPort).sync().addListener(future -> {
                onConnect.onConnect();
                connected = future.isSuccess();
            });

            channel = f.channel();

            logger.info("AbstractClient started on {}:{}", serverHost, serverPort);

            channel.closeFuture().sync();
        } catch (InterruptedException ignored) {}
    }

    public void setHandler(ChannelHandler handler) {
        this.handler = handler;
    }

    public void write(Object message) throws InterruptedException {
        channel.writeAndFlush(message).sync();
    }

    public void stop() {
        logger.info("AbstractClient on {}:{} is going to shutdown", serverHost, serverPort);
        client.shutdownGracefully();
    }

    public boolean isConnected() {
        return connected;
    }

    public void onConnect(OnConnectCallBack callBack) {
        onConnect = callBack;
    }
}
