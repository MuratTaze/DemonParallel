package labelPropagation;

import java.io.Serializable;
import java.util.HashSet;





public class Community<T> implements Serializable, Comparable<Community<T>> {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private T communityId;
    private HashSet<T> members;
    private HashSet<Community<T>> dependencyList;
    private int index;

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public HashSet<Community<T>> getDependencyList() {
        return dependencyList;
    }

    public void setDependencyList(HashSet<Community<T>> dependencyList) {
        this.dependencyList = dependencyList;
    }

    public Community() {
        super();
        dependencyList = new HashSet<Community<T>>(100);

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

    public int compareTo(Community<T> o) {
        // TODO Auto-generated method stub
        return this.members.size() - o.members.size();
    }

}