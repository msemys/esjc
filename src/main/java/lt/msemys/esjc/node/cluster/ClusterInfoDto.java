package lt.msemys.esjc.node.cluster;

import java.util.List;

public class ClusterInfoDto {
    public List<MemberInfoDto> members;

    public ClusterInfoDto() {
    }

    public ClusterInfoDto(List<MemberInfoDto> members) {
        this.members = members;
    }
}
