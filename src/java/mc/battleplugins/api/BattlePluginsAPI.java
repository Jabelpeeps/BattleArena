package mc.battleplugins.api;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Proxy;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.zip.GZIPOutputStream;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;

/**
 * @author alkarin, lducks
 */
public class BattlePluginsAPI {
    /** current version */
    public static final long version = 1L;

    /** request protocol */
    static final String PROTOCOL = "http";

    /** battleplugins site */
    protected static String HOST = "battleplugins.com";

    /** current user agent */
    static final String USER_AGENT = "BattlePluginsAPI/v1.0";

    /** api key: used to verify requests */
    protected String apiKey = "";

    /** whether to send stats or not */
    final AtomicBoolean sendStats = new AtomicBoolean(true);

    /** ID of any currently running tasks */
    Integer timer;

    /** Key Value pairs to send */
    final Map<String, String> pairs = new TreeMap<>();

    /** The plugins to send stats about */
    final List<Plugin> plugins = new ArrayList<>();

    /** debug */
    protected boolean debug;

    public BattlePluginsAPI() {
    }

    public BattlePluginsAPI(Plugin plugin) {
        plugins.add(plugin);
        try {
            if (getConfig().getBoolean("SendStatistics",false)) {
                sendStats.set(true);
                scheduleSendStats(plugin);
            } else {
                sendStats.set(false);
            }
        } catch (IOException e) {
            plugin.getLogger().severe(
                    "BattlePluginsAPI was not able to load. Message: " + e.getMessage());
            plugin.getLogger().log(Level.SEVERE, null, e);
            sendStats.set(false);
        }
    }

    private static String urlEncode(final String text) throws UnsupportedEncodingException {
        return URLEncoder.encode(text, "UTF-8");
    }

    /**
     * Paste the given file to the battleplugins website
     * @param title Title of the paste
     * @param file Full path to the file to paste
     * @return response
     * @throws IOException
     */
    public String pasteFile(String title, String file) throws IOException {
        FileConfiguration c = getConfig();
        apiKey = c.getString("API-Key", null);

        if (apiKey == null) {throw new IOException(
                "API Key was not found. You need to register before sending pastes");}
        File f = new File(file);
        addPair("title", title);
        addPair("content", toString(f.getPath()));
        Map<String,Object> result = post(new URL(PROTOCOL + "://" + HOST + "/api/web/paste/create"));
        if (result.containsKey("id"))
            return PROTOCOL + "://" + HOST + "/paste/"+result.get("id");
        return null;
    }

    /**
     * Send statistics about the server and the given plugin
     * @param plugin the plugin to send information about
     * @throws IOException
     */
    public void sendStatistics(Plugin plugin) throws IOException {
        List<Plugin> plugins = new ArrayList<>();
        plugins.add(plugin);
        sendStatistics(plugins);
    }

    public void sendStatistics(final List<Plugin> plugins) throws IOException {
        for (Plugin plugin : plugins){
            addPluginInfo(plugin);}
        addServerInfo();
        addSystemInfo();
        post(new URL(PROTOCOL + "://" + HOST + "/statistics/set"));
    }

    /**
     * Add the given pair to request
     * @param key key to send
     * @param value value to send
     * @throws UnsupportedEncodingException
     */
    public void addPair(String key,String value) throws UnsupportedEncodingException {
        pairs.put(key, urlEncode((value)));
    }

    /**
     * Add the given pair to request, will zip the value
     * @param key key to send
     * @param value value to zip and send
     * @throws UnsupportedEncodingException
     */
    public void addZippedPair(String key,String value) throws IOException {
        pairs.put(key, gzip(value));
    }

    /**
     * Add plugin info to the request
     * @param plugin the plugin to send information about
     * @throws UnsupportedEncodingException
     */
    public void addPluginInfo(Plugin plugin) throws UnsupportedEncodingException {
        PluginDescriptionFile d = plugin.getDescription();
        addPair("p"+d.getName(), d.getVersion());
    }

    /**
     * Add the server info to this request
     * @throws UnsupportedEncodingException
     */
    public void addServerInfo() throws UnsupportedEncodingException {
        addPair("bServerName", Bukkit.getServerName());
        addPair("bVersion", Bukkit.getVersion());
        addPair("bOnlineMode", String.valueOf(Bukkit.getServer().getOnlineMode()));
        addPair("bPlayersOnline", String.valueOf(Bukkit.getServer().getOnlinePlayers().size()));
    }

