package mc.alk.plugin.updater;

import org.apache.commons.lang.StringUtils;

/**
 *
 * @author alkarin
 *
 */
public class Version implements Comparable<Object> {
    final static String version ="1.1";
    final String[] parts;
    public Version(Object obj){
        parts = obj.toString().trim().split("\\.");
    }

    @Override
    public int compareTo(Object o) {
        if (o instanceof Version)
            return compareToVersion((Version)o);
        return compareToVersion(new Version(o.toString()));
    }

    public String getVersion(){
        return this.toString();
    }

    @Override
    public String toString(){
        return StringUtils.join(parts, '.');
    }

    private int compareToVersion(Version v) {
        int max = Math.max(parts.length, v.parts.length);
        String p1[],p2[];
        if (max > parts.length){
            p1 = new String[max];
            System.arraycopy(parts, 0, p1, 0, parts.length);
            for (int i=parts.length;i<max;i++){
                p1[i] = "0";}
        } else {
            p1 = parts;
        }
        if (max > v.parts.length){
            p2 = new String[max];
            System.arraycopy(v.parts, 0, p2, 0, v.parts.length);
            for (int i=v.parts.length;i<max;i++){
                p2[i] = "0";}
        } else {
            p2 = v.parts;
        }
        for (int i=0;i<max;i++){
            int c=0;
            if (isInt(p1[i]) && isInt(p2[i])){
                Integer i1 = Integer.valueOf(p1[i]);
                Integer i2 = Integer.valueOf(p2[i]);
                c = i1.compareTo(i2);
            } else {
                c = p1[i].compareTo(p2[i]);
            }
            if (c == 0) continue;
            return c;
        }
        return 0;
    }

    public static boolean isInt(String str){
        try{
            Integer.valueOf(str);
            return true;
        } catch(Exception e){
            return false;
        }
    }
}