package mc.alk.arena.tracker;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

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
import mc.alk.arena.BattleArena;
import mc.alk.arena.Defaults;

public class SQLSerializer {
    public static final String version = "1.5";
    protected static final boolean DEBUG = false;
    protected static final boolean DEBUG_UPDATE = false;
	protected static final int TIMEOUT = 4; 

	@AllArgsConstructor @Getter
	public enum SQLType {
		MYSQL( "MySQL", "com.mysql.jdbc.Driver" ), 
		SQLITE( "SQLite", "org.sqlite.JDBC" );
	    String name, driver;
	};
	
	private DataSource ds ;

	protected String DB = "minecraft";
	@Getter protected SQLType type = SQLType.MYSQL;
	protected String url = "localhost";
	protected String port = "3306";
	protected String username = "root";
	protected String password = "";

	private String create_database = "CREATE DATABASE IF NOT EXISTS `" + DB + "`";

	protected class RSCon implements AutoCloseable {
		public ResultSet rs;
		public PreparedStatement ps;
		
		@Override
        public void close() {
		    try {
                if ( rs != null ) rs.close();
                if ( ps != null ) ps.close();
            } 
		    catch ( SQLException e ) {
                e.printStackTrace();
            }
		}
	}

	public Connection getConnection() throws SQLException {

		if ( ds == null ) 
			throw new SQLException( "Connection is null.  Did you intiliaze your SQL connection?" ); 
		try {
			Connection con = ds.getConnection();
			con.setAutoCommit(true);
			return con;
		} catch ( SQLException e1 ) {
			e1.printStackTrace();
			return null;
		}
	}

	public static DataSource setupDataSource( String connectURI, String username, String password, 
	                                                                    int minIdle, int maxActive ) {

	    GenericObjectPool.Config poolConf = new GenericObjectPool.Config();
	    poolConf.lifo = false;
	    poolConf.maxActive = maxActive;
	    	    
		if ( minIdle != -1 )
			poolConf.minIdle = minIdle;
		
		PoolableConnectionFactory factory = new PoolableConnectionFactory( 
		        new DriverManagerConnectionFactory( connectURI, username, password ), 
		        new GenericObjectPool( null, poolConf ),
		        new GenericKeyedObjectPoolFactory( null ), 
				"select 1", false, true );
	
		return new PoolingDataSource( factory.getPool() );
	}

