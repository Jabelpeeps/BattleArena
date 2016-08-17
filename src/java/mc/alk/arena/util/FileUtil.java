package mc.alk.arena.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import mc.alk.arena.BattleArena;

public class FileUtil {

	@SuppressWarnings( "resource" )
    public static InputStream getInputStream(Class<?> clazz, File file) {
		InputStream inputStream = null;
		if (file.exists()){
			try {
				return new FileInputStream(file);
			} catch (FileNotFoundException e) {
				Log.printStackTrace(e);
			}
		}
		String path = file.getPath();
		/// Load from pluginJar
		inputStream = clazz.getResourceAsStream(path);
		
		if (inputStream == null)
			inputStream = clazz.getClassLoader().getResourceAsStream(path);
		
		return inputStream;
	}


	@SuppressWarnings( "resource" )
    public static InputStream getInputStream(Class<?> clazz, File defaultFile, File defaultPluginFile) {
		InputStream inputStream = null;
		if (defaultPluginFile.exists()){
			try {
				inputStream = new FileInputStream(defaultPluginFile);
			} catch (FileNotFoundException e) {
				Log.printStackTrace(e);
			}
		}

		/// Try to load a default file from the given plugin
		/// Load from ExtensionPlugin.Jar
		if (inputStream == null)
			inputStream = clazz.getResourceAsStream(defaultPluginFile.getPath());
		
		if (inputStream == null) /// will this work to fix the problems in windows??
			inputStream = clazz.getClassLoader().getResourceAsStream(defaultPluginFile.getPath());
		/// Load from the defaults
		/// Load from BattleArena.jar
		if (inputStream == null)
			inputStream = BattleArena.getSelf().getClass().getResourceAsStream(defaultFile.getPath());
		
		if (inputStream == null)
			inputStream = BattleArena.getSelf().getClass().getClassLoader().getResourceAsStream(defaultFile.getPath());
		
		return inputStream;
	}

	public static boolean hasResource(Class<?> clazz, String default_file) {
		
	    boolean hasResource = false;
	    
		try ( InputStream inputStream = clazz.getResourceAsStream(default_file) ) {
		    
			if (inputStream != null) hasResource = true;
		
		} catch ( IOException e ) {
            e.printStackTrace();
        } 
		
		if ( !hasResource ) {
		    /// will this work to fix the problems in windows??
		    try ( InputStream inputStream = clazz.getClassLoader().getResourceAsStream(default_file) ) {
	            
	            if (inputStream != null) hasResource = true;
	        
	        } catch ( IOException e ) {
	            e.printStackTrace();
	        } 
		}
		return hasResource;		
	}

	public static File load(Class<?> clazz, String config_file, String default_file) {
	    
		File file = new File( config_file );
		
		if ( !file.exists() ) { /// Create a new file from our default example
		
    		try ( OutputStream out = new FileOutputStream( config_file ) ) {
    		    boolean succeeded = false;
    		    
    		    try ( InputStream inputStream = clazz.getResourceAsStream(default_file) ) {                    		        
    		        if ( inputStream != null ) {
    		            writeFile( inputStream, out );
    		            succeeded = true;
    		        }
                } 
    		    
    		    if ( !succeeded ) 
    		        try ( InputStream inputStream = clazz.getClassLoader().getResourceAsStream(default_file) ) {
    		            if ( inputStream != null ) {
                            writeFile( inputStream, out );
                            succeeded = true;
                        }
    		        }
            } catch ( IOException e1 ) {
                e1.printStackTrace();
            } 
		}
		return file;
	}
	private static void writeFile( InputStream input, OutputStream output ) throws IOException {

        byte buf[] = new byte[1024];
        int len;
        
        while( ( len = input.read( buf ) ) > 0 ) {
            output.write( buf, 0, len );
        }
	}
}
