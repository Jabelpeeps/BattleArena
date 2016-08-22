package mc.alk.arena.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Formatter;
import java.util.GregorianCalendar;
import java.util.Vector;

import mc.alk.arena.BattleArena;


/**
 *
 * @author alkarin
 *
 */
public class FileLogger {
    static final String version = "1.0.3";
    static Vector<String> msgs = new Vector<>();

    public static Integer count = 0;
    public static final Integer saveEvery = 100;
    public static final Integer saveTime = 60000; /// every 60 seconds
    public static final Integer maxFileSize = 300000; /// in lines
    public static final Integer reduceToSize = 100000; /// reduce to this many lines when it exceeds maxFileSize
    static long lastSave = 0L;

    public static synchronized void init(){
        File f = new File(BattleArena.getSelf().getDataFolder()+"/saves/log.txt");
        int lineCount = count(f.getAbsolutePath());
        if (lineCount > maxFileSize){
            trimFile(f,lineCount);
        }
    }

    public static synchronized int log(String msg) {
        try {
            Calendar cal = new GregorianCalendar();
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd,hh:mm:ss");
            msgs.add(sdf.format(cal.getTime()) + ","+msg+"\n");
        } catch(Exception e){
            e.printStackTrace();
        }
        if ( (saveEvery != null&&(count++ % saveEvery == 0)) || System.currentTimeMillis()-lastSave > saveTime){
            saveAll();
            lastSave = System.currentTimeMillis();
        }
        return -1;
    }

    public static synchronized int log(String node, Object... varArgs) {
        try {
            Calendar cal = new GregorianCalendar();
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd,hh:mm:ss");
            StringBuilder buf = new StringBuilder();
            Formatter form = new Formatter(buf);
            form.format(node, varArgs);
            msgs.add(sdf.format(cal.getTime()) + "," + buf.toString() +"\n");
            form.close();
            return msgs.size();
        } catch(Exception e){
            e.printStackTrace();
        }
        if (saveEvery != null){
            if (count++ % saveEvery == 0)
                saveAll();
        }
        return -1;
    }

    public static synchronized void saveAll() {
        BufferedWriter out = null;
        try {
            File f = new File(BattleArena.getSelf().getDataFolder()+"/saves/log.txt");
            out = new BufferedWriter(new FileWriter(f,true));
            for (String msg : msgs){
                out.write(msg);
            }
            msgs.clear();

        } catch (Exception e){
            e.printStackTrace();
        } finally{
            if (out != null)
                try {out.close();} catch (IOException e) {/* do nothing */}
        }
    }

    private static File trimFile(File f, int lineCount) {
        File f2 = new File(BattleArena.getSelf().getDataFolder()+"/log2.txt");

        try ( BufferedWriter out = new BufferedWriter(new FileWriter(f2,true));
              BufferedReader br = new BufferedReader(new FileReader(f)); ) {

            int _count = 0;
            String line;
            while (_count < maxFileSize - lineCount){
                br.readLine();
            }
            while ((line = br.readLine()) != null){
                out.write(line+"\n");
            }
            if (!f2.renameTo(f)){
                Log.info("Couldn't rename file " + f.getName());
            }
        } catch ( IOException e ) {
            e.printStackTrace();
        } 
        return f2;
    }

    /**
     * Code from
     * http://stackoverflow.com/questions/453018/number-of-lines-in-a-file-in-java
     *
     * @param filename name of file
     * @return line count
     * @throws IOException
     */
    static int count(String filename)  {
        File f = new File(filename);
        if (!f.exists())
            return 0;
        
        try ( InputStream is = new BufferedInputStream(new FileInputStream(filename)); ) {
            byte[] c = new byte[1024];
            int _count = 0;
            int readChars;
            while ((readChars = is.read(c)) != -1) {
                for (int i = 0; i < readChars; ++i) {
                    if (c[i] == '\n')
                        ++_count;
                }
            }
            return _count;
        } catch ( IOException e ) {
            e.printStackTrace();
        } 
        return 0;
    }
}
