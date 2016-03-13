
public class Debug{
    public final static boolean debug = true;    

    public static void log(String tag, String text) {
    	if(Debug.debug)
    		System.out.println(tag + ": " + text);
    }
}
