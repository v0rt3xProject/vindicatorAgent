package ru.v0rt3x.vindicator.agent;

import org.json.simple.JSONObject;

public class AgentTask {

    private String name;
    private JSONObject data;
    private String status;

    private JSONObject result;

    public AgentTask(String name, JSONObject data) {
        this.name = name;
        this.data = data;
        this.status = "Idle";
        this.result = null;
    }

    public String name() {
        return name;
    }

    public JSONObject data() {
        return data;
    }

    public String state() {
        return status;
    }

    public void state(String status) {
        this.status = status;
        VindicatorAgent.getInstance().update();
    }

    public JSONObject result() {
        return result;
    }

    public void result(JSONObject result) {
        this.result = result;
    }

    public void start() {
        status = "Running";
        VindicatorAgent.getInstance().update();
    }

    public void complete() {
        status = "Complete";
        VindicatorAgent.getInstance().update();
    }

    @SuppressWarnings("unchecked")
    public JSONObject toJSON() {
        JSONObject task =  new JSONObject();

        task.put("name", name);
        task.put("state", status);
        task.put("result", result);

        return task;
    }

    public static AgentTask fromJSON(JSONObject task) {
        return new AgentTask(
            (String) task.get("name"),
            (JSONObject) task.get("data")
        );
    }

    public void update(AgentTask task) {
        this.name = task.name();
        this.data = task.data();
    }
}
