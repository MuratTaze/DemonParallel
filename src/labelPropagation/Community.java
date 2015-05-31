package labelPropagation;

import java.io.Serializable;
import java.util.HashSet;

public class Community<T> implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private T communityId;
    private HashSet<T> members;
    private HashSet<Community<T>> dependencyList;

    public HashSet<Community<T>> getDependencyList() {
        return dependencyList;
    }

    public void setDependencyList(HashSet<Community<T>> dependencyList) {
        this.dependencyList = dependencyList;
    }

    public Community() {
        super();
        dependencyList = new HashSet<Community<T>>();

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
        return "\n    Community [communityId=" + communityId + ", members="
                + members + "]\n";
    }

}