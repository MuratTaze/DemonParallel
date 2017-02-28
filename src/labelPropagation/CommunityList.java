package labelPropagation;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;

public class CommunityList<T> implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6010425412583942237L;
	/**
	 * 
	 */

	private ArrayList<Community<T>> communities;

	public CommunityList() {
		super();
		communities = new ArrayList<Community<T>>();
	}

	public void addMember(T vertex, T label) {
		for (Community<T> community : communities) {
			if (label.equals(community.getCommunityId()))
				community.getMembers().add(vertex);
		}
	}

	public void createCommunity(T realId, T communityId) {
		Community<T> community = new Community<T>();
		community.setCommunityId(communityId);
		community.setMembers(new HashSet<T>());
		community.getMembers().add(realId);
		communities.add(community);
	}

	public ArrayList<Community<T>> getCommunities() {
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

	private void writeObject(ObjectOutputStream o) throws IOException {
		o.writeObject(this.communities.size());
		for (Community<T> cm : this.communities) {
			o.writeObject(cm);
		}

	}

	@SuppressWarnings("unchecked")
	private void readObject(ObjectInputStream o) throws IOException, ClassNotFoundException {
		communities = new ArrayList<Community<T>>();
		int numberOfComm = (int) o.readObject();
		for (int i = 0; i < numberOfComm; i++) {
			this.communities.add((Community<T>) o.readObject());
		}
	}

	@Override
	public String toString() {
		return "CommunityList ..\n" + communities + "\n";
	}
}