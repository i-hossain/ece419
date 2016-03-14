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
	public String name;
	public int pid;
	public String host;
	public int port;

	public ClientData(String name, int pid, String host, int port) {
		this.name = name;
		this.pid = pid;
		this.host = host;
		this.port = port;
	}
	
	public String toString() {
		return name + " pid: " + pid + " - " + host + ":" + port;
	}
}