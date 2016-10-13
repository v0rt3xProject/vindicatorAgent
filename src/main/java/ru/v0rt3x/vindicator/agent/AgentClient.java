package ru.v0rt3x.vindicator.agent;

import org.json.simple.JSONObject;
import ru.v0rt3x.vindicator.common.AbstractClient;
import ru.v0rt3x.vindicator.exploit.ExploitManager;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

public class AgentClient {

    private AgentClient() {} // Prevent AgentClient instantiation

    private static UUID agentID() {
        return VindicatorAgent.getInstance().getAgentID();
    }

    private static String agentIDString() {
        return (agentID() != null) ? agentID().toString() : null;
    }

    @SuppressWarnings("unchecked")
    public static void register(AbstractClient client) throws InterruptedException {
        JSONObject request = new JSONObject();

        request.put("action", "register");
        request.put("agentId", agentIDString());

        JSONObject agentInfo = new JSONObject();

        try {
            agentInfo.put("hostName", InetAddress.getLocalHost().getHostName());
        } catch (UnknownHostException e) {
            agentInfo.put("hostName", "unknown");
        }

        agentInfo.put("osName", System.getProperty("os.name"));
        agentInfo.put("osArch", System.getProperty("os.arch"));
        agentInfo.put("osVersion", System.getProperty("os.version"));

        agentInfo.put("userName", System.getProperty("user.name"));
        agentInfo.put("userHome", System.getProperty("user.home"));
        agentInfo.put("workingDir", System.getProperty("user.dir"));

        request.put("agentInfo", agentInfo);
        request.put("agentType", VindicatorAgent.getInstance().config().getString("vindicator.agent.type", "executor"));

        client.write(request.toJSONString()+"\n");
    }

    @SuppressWarnings("unchecked")
    public static void update(AbstractClient client, AgentTask task) throws InterruptedException {
        JSONObject request = new JSONObject();

        request.put("action", "update");
        request.put("agentId", agentIDString());

        AgentType agentType = VindicatorAgent.getInstance().getAgentInstance();

        if (agentType != null) {
            if (task == null) {
                agentType.task_request(request);
            } else {
                agentType.update(task);
            }
        }

        request.put("task", (task != null) ? task.toJSON() : null);

        client.write(request.toJSONString()+"\n");
    }

    public static void exec(AbstractClient client, JSONObject request) throws InterruptedException {
        client.write(request.toJSONString()+"\n");
    }
}
