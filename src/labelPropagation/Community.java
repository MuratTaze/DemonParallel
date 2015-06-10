package labelPropagation;

import java.io.Serializable;

import net.ontopia.utils.CompactHashSet;

public class Community<T> implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private T communityId;
    private CompactHashSet<T> members;
    private CompactHashSet<Community<T>> dependencyList;
    private int index;
    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public CompactHashSet<Community<T>> getDependencyList() {
        return dependencyList;
    }

    public void setDependencyList(CompactHashSet<Community<T>> dependencyList) {
        this.dependencyList = dependencyList;
    }

    public Community() {
        super();
        dependencyList = new CompactHashSet<Community<T>>(100);

    }

    public T getCommunityId() {
        return communityId;
    }

    public CompactHashSet<T> getMembers() {
        return members;
    }

    public void setCommunityId(T communityId) {
        this.communityId = communityId;
    }

    public void setMembers(CompactHashSet<T> members) {
        this.members = members;
    }

    @Override
    public String toString() {
        return "\n    Community [communityId=" + communityId + ", members="
                + members + "]\n";
    }

}