package ru.v0rt3x.vindicator.agent;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class AgentHandler extends SimpleChannelInboundHandler<String> {

    private static final Logger logger = LoggerFactory.getLogger(AgentHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String message) throws Exception {
        JSONObject request = (JSONObject) new JSONParser().parse(message);

        try {
            VindicatorAgent.getInstance().handle(request);
        } catch (IOException e) {
            logger.error("Unable to handle server response: [{}]: {}", e.getClass().getSimpleName(), e.getMessage());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("Unable to handle server response: [{}]: {}", cause.getClass().getSimpleName(), cause.getMessage());
        ctx.close();
    }
}
