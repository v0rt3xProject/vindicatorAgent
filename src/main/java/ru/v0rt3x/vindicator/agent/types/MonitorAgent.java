package ru.v0rt3x.vindicator.agent.types;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.pcap4j.core.NotOpenException;
import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.TcpPacket;

import ru.v0rt3x.vindicator.agent.AgentTask;
import ru.v0rt3x.vindicator.agent.AgentType;
import ru.v0rt3x.vindicator.agent.AgentTypeInfo;
import ru.v0rt3x.vindicator.agent.VindicatorAgent;

import java.io.EOFException;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@AgentTypeInfo("monitor")
public class MonitorAgent implements AgentType {

    @Override
    @SuppressWarnings("unchecked")
    public void exec(AgentTask task) {
        try {
            String iFace = VindicatorAgent.getInstance().config().getString("vindicator.monitor.interface", "eno1");
            PcapHandle pCap;

            while (VindicatorAgent.getInstance().isRunning()) {
                pCap = new PcapHandle.Builder(iFace).build();

                try {
                    while (VindicatorAgent.getInstance().isRunning()) {
                        Packet packet = pCap.getNextPacketEx();

                        String tgtHost = (String) task.data().get("host");

                        String srcHost = packet.get(IpV4Packet.class).getHeader().getSrcAddr().getHostAddress();
                        String dstHost = packet.get(IpV4Packet.class).getHeader().getDstAddr().getHostAddress();

                        Integer srcPort = packet.get(TcpPacket.class).getHeader().getSrcPort().valueAsInt();
                        Integer dstPort = packet.get(TcpPacket.class).getHeader().getDstPort().valueAsInt();

                        Long sequenceId = packet.get(TcpPacket.class).getHeader().getSequenceNumberAsLong();

                        List<Long> servicePorts = ((List<JSONObject>) task.data().get("service")).stream()
                            .map(service -> (Long) service.get("port"))
                            .collect(Collectors.toList());

                        for (Long tgtPort: servicePorts) {
                            boolean srcIsTarget = srcHost.equals(tgtHost) && (new Long(srcPort).equals(tgtPort));
                            boolean dstIsTarget = dstHost.equals(tgtHost) && (new Long(dstPort).equals(tgtPort));

                            if (srcIsTarget || dstIsTarget) {
                                String tgtName = (String) (
                                    (JSONObject) (
                                        (JSONArray) task.data().get("service")).get(servicePorts.indexOf(tgtPort)
                                    )
                                ).get("name");

                                if (srcIsTarget) {
                                    logger.info(String.format(
                                        "Service<%s:%d>(%s): --{%s}-> DST[%s:%d]",
                                        tgtHost, tgtPort, tgtName, sequenceId, dstHost, dstPort
                                    ));
                                } else {
                                    logger.info(String.format(
                                        "Service<%s:%d>(%s): <-{%s}-- SRC[%s:%d]",
                                        tgtHost, tgtPort, tgtName, sequenceId, srcHost, srcPort
                                    ));
                                }

                                logPacket(packet, tgtName, dstIsTarget);
                            }
                        }
                    }
                } catch (TimeoutException e) {
                    logger.error("Handle reached timeout. Re-opening handle.");
                } catch (EOFException e) {
                    logger.error("Unexpected EOF. Re-opening handle.");
                }

                pCap.close();
            }

        } catch (PcapNativeException e) {
            logger.error("PCAPError: [{}] {}", e.getClass().getSimpleName(), e.getMessage());
        } catch (NotOpenException e) {
            logger.error("Unable to listen on closed handle: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error: [{}]: {}", e.getClass().getSimpleName(), e.getMessage());
        }
    }

    @Override
    public void update(AgentTask task) {

    }

    @Override
    public void task_request(JSONObject request) {

    }

    @SuppressWarnings("unchecked")
    private void logPacket(Packet packet, String service, boolean direction) {
        JSONObject request = new JSONObject();

        IpV4Packet.IpV4Header ipV4Header = packet.get(IpV4Packet.class).getHeader();
        TcpPacket.TcpHeader tcpHeader = packet.get(TcpPacket.class).getHeader();

        JSONObject client = new JSONObject();
        client.put("host", (direction ? ipV4Header.getSrcAddr() : ipV4Header.getDstAddr()).getHostAddress());
        client.put("port", (direction ? tcpHeader.getSrcPort() : tcpHeader.getDstPort()).valueAsInt());

        Long sequenceId = tcpHeader.getSequenceNumberAsLong();

        request.put("action", "monitor");
        request.put("service", service);
        request.put("direction", direction);
        request.put("client", client);
        request.put("sequenceId", sequenceId);
        request.put("time", System.currentTimeMillis());

        Packet payload = packet.get(TcpPacket.class).getPayload();
        if (payload != null) {
            request.put("data", new String(Base64.getEncoder().encode(payload.getRawData())));
        }

        VindicatorAgent.getInstance().exec(request);
    }
}
