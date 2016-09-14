package mc.alk.arena.tracker;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.dbcp.DriverManagerConnectionFactory;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingDataSource;
import org.apache.commons.pool.impl.GenericKeyedObjectPoolFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.craftbukkit.libs.jline.internal.Log;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import mc.alk.arena.BattleArena;
import mc.alk.arena.Defaults;

public class SQLSerializer {
    public static final String version = "1.5";

    protected static final boolean DEBUG = false;
    protected static final boolean DEBUG_UPDATE = false;

	protected static final int TIMEOUT = 4; 

	/**
	 * Valid SQL Types
	 * @author alkarin
	 *
	 */
	@AllArgsConstructor @Getter
	public enum SQLType {
		MYSQL( "MySQL", "com.mysql.jdbc.Driver" ), 
		SQLITE( "SQLite", "org.sqlite.JDBC" );
	    @Getter String name, driver;
	};
	
	private DataSource ds ;

	@Getter protected String DB = "minecraft";
	@Getter @Setter protected SQLType type = SQLType.MYSQL;
	@Getter @Setter protected String url = "localhost";
	@Getter @Setter protected String port = "3306";
	@Getter @Setter protected String username = "root";
	@Getter @Setter protected String password = "";

	private String create_database = "CREATE DATABASE IF NOT EXISTS `" + DB + "`";

	public void setDB(String dB) {
		DB = dB;
		create_database = "CREATE DATABASE IF NOT EXISTS `" + DB + "`";
	}

	protected class RSCon {
		public ResultSet rs;
		public Connection con;
	}

