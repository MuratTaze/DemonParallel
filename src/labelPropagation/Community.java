package labelPropagation;

import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

public class Community<T> implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private T communityId;
    private HashSet<T> members;
    private Queue<Community<T>> dependencyList;
        public Queue<Community<T>> getDependencyList() {
        return dependencyList;
    }

    public void setDependencyList(Queue<Community<T>> dependencyList) {
        this.dependencyList = dependencyList;
    }

    public Community() {
        super();
        dependencyList = new LinkedList<Community<T>>();
       
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