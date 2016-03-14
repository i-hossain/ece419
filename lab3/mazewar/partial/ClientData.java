import java.io.Serializable;
import java.net.InetAddress;

public class ClientData implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * 
	 */
	String name;
	int pid;
	InetAddress ipAddr;
	int port;

	public ClientData(String name, int pid, InetAddress ip, int port) {
		this.name = name;
		this.pid = pid;
		this.ipAddr = ip;
		this.port = port;
	}
	
	public String toString() {
		return name + " pid: " + pid + " - " + ipAddr + ":" + port;
	}
}