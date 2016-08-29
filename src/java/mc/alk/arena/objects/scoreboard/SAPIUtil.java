package mc.alk.arena.objects.scoreboard;

import org.bukkit.craftbukkit.libs.jline.internal.Log;

import mc.alk.arena.Defaults;

/**
 * @author alkarin
 */
public class SAPIUtil {
    public static String createLimitedString( String prefix,
                                               String name,
                                               String suffix, 
                                               int maxCharLength ) {
        if ( Defaults.DEBUG ) 
            Log.info( "SAPIUtil.createLimitedString() called with:- " + System.lineSeparator() +
                      "prefix='" + prefix + "'  name='" + name + "'  suffix='" + suffix + "'" );
        StringBuilder sb = new StringBuilder();
        /// all < length
        int s = (prefix != null ? prefix.length() : 0) + name.length() +
                (suffix != null ? suffix.length() : 0);
        if ( s <= maxCharLength){
            
            if ( prefix != null ) sb.append( prefix );
            sb.append( name );
            if ( suffix != null ) sb.append( suffix );
            return sb.toString();
        }
        /// Shrink name if possible
        if ( s - maxCharLength < ( name.length() - name.length() / 3 ) ){
            name = name.substring( 0, name.length() - ( s - maxCharLength ) );
            
            if ( prefix != null ) sb.append( prefix );
            sb.append( name );
            if ( suffix != null ) sb.append( suffix );
            return sb.toString();
        }
        
        if ( name.length() > maxCharLength / 3 ) {
            name = name.substring( 0, maxCharLength / 3 );
            
            if ( ( prefix != null ? prefix.length() : 0 ) + 
                    name.length() +
                    ( suffix != null ? suffix.length() : 0) <= maxCharLength ) {
                
                if ( prefix != null ) sb.append(prefix);             
                sb.append( name );
                if ( suffix != null ) sb.append( suffix );
                
                return sb.toString();
            }
        }
        ///Shrink prefix
        if ( prefix != null ) {
            sb.append( prefix.length() > maxCharLength / 3 ? prefix.substring( 0, maxCharLength / 3 ) 
                                                           : prefix );
        }
        ///Shrink suffix
        sb.append( name );
        if ( suffix != null ) {
            sb.append( suffix.length() > maxCharLength / 3 ? suffix.substring( 0, maxCharLength / 3 ) 
                                                           : suffix );
        }
        if ( sb.length() > maxCharLength ) {
            return sb.substring(0, maxCharLength);
        }
        return sb.toString();
    }
}
