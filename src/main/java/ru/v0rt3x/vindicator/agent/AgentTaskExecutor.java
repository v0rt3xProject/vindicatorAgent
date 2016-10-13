package ru.v0rt3x.vindicator.agent;


public class AgentTaskExecutor implements Runnable {

    private AgentTask task;

    public AgentTaskExecutor(AgentTask task) {
        this.task = task;
    }

    @Override
    public void run() {
        VindicatorAgent.getInstance().setTask(task);
        task.start();

        AgentType agentTypeInstance = VindicatorAgent.getInstance().getAgentInstance();

        if (agentTypeInstance != null) {
            agentTypeInstance.exec(task);
        }

        task.complete();
        VindicatorAgent.getInstance().setTask(null);
    }
}