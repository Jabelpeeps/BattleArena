package mc.alk.tracker.serializers;


import org.bukkit.configuration.ConfigurationSection;

import mc.alk.arena.BattleArena;
import mc.alk.tracker.serializers.SQLSerializer.SQLType;
import mc.alk.util.Log;


public class SQLSerializerConfig {

	public static void configureSQL(SQLSerializer sql, ConfigurationSection cs) {
		String type = cs.getString("type");
		String url = cs.getString("url");
		if (type != null && type.equalsIgnoreCase("sqlite")){
			url = BattleArena.getSelf().getDataFolder().toString();
		}
		configureSQL(sql, type, url, cs.getString("db"),
				cs.getString("port"), cs.getString("username"), cs.getString("password"));
	}

	public static void configureSQL(SQLSerializer sql, String type, String urlOrPath,
			String db, String port, String user, String password) {
		try{
			if (db != null){
				sql.setDB(db);
			}
			if (type == null || type.equalsIgnoreCase("mysql")){
				sql.setType(SQLType.MYSQL);
				if (urlOrPath==null) urlOrPath = "localhost";
				if (port == null)  port = "3306";
				sql.setUrl(urlOrPath);
				sql.setPort(port);
			} else { /// sqlite
				sql.setType(SQLType.SQLITE);
				sql.setUrl(urlOrPath);
			}
			sql.setUsername(user);
			sql.setPassword(password);
			sql.init();
		} catch (Exception e){
			Log.err("Error configuring sql");
			Log.err("Error message = " + e.getMessage());
			e.printStackTrace();
		}
	}

}
