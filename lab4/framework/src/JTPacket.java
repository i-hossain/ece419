import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


public class JTPacket implements Serializable {
	private static final long serialVersionUID = 1L;
	public String query = "";
	public String hash = "";
	public String result = "";
	public int status;
	
	public static String [] statusText = {"invalid", "submitted", "already_submitted", "never_submitted", "in_progress"};
	public static int submitted = 1;
	public static int already_submitted = 2;
	public static int never_submitted = 3;
	public static int in_progress = 4;
	public static int success = 5;
	public static int failure = 6;
}
