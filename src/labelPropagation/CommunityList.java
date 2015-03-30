package labelPropagation;

import java.util.HashSet;
import java.util.concurrent.CopyOnWriteArrayList;

public class CommunityList<T> {

    private CopyOnWriteArrayList<Community<T>> communities;

    public CommunityList() {
        super();
        communities = new CopyOnWriteArrayList<Community<T>>();
    }

    public void addMember(Vertex<T> member) {
        for (Community<T> community : communities) {
            if (member.getLabel().equals(community.getCommunityId()))
                community.getMembers().add(member.getValue());
        }
    }

    public void createCommunity(Vertex<T> member) {
        Community<T> community = new Community<T>();
        community.setCommunityId(member.getLabel());
        community.setMembers(new HashSet<T>());
        community.getMembers().add(member.getValue());
        communities.add(community);
    }

    public CopyOnWriteArrayList<Community<T>> getCommunities() {
        return communities;
    }

    public boolean hasCommunity(Vertex<T> vertex) {

        for (Community<T> community : communities) {
            if (vertex.getLabel().equals(community.getCommunityId()))
                return true;
        }

        return false;
    }

    public void setCommunities(CopyOnWriteArrayList<Community<T>> communities) {
        this.communities = communities;
    }

    @Override
    public String toString() {
        return "CommunityList [communities=" + communities + "]";
    }

}