    /**
     * Add system info to this request
     * @throws UnsupportedEncodingException
     */
    public void addSystemInfo() throws UnsupportedEncodingException {
        addPair("osArch", System.getProperty("os.arch"));
        addPair("osName", System.getProperty("os.name"));
        addPair("osVersion", System.getProperty("os.version"));
        addPair("jVersion", System.getProperty("java.version"));
        addPair("nCores", String.valueOf(Runtime.getRuntime().availableProcessors()));
    }

    /**
     * Send a get request
     * @param baseUrl url destination
     * @throws IOException
     */
    public List<String> get(URL baseUrl) throws IOException {
        /// Connect
        URL url = new URL (baseUrl.getProtocol()+"://"+baseUrl.getHost()+
                baseUrl.getPath() + "?" + toString(pairs));
        if (debug) System.out.println(url);
        URLConnection connection = url.openConnection(Proxy.NO_PROXY);

        /// Connection information
        connection.addRequestProperty("GET", "/api/web/blog/all HTTP/1.1");
        connection.addRequestProperty("Host", HOST);
        connection.addRequestProperty("X-API-Key", apiKey);
        connection.addRequestProperty("User-Agent", USER_AGENT);
        connection.setDoOutput(true);

        /// write the data to the stream
        OutputStream os = connection.getOutputStream();
        os.flush();

        /// Get our response
        final BufferedReader br = new BufferedReader(
                new InputStreamReader(connection.getInputStream()));
        List<String> response = new ArrayList<>();
        String line;
        while ( (line = br.readLine()) != null){
            response.add(line);
        }

        os.close();
        br.close();
        return response;
    }

