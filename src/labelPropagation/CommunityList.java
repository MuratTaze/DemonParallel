package labelPropagation;

import java.io.Serializable;
import java.util.HashSet;
import java.util.concurrent.CopyOnWriteArrayList;

public class CommunityList<T> implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private CopyOnWriteArrayList<Community<T>> communities;

    public CommunityList() {
        super();
        communities = new CopyOnWriteArrayList<Community<T>>();
    }

    public void addMember(Vertex<T> member,T label) {
        for (Community<T> community : communities) {
            if (label.equals(community.getCommunityId()))
                community.getMembers().add(member.getValue());
        }
    }

    public void createCommunity(T realId,T communityId) {
        Community<T> community = new Community<T>();
        community.setCommunityId(communityId);
        community.setMembers(new HashSet<T>());
        community.getMembers().add(realId);
        communities.add(community);
    }

    public CopyOnWriteArrayList<Community<T>> getCommunities() {
        return communities;
    }

    public boolean hasCommunity(T label) {
        for (Community<T> community : communities) {
            if (label.equals(community.getCommunityId()))
                return true;
        }

        return false;
    }

    public void setCommunities(CopyOnWriteArrayList<Community<T>> communities) {
        this.communities = communities;
    }

    @Override
    public String toString() {
        return "CommunityList ..\n" + communities+"\n" ;
    }

}