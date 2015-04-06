package labelPropagation;

import java.util.HashSet;

public class Community<T> {
    private T communityId;
    private HashSet<T> members;

    public Community() {
        super();
    }

    public T getCommunityId() {
        return communityId;
    }

    public HashSet<T> getMembers() {
        return members;
    }

    public void setCommunityId(T communityId) {
        this.communityId = communityId;
    }

    public void setMembers(HashSet<T> members) {
        this.members = members;
    }

    @Override
    public String toString() {
        return "\n    Community [communityId=" + communityId + ", members=" + members
                + "]\n";
    }

}