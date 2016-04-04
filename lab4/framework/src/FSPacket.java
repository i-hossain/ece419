import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


public class FSPacket implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public int partitionID = 0;
	public List<String> list = new ArrayList<String>();

	
	public FSPacket() {
		partitionID = 0;
		list = null;
	}
	
	public FSPacket(int ID) {
		partitionID = ID;
	}
	
	public FSPacket(int ID, List<String> list) {
		partitionID = ID;
		this.list = list;
	}
	
	public int getID() {
		return partitionID;
	}
	
	public List<String> getList() {
		return list;
	}
}
