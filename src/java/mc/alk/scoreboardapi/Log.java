package mc.alk.scoreboardapi;



public class Log {

	private static final Character COLOR_MC_CHAR = (char) 167;

	public static void info(String msg){
		System.out.println(colorChat(msg));
	}
	public static void config(String msg){
		info(msg);
	}
	public static void warn(String msg){
		System.err.println(colorChat(msg));
	}
	public static void err(String msg){
		System.err.println(colorChat(msg));
	}

	public static String colorChat(String msg) {
		return msg.replace('&', COLOR_MC_CHAR);
	}
	public static void debug(String string) {
		System.out.println(colorChat(string));
	}
	static public void printStackTrace(){
		for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
			System.out.println(ste);}
	}

}
