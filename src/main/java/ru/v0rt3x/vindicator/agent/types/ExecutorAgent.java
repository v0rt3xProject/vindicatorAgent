package ru.v0rt3x.vindicator.agent.types;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import ru.v0rt3x.vindicator.agent.AgentTask;
import ru.v0rt3x.vindicator.agent.AgentType;
import ru.v0rt3x.vindicator.agent.AgentTypeInfo;
import ru.v0rt3x.vindicator.exploit.ExploitManager;

import java.util.List;
import java.util.stream.Collectors;

@AgentTypeInfo("executor")
public class ExecutorAgent implements AgentType {

    @Override
    @SuppressWarnings("unchecked")
    public void exec(AgentTask task) {
        JSONObject result = new JSONObject();
        JSONArray flags = new JSONArray();

        List<ExploitManager.Exploit> exploits = ExploitManager.list();
        List<JSONObject> targetData = (List<JSONObject>) task.data().get("target");
        List<String> targetList = targetData.stream()
                .map(target -> (String) target.get("ip"))
                .collect(Collectors.toList());

        for (ExploitManager.Exploit exploit : exploits) {
            List<List<String>> exploitResults = exploit.exec(targetList);

            for (int i = 0; i < targetList.size(); i++) {
                List<String> flagList = exploitResults.get(i);
                String targetName = (String) targetData.get(i).get("name");

                flags.addAll(flagList);

                logger.info(
                    String.format(
                        "Exploit '%s' executed on '%s': %s",
                        exploit.name(), targetName, flagList.size()
                    )
                );
            }
        }

        result.put("flags", flags);
        task.result(result);
    }

    @Override
    public void update(AgentTask task) {

    }

    @Override
    @SuppressWarnings("unchecked")
    public void task_request(JSONObject request) {
        request.put("exploits", ExploitManager.listToJSON());
    }
}
