import java.io.Serializable;

public class Node implements Serializable{
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
}