	protected boolean createTable( String tableName, String sql_create_table, String... sql_updates ) {
	    
		Boolean exists = false;
		if ( type == SQLType.SQLITE ) {
			exists = getBoolean( "SELECT count(name) FROM sqlite_master WHERE type='table' AND name='" + tableName + "';" );
		} 
		else {
			List<Object> objs = getObjects( "SHOW TABLES LIKE '" + tableName + "';" );
			exists = objs != null && objs.size() == 1;
		}
		if (DEBUG) Log.info( "table " + tableName + " exists =" + exists );

		if ( exists != null && exists ) return true; 

		int result = 0;
		
		try ( Connection con = getConnection();
              Statement st = con.createStatement(); ) {
		    
			result = st.executeUpdate( sql_create_table );			
			if (DEBUG) Log.info( "Created Table with stmt=" + sql_create_table );
			
		    if ( sql_updates != null ) {
	            for ( String sql_update : sql_updates ) {
	                if (sql_update == null) continue;
	                
	                try {
	                    result = st.executeUpdate( sql_update );
	                    if (DEBUG) Log.info( "Update Table with stmt=" + sql_update );
	                } 
	                catch (SQLException e) {
	                    Log.error( "Failed in updating Table " + sql_update + " result=" + result);
	                    e.printStackTrace();
	                    return false;
	                }
	            }
	        }
		} catch (SQLException e) {
		    Log.error( "Failed in creating Table " + sql_create_table + " result=" + result);
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
	    
	    if ( Defaults.DEBUG_TRACKING )
	        Log.info( "SQLSerializer.hasTable() called with tableName= " + tableName ); 
	    
		Boolean exists;
		if ( type == SQLType.SQLITE )
			exists = getBoolean( "SELECT count(name) FROM sqlite_master WHERE type='table' AND name='" + tableName + "'" );
		else {
			List<Object> objs = getObjects( "SHOW TABLES LIKE '" + tableName + "';" );
			exists = ( objs != null && objs.size() == 1 );
		}
		return exists == null ? false : exists;
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
      
		try ( Connection con = getConnection() ) {
		    
            RSCon rscon = new RSCon();
		    rscon.ps = getStatement( strRawStmt, con, varArgs );
		    
			if (DEBUG) System.out.println( "Executing =" + rscon.ps + " timeout=" + timeoutSeconds + " raw=" + strRawStmt );

			rscon.ps.setQueryTimeout( timeoutSeconds );
			rscon.rs = rscon.ps.executeQuery();	
			
			return rscon;
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
		return null;
	}

	protected void executeUpdate( boolean async, String strRawStmt, Object... varArgs ) {
		if ( async )
			new Thread( () -> executeUpdate(strRawStmt, varArgs) ).start();
		else 
		    executeUpdate(strRawStmt, varArgs);
	}
	
	private int executeUpdate( String strRawStmt, Object... varArgs ) {
		int result = -1;

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
	    if ( async ) 
			new Thread( () -> executeBatch(updateStatement, batch) ).start();
		else 
			executeBatch(updateStatement, batch);
	}

	private void executeBatch( String updateStatement, List<List<Object>> batch ) {
		
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
		try {
			ps = con.prepareStatement( strRawStmt );
			for ( int i = 0; i < varArgs.length; i++ ) {
				if (DEBUG_UPDATE) System.out.println( i + " = " + varArgs[i] );
				ps.setObject( i + 1, varArgs[i] );
			}
		} catch ( SQLException e ) {
			if ( Defaults.DEBUG_TRACKING ) {
				System.err.println("Couldnt prepare statment "  + ps +"   rawStmt='" + strRawStmt +"' args="+varArgs);
				for (int i=0;i< varArgs.length;i++){
					System.err.println("   arg["+ i+"] = " + varArgs[i]);}
				e.printStackTrace();
			}
		}
		return ps;
	}

	protected Integer getInteger( String query, Object... varArgs ) {
		
		try ( RSCon rscon = executeQuery( TIMEOUT, query, varArgs ) ) {
			ResultSet rs = rscon.rs;
			while ( rs.next() ) {
				return rs.getInt(1);
			}
		} catch ( SQLException e ) {
			e.printStackTrace();
		}
		return null;
	}

	private Boolean getBoolean( String query, Object... varArgs ) {
		return getBoolean( TIMEOUT, query, varArgs );
	}

	private Boolean getBoolean( Integer timeoutSeconds, String query, Object... varArgs){
		
		try ( RSCon rscon = executeQuery( timeoutSeconds, query, varArgs ) ) {
			ResultSet rs = rscon.rs;
			while ( rs.next() ) {
				int i = rs.getInt(1);
				if ( i == 0 ) return null;
				return i > 0;
			}
		} catch (SQLException e) {
			if ( Defaults.DEBUG_TRACKING )
				e.printStackTrace();
		}
		return null;
	}

	protected List<Object> getObjects( String query, Object... varArgs ) {
		
		try ( RSCon rscon = executeQuery( TIMEOUT, query, varArgs ) ) {
			ResultSet rs = rscon.rs;
			while ( rs.next() ) {
				int nCol = rs.getMetaData().getColumnCount();
				List<Object> objs = new ArrayList<>(nCol);
				for ( int i = 0; i < nCol; i++ ) {
					objs.add( rs.getObject( i + 1 ) );
				}
				return objs;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

    public void configureSQL( ConfigurationSection cs ) {
        
    	String _type = cs.getString("type");
    	
    	if ( _type == null ) return;
    	
    	DB = cs.getString( "db", "minecraft" );
        create_database = "CREATE DATABASE IF NOT EXISTS `" + DB + "`";
    	
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
            try ( Connection con = getConnection();
                  Statement st = con.createStatement(); ) {
                
                st.executeUpdate( create_database );              
                if (DEBUG) Log.info( "Creating db" );
            } 
            catch (SQLException e) {
                Log.error( "Failed creating db: " + create_database );
                e.printStackTrace();
            } 
        }
    }
}