    /**
     * Send a post request
     * @param url destination
     * @throws IOException
     */
    public Map<String,Object> post(URL url) throws IOException {
        /// Connect
        URLConnection connection = url.openConnection(Proxy.NO_PROXY);
        if (debug) System.out.println(url + "?" + toString(pairs));
        byte[] data = toString(pairs).getBytes();
        connection.addRequestProperty("POST", "/api/web/blog/all HTTP/1.1");
        connection.addRequestProperty("Host", HOST);
        connection.addRequestProperty("X-API-Key", apiKey);
        connection.addRequestProperty("User-Agent", USER_AGENT);
        connection.setRequestProperty("Content-length",String.valueOf(data.length));
        connection.setDoOutput(true);

        /// write the data to the stream
        OutputStream os = connection.getOutputStream();
        os.write(data);
        os.flush();

        /// Get our response
        final BufferedReader br = new BufferedReader(
                new InputStreamReader(connection.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ( (line = br.readLine()) != null) {
            sb.append(line);
        }

        os.close();
        br.close();
        return stringToMap(sb.toString());
    }

    String gzip(String str) throws IOException {
        GZIPOutputStream out = new GZIPOutputStream(new ByteArrayOutputStream());
        out.write(str.getBytes());
        out.close();
        return out.toString();
    }

    String toString(Map<String, String> pairs) throws IOException {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Entry<String, String> e : pairs.entrySet()) {
            if (!first) sb.append("&");
            else first = false;
            sb.append(e.getKey()).append("=").append(e.getValue());
        }
        return sb.toString();
    }

    /**
     * A naive (very naive) unnested json parset
     * @param str string to parse
     * @return Map of strings to Objects
     */
    Map<String,Object> stringToMap(String str) {
        Map<String,Object> map = new HashMap<>();
        int f = str.indexOf('{');
        int l = str.lastIndexOf('}');
        if (f == -1 || l == -1 || f >= l || str.length() < 2) {
            return map;}
        String[] kvs = str.substring(f+1, l).split(",");
        for (String kv : kvs) {
            String[] split = kv.replace('"', ' ').split(":");
            map.put(split[0].trim(), split[1].trim());
        }
        return map;
    }


    /**
     * The name and version
     * @return name and version
     */
    @Override
    public String toString(){
        return "[BattlePluginsAPI v" + version+"]";
    }

    /**
     * Get the configuration file
     * @return returns the configuration file
     * @throws IOException if file not found or could not be created
     */
    public File getConfigurationFile() throws IOException {
        File pluginFolder = Bukkit.getServer().getUpdateFolderFile().getParentFile();
        File f = new File(pluginFolder,"BattlePluginsAPI");
        if (!f.exists()){
            if (!f.mkdirs()){
                throw new IOException("Couldn't create config directory");}
        }
        f = new File(f, "config.yml");
        if (!f.exists()){
            /// Strangely the file can be created correctly and createNewFile can
            // return false, so double check
            if (f.createNewFile() && !f.exists()) {
                throw new IOException("Couldn't create config file");}
        }
        return f;
    }

    /**
     * Get the configuration
     * @return returns the configuration
     * @throws IOException if file not found or could not be created
     */
    public FileConfiguration getConfig() throws IOException {
        File f = getConfigurationFile();
        FileConfiguration c = YamlConfiguration.loadConfiguration(f);
        if (c.get("API-Key", null) == null || c.get("SendStatistics", null)==null) {
            c.options().header(
                    "Configuration file for BattlePluginsAPI. http://battleplugins.com\n"+
                            "Allows plugins using BattlePluginsAPI to interface with the website\n"+
                            "API-Key : unique id for server authentication\n"+
                            "SendStatistics : set to false to not send statistics");
            c.addDefault("API-Key", "");
            c.addDefault("SendStatistics", true);
            c.options().copyDefaults(true);
            c.save(f);
        }
        return c;
    }

    /**
     * Get the current API-Key
     * @return API-Key
     */
    public String getAPIKey(){
        try {
            FileConfiguration c = getConfig();
            return c.getString("API-Key");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Set the API-Key
     * @param newKey new API key
     */
    public void setAPIKey(String newKey){
        try {
            FileConfiguration c = getConfig();
            c.set("API-Key",newKey);
            c.save(getConfigurationFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings( "deprecation" )
    private void scheduleSendStats(final List<Plugin> plugins){
        if (!sendStats.get() || plugins.isEmpty())
            return;
        if (timer != null){
            Bukkit.getScheduler().cancelTask(timer);}
        //noinspection deprecation
        timer = Bukkit.getScheduler().scheduleAsyncRepeatingTask(plugins.iterator().next(),new Runnable(){
            @Override
            public void run() {
                if (!sendStats.get()) {
                    if (timer != null) {
                        Bukkit.getScheduler().cancelTask(timer);
                        timer = null;
                    }
                } else {
                    try {
                        sendStatistics(plugins);
                    } catch(UnknownHostException e) {
                        /** Don't send stack trace on no network errors, just continue on*/
                    } catch(InterruptedIOException e) {
                        Bukkit.getLogger().warning(e.getMessage());
                        sendStats.set(false);
                    } catch(SocketException e){
                        /** Don't send stack trace on no network errors, just continue on*/
                        if (e.getMessage() == null || !e.getMessage().contains("unreachable")){
                            e.printStackTrace();
                            sendStats.set(false);
                        }
                    } catch (IOException e) {
                        Bukkit.getLogger().warning(e.getMessage());
                        sendStats.set(false);
                    }
                }
            }
        },60*20+(new Random().nextInt(20*120))/* start between 1-3 minutes*/,
                60*20*15/*send stats every 15 min*/);
    }

    /**
     * schedule BattlePluginsAPI to send plugin statistic
     * @param plugin the plugin to send information about
     */
    public void scheduleSendStats(final Plugin plugin) {
        plugins.add(plugin);
        if (timer != null){
            Bukkit.getScheduler().cancelTask(timer);
            timer = null;
        }
        scheduleSendStats(plugins);
    }

    /**
     * stop sending plugin statistics
     */
    public void stopSendingStatistics() throws IOException {
        setSending(false);
        if (timer != null) {
            Bukkit.getScheduler().cancelTask(timer);
            timer = null;
        }
    }

    /**
     * stop sending plugin statistics for the given Plugin
     */
    public void stopSendingStatistics(Plugin plugin) throws IOException {
        if (plugins.remove(plugin)){
            scheduleSendStats(plugins);
        }
    }

    /**
     * start sending plugin statistics
     * @param plugin the plugin to send information about
     * @throws IOException
     */
    public void startSendingStatistics(Plugin plugin) throws IOException {
        setSending(true);
        scheduleSendStats(plugin);
    }

    private void setSending(boolean send) throws IOException {
        sendStats.set(send);
        FileConfiguration config = getConfig();
        if (config.getBoolean("SendStatistics", !send)){
            config.set("SendStatistics", send);
            config.save(getConfigurationFile());
        }
    }

    /** Code from erikson,
     * http://stackoverflow.com/questions/326390/how-to-create-a-java-string-from-the-contents-of-a-file*/
    private static String toString(String path) throws IOException {
        FileInputStream stream = new FileInputStream(new File(path));
        try {
            FileChannel fc = stream.getChannel();
            MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
            return Charset.defaultCharset().decode(bb).toString();
        } finally {
            stream.close();
        }
    }

}