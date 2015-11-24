package labelPropagation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;





public class CommunityList<T> implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private ArrayList<Community<T>> communities;

    public CommunityList() {
        super();
        communities = new ArrayList<Community<T>>();
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

    public  ArrayList<Community<T>> getCommunities() {
        return communities;
    }

    public boolean hasCommunity(T label) {
        for (Community<T> community : communities) {
            if (label.equals(community.getCommunityId()))
                return true;
        }

        return false;
    }

    public void setCommunities(ArrayList<Community<T>> communities) {
        this.communities = communities;
    }

    @Override
    public String toString() {
        return "CommunityList ..\n" + communities+"\n" ;
    }
}