package labelPropagation;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashSet;

public class Community<T> implements Serializable, Comparable<Community<T>> {
	/**
	 * 
	 */

	/**
	 * 
	 */

	private T communityId;
	private HashSet<T> members;
	private HashSet<T> dependencyList;
	private int index;

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public HashSet<T> getDependencyList() {
		return dependencyList;
	}

	public void setDependencyList(HashSet<T> dependencyList) {
		this.dependencyList = dependencyList;
	}

	public Community() {
		super();
		dependencyList = new HashSet<T>();

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

	private void writeObject(ObjectOutputStream o) throws IOException {
		if (members != null) {
			o.writeObject(members.size());

			for (T member : members) {
				o.writeObject(member);
			}
		} else {
			o.writeObject(0);
		}
		
		o.writeObject(communityId);
		o.writeObject(index);
	}

	@SuppressWarnings("unchecked")
	private void readObject(ObjectInputStream o) throws IOException, ClassNotFoundException {
		int numberOfMembers = (int) o.readObject();
		this.members = new HashSet<T>(numberOfMembers);
		for (int i = 0; i < numberOfMembers; i++) {
			this.members.add(((T) (o.readObject())));
		}

		this.communityId = (T) o.readObject();
		this.index = (int) o.readObject();
	}

	@Override
	public String toString() {
		return "\n    Community [communityIndex=" + index + ", members=" + members + "]\n";
	}

	public int compareTo(Community<T> o) {
		return this.members.size() - o.members.size();
	}

}