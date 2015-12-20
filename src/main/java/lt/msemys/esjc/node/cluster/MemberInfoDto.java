package lt.msemys.esjc.node.cluster;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class MemberInfoDto {
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter
        .ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
        .withZone(ZoneId.systemDefault());

    public UUID instanceId;

    public Instant timeStamp;
    public VNodeState state;
    public boolean isAlive;

    public String internalTcpIp;
    public int internalTcpPort;
    public int internalSecureTcpPort;

    public String externalTcpIp;
    public int externalTcpPort;
    public int externalSecureTcpPort;

    public String internalHttpIp;
    public int internalHttpPort;

    public String externalHttpIp;
    public int externalHttpPort;

    public long lastCommitPosition;
    public long writerCheckpoint;
    public long chaserCheckpoint;

    public long epochPosition;
    public int epochNumber;
    public UUID epochId;

    public int nodePriority;

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        if (state == VNodeState.Manager) {
            sb.append("MAN ")
                .append(instanceId.toString())
                .append(" <").append(isAlive ? "LIVE" : "DEAD").append("> ")
                .append("[").append(state).append(", ")
                .append(internalHttpIp).append(":").append(internalHttpPort).append(", ")
                .append(externalHttpIp).append(":").append(externalHttpPort).append("] | ")
                .append(TIMESTAMP_FORMATTER.format(timeStamp));
        } else {
            sb.append("VND ")
                .append(instanceId.toString())
                .append(" <").append(isAlive ? "LIVE" : "DEAD").append("> ")
                .append("[").append(state).append(", ")
                .append(internalTcpIp).append(":").append(internalTcpPort).append(", ")
                .append(internalSecureTcpPort > 0 ? internalTcpIp + ":" + internalSecureTcpPort : "n/a").append(", ")
                .append(externalTcpIp).append(":").append(externalTcpPort).append(", ")
                .append(externalSecureTcpPort > 0 ? externalTcpIp + ":" + externalSecureTcpPort : "n/a").append(", ")
                .append(internalHttpIp).append(":").append(internalHttpPort).append(", ")
                .append(externalHttpIp).append(":").append(externalHttpPort).append("] ")
                .append(lastCommitPosition).append("/").append(writerCheckpoint).append("/").append(chaserCheckpoint)
                .append("E").append(epochNumber).append("@").append(epochPosition).append(":").append(epochId.toString())
                .append(" | ").append(TIMESTAMP_FORMATTER.format(timeStamp));
        }

        return sb.toString();
    }
}
