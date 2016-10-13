package ru.v0rt3x.vindicator.agent;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;
import org.json.simple.JSONObject;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.v0rt3x.vindicator.common.*;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VindicatorAgent {

    private final ArgParser.Args args;
    private final ConfigFile config;

    private final Reflections discoveryHelper;
    private final Map<String, Class<? extends AgentType>> agentTypes = new HashMap<>();

    private final ExecutorService threadPool = Executors.newWorkStealingPool();
    private final Map<String, Queue<?>> queueMap = new HashMap<>();

    private boolean isRunning = true;
    private boolean isReadyForShutDown = false;

    private static final Logger logger = LoggerFactory.getLogger(VindicatorAgent.class);
    private static VindicatorAgent instance;

    private UUID agentID;

    private AbstractClient agentClient;

    private AgentTask task = null;

    public VindicatorAgent(ArgParser.Args commandArgs) throws IOException {
        args = commandArgs;

        config = new ConfigFile(
            new File(args.kwargs("config", "vindicator-agent.ini"))
        );

        File agentIDFile = new File(args.kwargs("agent-id", "vindicator-agent.aid"));
        if (agentIDFile.exists()) {
            BufferedReader agentIdReader = new BufferedReader(new FileReader(agentIDFile));
            agentID = UUID.fromString(agentIdReader.readLine());
            agentIdReader.close();
        }

        discoveryHelper = new Reflections(
            ClasspathHelper.forPackage("ru.v0rt3x"),
            new SubTypesScanner()
        );

        String agentServer = config.getString("vindicator.host", "127.0.0.1");
        Integer agentPort = config.getInt("vindicator.port", 65431);

        agentClient = new AbstractClient(agentServer, agentPort);
        agentClient.setHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();

                pipeline.addLast("frameDecoder", new LineBasedFrameDecoder(4096));
                pipeline.addLast("stringDecoder", new StringDecoder(CharsetUtil.UTF_8));
                pipeline.addLast("stringEncoder", new StringEncoder(CharsetUtil.UTF_8));
                pipeline.addLast(new AgentHandler());
            }
        });

        detectAgentTypes();

        registerShutdownHook();
    }

    public <T> Set<Class<? extends T>> discover(Class<T> parentClass) {
        return discoveryHelper.getSubTypesOf(parentClass);
    }

    private void detectAgentTypes() {
        discover(AgentType.class).stream()
            .filter(agentType -> agentType.isAnnotationPresent(AgentTypeInfo.class))
            .forEach(agentType -> {
                AgentTypeInfo agentTypeInfo = agentType.getAnnotation(AgentTypeInfo.class);
                agentTypes.put(agentTypeInfo.value(), agentType);
            });
    }

    public AgentType getAgentInstance() {
        String type = config.getString("vindicator.agent.type", "executor");
        Class<? extends AgentType> agentType = agentTypes.getOrDefault(type, null);
        if (agentType != null) {
            try {
                return agentType.newInstance();
            } catch (InstantiationException e) {
                logger.error(
                    "Unable to instantiate AgentType instance: [{}]: {}", e.getClass().getSimpleName(), e.getMessage()
                );
            } catch (IllegalAccessException e) {
                logger.error("AgentType is not accessible: {}", e.getMessage());
            }
        }

        return null;
    }

    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(new ShutdownHook()));
    }

    public void executeThread(Runnable runnable) {
        threadPool.submit(runnable);
    }

    public static VindicatorAgent getInstance() {
        return instance;
    }

    private void mainLoop() throws InterruptedException {
        logger.info("Vindicator Agent started");

        executeThread(agentClient);

        agentClient.onConnect(() -> {
            AgentClient.register(agentClient);
        });

        while (!agentClient.isConnected()||(agentID == null)) {
            Thread.sleep(100);
        }

        while (isRunning) {
            update();

            Thread.sleep(config.getLong("vindicator.agent.update_interval", 2000L));
        }

        agentClient.stop();

        logger.info("Vindicator Agent is going to shutdown. Waiting for remaining operations...");

        isReadyForShutDown = true;

        logger.info("Vindicator Agent is down...");
        Runtime.getRuntime().exit(0);
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void shutdown() {
        isRunning = false;
    }

    public boolean isReadyForShutDown() {
        return isReadyForShutDown;
    }

    public <T> boolean createQueue(Class<T> queueType, String queueName) {
        if (!queueMap.containsKey(queueName)) {
            logger.info("Creating queue: [{}] {}", queueType.getSimpleName(), queueName);
            queueMap.put(queueName, new Queue<T>());
            return true;
        }
        return false;
    }

    public Queue<?> getQueue(String queueName) {
        return queueMap.getOrDefault(queueName, null);
    }

    public static void main(String[] cmdline) throws InterruptedException {
        ArgParser.Args args = ArgParser.parse(cmdline);

        try {
            VindicatorAgent.instance = new VindicatorAgent(args);
        } catch (IOException e) {
            logger.error("Unable to open config file: {}", e.getMessage());
            System.exit(1);
        }

        VindicatorAgent.instance.mainLoop();
    }

    public ConfigFile config() {
        return config;
    }

    public UUID getAgentID() {
        return agentID;
    }

    @SuppressWarnings("unchecked")
    public void handle(JSONObject request) throws IOException {
        String action = (String) request.get("action");
        switch (action) {
            case "register":
                UUID id = UUID.fromString((String) request.get("id"));
                if ((agentID == null)||!agentID.equals(id)) {
                    File agentIDFile = new File(args.kwargs("agent-id", "vindicator-agent.aid"));
                    FileWriter agentIDWriter = new FileWriter(agentIDFile);

                    agentIDWriter.write(id.toString());
                    agentIDWriter.flush();
                    agentIDWriter.close();

                    agentID = id;
                }

                break;
            case "update":
                if (request.get("task") != null) {
                    AgentTask task = AgentTask.fromJSON((JSONObject) request.get("task"));
                    if (this.task != null) {
                        this.task.update(task);
                    } else {
                        logger.info("Got task: {}", task.toJSON());
                        executeThread(new AgentTaskExecutor(task));
                    }
                }
                break;
            default:
                logger.info("Unknown action: {}", action);
                break;
        }
    }

    public void setTask(AgentTask task) {
        this.task = task;
    }

    public AgentTask getTask() {
        return this.task;
    }

    public void update() {
        try { AgentClient.update(agentClient, task); } catch(InterruptedException ignored) {}
    }

    public void exec(JSONObject request) {
        try { AgentClient.exec(agentClient, request); } catch(InterruptedException ignored) {}
    }
}
