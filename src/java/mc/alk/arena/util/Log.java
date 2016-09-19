package mc.alk.arena.util;

import java.util.logging.Logger;

import org.bukkit.Bukkit;

import mc.alk.arena.Defaults;

public class Log {
	private static Logger log;
    public static void setLogger( Logger _log ) {
        log = _log;
    }

    public static void info(String msg){
		if (msg == null) return;
		try{
			MessageUtil.sendMessage( Bukkit.getConsoleSender(), msg );
		} catch (Exception e){
			if (log != null)
				log.info( MessageUtil.colorChat(msg) );
			else
				System.out.println( MessageUtil.colorChat(msg) );
		}
        NotifierUtil.notify("info", msg);
    }

	public static void warn(String msg){
		if (msg == null) return;
        try{
            MessageUtil.sendMessage(Bukkit.getConsoleSender(), msg );
        } catch (Exception e){
            if (log != null)
                log.warning( MessageUtil.colorChat(msg) );
            else
                System.out.println( MessageUtil.colorChat(msg) );
        } 
        NotifierUtil.notify("warn", msg);
	}

	public static void err(String msg){
		if (msg == null) return;
        try{
            MessageUtil.sendMessage( Bukkit.getConsoleSender(), msg );
        } catch (Exception e){
            if (log != null)
                log.severe( MessageUtil.colorChat(msg) );
            else
                System.err.println( MessageUtil.colorChat(msg) );
        }
		NotifierUtil.notify("errors", msg);
	}

	public static void debug(String msg){
		msg = MessageUtil.colorChat(msg);
		if (Defaults.DEBUG){
			try{
				MessageUtil.sendMessage( Bukkit.getConsoleSender(), msg );
			} catch (Exception e){
				System.out.println(msg);
			}
		}
		if (NotifierUtil.hasListener("debug")){
			NotifierUtil.notify("debug", msg);}
	}

	public static void printStackTrace(Throwable e) {
		e.printStackTrace();
		if (NotifierUtil.hasListener("errors")){
			NotifierUtil.notify("errors", e);}
	}

    public static void trace(int id, String msg) {
        info(msg);
//        NotifierUtil.notify(id, colorChat(msg));
    }
}