	protected void close(RSCon rscon) {
		try {
			rscon.rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public Connection getConnection() throws SQLException {

		if ( ds == null ) 
			throw new SQLException( "Connection is null.  Did you intiliaze your SQL connection?"); 
		try {
			Connection con = ds.getConnection();
			con.setAutoCommit(true);
			return con;
		} catch (SQLException e1) {
				e1.printStackTrace();
			return null;
		}
	}

	public void closeConnection(RSCon rscon) {
		if (rscon == null || rscon.con == null) return;
		
		try {rscon.con.close();} catch (SQLException e) {}
	}

	public static DataSource setupDataSource( String connectURI, String username, String password, 
	                                                                    int minIdle, int maxActive ) {

	    GenericObjectPool.Config poolConf = new GenericObjectPool.Config();
	    poolConf.lifo = false;
	    poolConf.maxActive = maxActive;
	    	    
		if ( minIdle != -1 )
			poolConf.minIdle = minIdle;
		
		PoolableConnectionFactory factory = new PoolableConnectionFactory( 
		        new DriverManagerConnectionFactory( connectURI,username, password ), 
		        new GenericObjectPool( null, poolConf ),
		        new GenericKeyedObjectPoolFactory( null ), 
				"select 1", false, true );
	
		return new PoolingDataSource( factory.getPool() );
	}

	protected boolean createTable( String tableName, String sql_create_table, String... sql_updates ) {
	    
		boolean exists = false;
		if ( type == SQLType.SQLITE ) {
			exists = getBoolean( "SELECT count(name) FROM sqlite_master WHERE type='table' AND name='" + tableName + "';" );
		} 
		else {
			List<Object> objs = getObjects( "SHOW TABLES LIKE '" + tableName + "';" );
			exists = objs != null && objs.size() == 1;
		}
		if (DEBUG) Log.info( "table " + tableName + " exists =" + exists );

		if ( exists ) return true; 

		/// Create our table and index
		String strStmt = sql_create_table;
		int result = 0;
		
		try ( Connection con = getConnection();
              Statement st = con.createStatement(); ) {
		    
			result = st.executeUpdate( strStmt );			
			if (DEBUG) Log.info( "Created Table with stmt=" + strStmt);
			
		    if ( sql_updates != null ) {
	            for ( String sql_update : sql_updates ) {
	                if (sql_update == null) continue;
	                
	                strStmt = sql_update;
	                
	                try {
	                    result = st.executeUpdate(strStmt);
	                    if (DEBUG) Log.info( "Update Table with stmt=" + strStmt);
	                } 
	                catch (SQLException e) {
	                    Log.error( "Failed in updating Table " +strStmt + " result=" + result);
	                    e.printStackTrace();
	                    return false;
	                }
	            }
	        }
		} catch (SQLException e) {
		    Log.error( "Failed in creating Table " + strStmt + " result=" + result);
			e.printStackTrace();
			return false;
		}

		return true;
	}

	/**
	 * Check to see whether the database has a particular column
	 * @param table
	 * @param column
	 * @return Boolean: whether the column exists
	 */
	protected Boolean hasColumn( String table, String column ) {
		Boolean b = null;
		
		switch (type){
		    case MYSQL:
    			b = getBoolean( 2, 
    			        "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = ? "
    			        + "AND TABLE_NAME = ? AND COLUMN_NAME = ?", DB, table, column );
    			return b == null ? false : b;
    		
    		case SQLITE:
				return getBoolean( 2, "SELECT COUNT(" + column + ") FROM '" + table + "'" ) == null ? false : true;
		}
		return false;
	}

	protected boolean hasTable( String tableName ) {
		boolean exists;
		if ( type == SQLType.SQLITE )
			exists = getBoolean( "SELECT count(name) FROM sqlite_master WHERE type='table' AND name='" + tableName + "'" );
		else {
			List<Object> objs = getObjects( "SHOW TABLES LIKE '" + tableName + "';" );
			exists = ( objs != null && objs.size() == 1 );
		}
		return exists;
	}

	protected RSCon executeQuery( String strRawStmt, Object... varArgs ) {
		return executeQuery( TIMEOUT, strRawStmt, varArgs );
	}

	/**
	 * Execute the given query
	 * @param strRawStmt
	 * @param varArgs
	 * @return
	 */
	protected RSCon executeQuery( Integer timeoutSeconds, String strRawStmt, Object... varArgs ) {
		RSCon rscon = null;

        
		try (   Connection con = getConnection();
		        PreparedStatement ps = getStatement( strRawStmt, con, varArgs ) ) {
		    
			if (DEBUG) System.out.println( "Executing =" + ps + " timeout=" + timeoutSeconds + " raw=" + strRawStmt );
			ps.setQueryTimeout(timeoutSeconds);
			ResultSet rs = ps.executeQuery();
			rscon = new RSCon();
			rscon.con = con;
			rscon.rs = rs;	
		} 
		catch ( SQLException e ) {
			if ( Defaults.DEBUG_TRACKING ) {
				System.err.println( "Couldnt execute query " + strRawStmt );
				
				for ( int i = 0; i < varArgs.length; i++ ) {
					System.err.println("   arg[" + i + "] = " + varArgs[i] ); 
				}
				e.printStackTrace();
			}
		}
		return rscon;
	}

//	private void executeUpdate( final boolean async, final String strRawStmt, final Object... varArgs ) {
//		if ( async ) {
//			new Thread( new Runnable() {
//				@Override
//                public void run() {
//					try{
//						executeUpdate(strRawStmt, varArgs);
//					} catch (Exception e){
//						e.printStackTrace();
//					}
//				}
//			}).start();
//		} else {
//			try{
//				executeUpdate(strRawStmt, varArgs);
//			} catch (Exception e){
//				e.printStackTrace();
//			}
//		}
//	}

	protected int executeUpdate( String strRawStmt, Object... varArgs ) {
		int result= -1;

		try ( Connection con = getConnection();
		      PreparedStatement ps = getStatement(strRawStmt,con,varArgs); ) {
		    
			if (DEBUG) System.out.println("Executing   =" + ps.toString() +"  raw="+strRawStmt);
			
			result = ps.executeUpdate();
		} 
		catch (Exception e) {
			System.err.println("Couldnt execute update "  + strRawStmt );
			e.printStackTrace();
		} 
		return result;
	}

	protected void executeBatch( boolean async, String updateStatement, List<List<Object>> batch) {
		
	    if (async) 
			new Thread( () -> executeBatch(updateStatement, batch) ).start();
		else 
			executeBatch(updateStatement, batch);
	}

	protected void executeBatch( String updateStatement, List<List<Object>> batch ) {
		
	    try ( Connection con = getConnection(); 
	          PreparedStatement ps  = con.prepareStatement( updateStatement ); ) {
	    
        con.setAutoCommit(false);          
		
		for ( List<Object> update: batch ) {
			for ( int i = 0; i < update.size(); i++ ) {
				if (DEBUG_UPDATE) Log.info( i + " = " + update.get(i) );
				
				ps.setObject( i + 1, update.get(i) );
			}
			if (DEBUG) Log.info( "Executing   =" + ps.toString() + "  raw=" + updateStatement );
			ps.addBatch();			
		}
		ps.executeBatch();
		con.commit();
		
	    } catch ( SQLException e ) {
	        e.printStackTrace();
	    } 
	}

	private PreparedStatement getStatement( String strRawStmt, Connection con, Object... varArgs ) {

		PreparedStatement ps = null;
		try{
			ps = con.prepareStatement(strRawStmt);
			for (int i=0;i<varArgs.length;i++){
				if (DEBUG_UPDATE) System.out.println(i+" = " + varArgs[i]);
				ps.setObject(i+1, varArgs[i]);
			}
		} catch (Exception e){
			if ( Defaults.DEBUG_TRACKING ) {
				System.err.println("Couldnt prepare statment "  + ps +"   rawStmt='" + strRawStmt +"' args="+varArgs);
				for (int i=0;i< varArgs.length;i++){
					System.err.println("   arg["+ i+"] = " + varArgs[i]);}
				e.printStackTrace();
			}
		}
		return ps;
	}


	public Double getDouble(String query, Object... varArgs){
		RSCon rscon = executeQuery(query,varArgs);
		if (rscon == null || rscon.con == null)
			return null;
		try {
			ResultSet rs = rscon.rs;
			while (rs.next()){
				return rs.getDouble(1);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally{
			try{rscon.con.close();}catch(Exception e){}
		}
		return null;
	}

	public Integer getInteger(String query, Object... varArgs){
		RSCon rscon = executeQuery(query,varArgs);
		if (rscon == null || rscon.con == null)
			return null;
		try {
			ResultSet rs = rscon.rs;
			while (rs.next()){
				return rs.getInt(1);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}finally{
			try{rscon.con.close();}catch(Exception e){}
		}
		return null;
	}

	public Short getShort(String query, Object... varArgs){
		RSCon rscon = executeQuery(query,varArgs);
		if (rscon == null || rscon.con == null)
			return null;
		try {
			ResultSet rs = rscon.rs;
			while (rs.next()){
				return rs.getShort(1);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}finally{
			try{rscon.con.close();}catch(Exception e){}
		}
		return null;
	}

	public Long getLong(String query, Object... varArgs){
		RSCon rscon = executeQuery(query,varArgs);
		if (rscon == null || rscon.con == null)
			return null;
		try {
			ResultSet rs = rscon.rs;
			while (rs.next()){
				return rs.getLong(1);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}finally{
			try{rscon.con.close();}catch(Exception e){}
		}
		return null;
	}

	public Boolean getBoolean(String query, Object... varArgs){
		return getBoolean( TIMEOUT, query,varArgs);
	}

	protected Boolean getBoolean( Integer timeoutSeconds, String query, Object... varArgs){
		RSCon rscon = executeQuery( timeoutSeconds, query,varArgs);
		if (rscon == null || rscon.con == null)
			return null;
		try {
			ResultSet rs = rscon.rs;
			while (rs.next()){
				int i = rs.getInt(1);
				if ( i == 0 )
					return null;
				return i > 0;
			}
		} catch (SQLException e) {
			if ( Defaults.DEBUG_TRACKING )
				e.printStackTrace();
		}finally{
			try{rscon.con.close();}catch(Exception e){}
		}
		return null;
	}

	public String getString(String query, Object... varArgs){
		RSCon rscon = executeQuery(query,varArgs);
		if (rscon == null || rscon.con == null)
			return null;
		try {
			ResultSet rs = rscon.rs;
			while (rs.next()){
				return rs.getString(1);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}finally{
			try{rscon.con.close();}catch(Exception e){}
		}
		return null;
	}

	public List<Object> getObjects(String query, Object... varArgs){
		RSCon rscon = executeQuery(query,varArgs);
		if (rscon == null || rscon.con == null)
			return null;
		try {
			ResultSet rs = rscon.rs;
			while (rs.next()){
				int nCol = rs.getMetaData().getColumnCount();
				List<Object> objs = new ArrayList<>(nCol);
				for ( int i = 0; i < nCol; i++ ) {
					objs.add( rs.getObject( i + 1 ) );
				}
				return objs;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}finally{
			try{rscon.con.close();}catch(Exception e){}
		}
		return null;
	}

	protected ArrayList<Map<String,Object>> convertToResult(RSCon rscon){
		ArrayList<Map<String,Object>> values = new ArrayList<>();
		try {
			ResultSet rs = rscon.rs;
			if (rs == null)
				return null;
			ResultSetMetaData rmd = rs.getMetaData();
			while (rs.next()){
				Map<String,Object> row = new HashMap<>();
				for (int i=1;i<rmd.getColumnCount()+1;i++){
					row.put(rmd.getColumnName(i), rs.getObject(i));
				}
				values.add(row);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return values;
	}

    public void configureSQL( ConfigurationSection cs ) {
        
    	String _type = cs.getString("type");
    	
    	if ( _type == null ) return;
    	
    	setDB( cs.getString( "db", "minecraft" ) );
    	
    	if ( _type.equalsIgnoreCase("sqlite") ) {
    		url = BattleArena.getSelf().getDataFolder().toString();
            type = SQLType.SQLITE;    		
    	}
    	else if ( _type.equalsIgnoreCase( "mysql" ) ) {
    	    type = SQLType.MYSQL;
    	    url = cs.getString( "url", "localhost" );
    	    port = cs.getString( "port", "3306" );
    	    
    	}
    	username = cs.getString( "username", "root" );
    	password = cs.getString( "password", "" );
    	
        try {
            Class.forName(type.getDriver());
            if (DEBUG) Log.info( "Got Driver " + type.getDriver());
        } 
        catch (ClassNotFoundException e1) {
            Log.error( "Failed getting driver " + type.getDriver());
            e1.printStackTrace();
            return;
        }
        String datasourceString = null;
        int minIdle;
        int maxActive;
        
        switch(type){
            case SQLITE:
                datasourceString = "jdbc:sqlite:" + url + "/" + DB + ".sqlite";
                maxActive = 1;
                minIdle = -1;
                break;
            case MYSQL:
            default:
                minIdle = 10;
                maxActive = 20;
                datasourceString = "jdbc:mysql://" + url + ":" + port + "/" + DB + "?autoReconnect=true";
                break;
        }

        ds = setupDataSource( datasourceString, username, password, minIdle, maxActive );
        
        if ( ds == null ) return;       
        if ( DEBUG ) Log.info( "Connection to database succeeded." );

        if ( type == SQLType.MYSQL ) {
            try (   Connection con = ds.getConnection();
                    Statement st = con.createStatement(); ) {
                
                st.executeUpdate( create_database );              
                if (DEBUG) Log.info("Creating db");
            } 
            catch (SQLException e) {
                Log.error( "Failed creating db: " + create_database );
                e.printStackTrace();
            } 
        }
    }

}
