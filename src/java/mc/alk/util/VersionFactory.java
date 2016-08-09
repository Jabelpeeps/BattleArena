package mc.alk.util;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class VersionFactory {
    
    /**
     * Factory method used when you want to construct a Version object via pluginName. <br/>
     */
    public static Version<Plugin> getPluginVersion(String pluginName) { 
        
        Plugin plugin = Bukkit.getPluginManager().getPlugin( pluginName );
        
        return new Version<>( ( plugin == null ) ? "" 
                                                 : plugin.getDescription().getVersion() ); 
    }
    
    public static class Version<T> implements Comparable<Version<T>> {
        
        final String version;
        String separator = "[_.-]";
        
        public Version( String _version ) {
            version = _version;
        }
         
        /**
         * Alias for isGreaterThanOrEqualTo().
         * @param minVersion - The absolute minimum version that's required to achieve compatibility.
         * @return Return true, if the currently running/installed version is greater than or equal to minVersion.
         */
        public boolean isCompatible(String minVersion) {
            return compareTo( new Version<>( minVersion ) ) >= 0;
        }
        
        @Override
        public int compareTo( Version<T> whichVersion ) {
            int[] currentVersion = parseVersion( version );
            int[] otherVersion = parseVersion( whichVersion.toString() );
            
            int length = (currentVersion.length >= otherVersion.length) ? currentVersion.length 
                                                                        : otherVersion.length;
            
            for ( int index = 0; index < length; index = index + 1 ) {
                
                int self = (index < currentVersion.length) ? currentVersion[index] : 0;
                int other = (index < otherVersion.length) ? otherVersion[index] : 0;
                
                if (self != other) {
                    return self - other;
                }
            }
            return 0;
        }
        
        /**
         * A typical version of 1.2.3.4-b567 will be broken down into an array. <br/><br/>
         * 
         * [1] [2] [3] [4] [567]
         */
        private int[] parseVersion(String versionParam) {
            
            versionParam = ( versionParam == null ) ? "" : versionParam;
            String[] stringArray = versionParam.split( separator );
            int[] temp = new int[stringArray.length];
            
            for ( int index = 0; index <= ( stringArray.length - 1 ); index = index + 1 ) {
                
                String t = stringArray[index].replaceAll("\\D", "");
                try {
                    temp[index] = Integer.valueOf(t);
                } 
                catch( NumberFormatException ex ) {
                    temp[index] = 0;
                }
            }
            return temp;
        }

        @Override
        public String toString() {    
            return ( version == null ) ? "" 
                                       : version;
        }
    }
}