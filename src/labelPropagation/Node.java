package labelPropagation;
import java.io.Serializable;

public class Node implements Serializable, Comparable<Node> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 4010598725887800685L;
	private String realId;
	private String communityId;

	public Node() {
		super();
		// TODO Auto-generated constructor stub
	}

	public Node(String realId, String communityId) {
		super();
		this.realId = realId;
		this.communityId = communityId;
	}

	@Override
	public String toString() {
		return "Node [realId=" + realId + ", communityId=" + communityId + "]";
	}

	public String getCommunityId() {
		return communityId;
	}

	public String getRealId() {
		return realId;
	}

	public void setCommunityId(String communityId) {
		this.communityId = communityId;
	}

	public void setRealId(String realId) {
		this.realId = realId;
	}

	
	@Override
	public int compareTo(Node o) {
		/*
		if (Integer.parseInt(this.getCommunityId())==Integer.parseInt(o.getCommunityId()))
			return 0;
		if (Integer.parseInt(this.getCommunityId())>Integer.parseInt(o.getCommunityId()))
			return 1;
		else
			return -1;*/
		return this.getCommunityId().compareTo(o.getCommunityId());
	}
}
