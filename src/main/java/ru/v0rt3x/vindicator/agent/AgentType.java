package ru.v0rt3x.vindicator.agent;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface AgentType {

    Logger logger = LoggerFactory.getLogger(AgentType.class);

    void exec(AgentTask task);
    void update(AgentTask task);
    void task_request(JSONObject request);
}
