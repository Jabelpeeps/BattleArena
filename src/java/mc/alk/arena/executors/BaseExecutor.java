package mc.alk.arena.executors;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import mc.alk.arena.BattleArena;
import mc.alk.arena.Permissions;
import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.util.Log;
import mc.alk.arena.util.MessageUtil;
import mc.alk.arena.util.ServerUtil;

public abstract class BaseExecutor implements CommandExecutor{
    public static final String version = "2.1.0";
    static final boolean DEBUG = false;
    static final String DEFAULT_CMD = "_dcmd_";
    private HashMap<String, TreeMap<Integer, MethodWrapper>> methods = new HashMap<>();
    private HashMap<String, Map<String, TreeMap<Integer, MethodWrapper>>> subCmdMethods = new HashMap<>();
    public static final String ONLY_INGAME = ChatColor.RED + "You need to be in game to use this command";
    static final int LINES_PER_PAGE = 8;

    protected PriorityQueue<MethodWrapper> usage = new PriorityQueue<>();
    
    /**
     * Custom arguments class so that we can return a modified arguments
     */
    public static class Arguments {
        public Object[] args;
    }

    @RequiredArgsConstructor
    protected static class MethodWrapper implements Comparable<MethodWrapper>{

        final Object obj; /// Object instance the method belongs to
        final Method method; 
        
        MCCommand command;
        String usage;
        Float helpOrder;
        
        public MCCommand getCommand(){
            if ( command == null )
                command = method.getAnnotation( MCCommand.class );
            return command;
        }
       
        public float getHelpOrder(){
            if ( helpOrder == null )
                helpOrder = getCommand().helpOrder();
            return helpOrder;
        }
        @Override
        public int compareTo( MethodWrapper o ) {
            int c = Float.compare( getHelpOrder(), o.getHelpOrder() );
            
            if ( c != 0 ) return c;
            
            MCCommand cmd1 = getCommand();
            MCCommand cmd2 = o.getCommand();
            
            c = Integer.compare( cmd1.order(), cmd2.order() );
            
            return c != 0 ? c 
                          : Integer.compare( cmd1.hashCode(), cmd2.hashCode() );
        }
    }
    protected BaseExecutor() {
        addMethods( this, getClass().getMethods() );
    }

    protected boolean validCommandSenderClass(Class<?> clazz) {
        return CommandSender.class.isAssignableFrom( clazz ) 
                || ArenaPlayer.class.isAssignableFrom( clazz );
    }

    public void addMethods(Object obj, Method[] methodArray){

        for (Method method : methodArray){
            MCCommand mc = method.getAnnotation(MCCommand.class);
            
            if (mc == null) continue;
            
            Class<?> types[] = method.getParameterTypes();
            if (types.length == 0 || !validCommandSenderClass(types[0])){
                System.err.println("MCCommands must start with a CommandSender,Player, or ArenaPlayer");
                continue;
            }
            if (mc.cmds().length == 0){ /// There is no subcommand. just the command itself with arguments
                addMethod( obj, method, mc, DEFAULT_CMD );
            } 
            else {
                /// For each of the cmds, store them with the method
                for ( String cmd : mc.cmds() ) {
                    addMethod( obj, method, mc, cmd.toLowerCase() );
                }
            }
        }
    }

    private void addMethod(Object obj, Method method, MCCommand mc, String cmd) {
        int ml = method.getParameterTypes().length;
        if (mc.subCmds().length == 0){
            TreeMap<Integer,MethodWrapper> mthds = methods.get(cmd);
            if (mthds == null){
                mthds = new TreeMap<>();
            }
            int order = (mc.order() != -1? mc.order()*100000 :Integer.MAX_VALUE) - ml*100 - mthds.size();
            MethodWrapper mw = new MethodWrapper(obj,method);
            mthds.put(order, mw);
            methods.put(cmd, mthds);
            addUsage(mw, mc);
        } 
        else {
            Map<String,TreeMap<Integer,MethodWrapper>> basemthds = subCmdMethods.get(cmd);
            if (basemthds == null){
                basemthds = new HashMap<>();
                subCmdMethods.put(cmd, basemthds);
            }
            for (String subcmd: mc.subCmds()){
                TreeMap<Integer,MethodWrapper> mthds = basemthds.get(subcmd);
                if (mthds == null){
                    mthds = new TreeMap<>();
                    basemthds.put(subcmd, mthds);
                }
                int order = (mc.order() != -1? mc.order()*100000 :Integer.MAX_VALUE) - ml*100-mthds.size();
                MethodWrapper mw = new MethodWrapper(obj,method);
                /// Set help order
                if (mc.helpOrder() == Integer.MAX_VALUE){
                    mw.helpOrder = (float) (Integer.MAX_VALUE - usage.size());
                }
                mthds.put(order, mw);
                addUsage(mw, mc);
            }
        }
    }
    private void addUsage(MethodWrapper method, MCCommand mc) {

        /// save the usages, for showing help messages
        if (!mc.usage().isEmpty())
            method.usage = mc.usage();
        else /// Generate an automatic usage string
            method.usage = createUsage(method.method);
        
        usage.add(method);
    }

    private String createUsage(Method method) {
        MCCommand cmd = method.getAnnotation(MCCommand.class);
        StringBuilder sb = new StringBuilder(cmd.cmds().length > 0 ? cmd.cmds()[0] +" " : "");
        int startIndex = 1;
        if (cmd.subCmds().length > 0){
            sb.append(cmd.subCmds()[0]).append(" ");
            startIndex = 2;
        }
        Class<?> types[] = method.getParameterTypes();
        for (int i=startIndex;i<types.length;i++){
            Class<?> theclass = types[i];
            sb.append(getUsageString(theclass));
        }
        return sb.toString();
    }

    protected String getUsageString(Class<?> clazz) {
        
        if (Player.class == clazz) return "<player> ";
        else if (OfflinePlayer.class == clazz) return "<player> ";
        else if (String.class == clazz) return "<string> ";
        else if (Integer.class == clazz || int.class == clazz) return "<int> ";
        else if (Float.class == clazz || float.class == clazz) return "<number> ";
        else if (Double.class == clazz || double.class == clazz) return "<number> ";
        else if (Short.class == clazz || short.class == clazz) return "<int> ";
        else if (Boolean.class == clazz || boolean.class == clazz) return "<true|false> ";
        else if (String[].class == clazz || Object[].class == clazz) return "[string ... ] ";
        
        return "<string> ";
    }

    @AllArgsConstructor
    public class CommandException {
        final IllegalArgumentException err;
        final MethodWrapper mw;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        TreeMap<Integer,MethodWrapper> methodmap = null;

        /// No method to handle, show some help
        if ((args.length == 0 && !methods.containsKey(DEFAULT_CMD))
                || (args.length > 0 && (args[0].equals("?") || args[0].equals("help")))){
            showHelp(sender, command,args);
            return true;
        }
        final int length = args.length;
        final String cmd = length > 0 ? args[0].toLowerCase() : null;
        final String subcmd = length > 1 ? args[1].toLowerCase() : null;
        int startIndex = 0;

        /// check for subcommands
        if (subcmd!=null && subCmdMethods.containsKey(cmd) && subCmdMethods.get(cmd).containsKey(subcmd)){
            methodmap = subCmdMethods.get(cmd).get(subcmd);
            startIndex = 2;
        }
        if (methodmap == null && cmd != null){ /// Find our method, and verify all the annotations
            methodmap = methods.get(cmd);
            if (methodmap != null)
                startIndex =1;
        }

        if (methodmap == null){ /// our last attempt
            methodmap = methods.get(DEFAULT_CMD);
        }

        if (methodmap == null || methodmap.isEmpty()){
            return MessageUtil.sendMessage(sender, "&cThat command does not exist!&6 /"+command.getLabel()+" help &c for help");}

        MCCommand mccmd;
        List<CommandException> errs =null;
        boolean success = false;
        for (MethodWrapper mwrapper : methodmap.values()){

            mccmd = mwrapper.method.getAnnotation(MCCommand.class);
            final boolean isOp = sender == null || sender.isOp() || sender instanceof ConsoleCommandSender;

            if (( mccmd.op() && !isOp ) || mccmd.admin() ) /// no op, no pass
                continue;
            Arguments newArgs = null;
            try {
                newArgs= verifyArgs(mwrapper,mccmd,sender,command, label, args, startIndex);
                    Object completed = mwrapper.method.invoke(mwrapper.obj,newArgs.args);
                if (completed != null && completed instanceof Boolean){
                    success = (Boolean)completed;
                    if (!success){
                        String usage = mwrapper.usage;
                        if (usage != null && !usage.isEmpty()){
                            MessageUtil.sendMessage(sender, usage);}
                    }
                } else {
                    success = true;
                }
                break; /// success on one
            } catch (IllegalArgumentException e){ /// One of the arguments wasn't correct, store the message
                if (errs == null)
                    errs = new ArrayList<>();
                errs.add(new CommandException(e,mwrapper));
            } catch (Exception e) { /// Just all around bad
                logInvocationError(e, mwrapper,newArgs);
            }
        }
        /// and handle all errors
        if (!success && errs != null && !errs.isEmpty()){
            HashSet<String> usages = new HashSet<>();
            for (CommandException e: errs){
                usages.add(ChatColor.GOLD+command.getLabel() +" " +e.mw.usage+" &c:"+e.err.getMessage());
            }
            for (String msg : usages){
                MessageUtil.sendMessage(sender, msg);}
        }
        return true;
    }

    private void logInvocationError(Exception e, MethodWrapper mwrapper, Arguments newArgs) {
        
        Log.err( "["+BattleArena.getNameAndVersion()+" Error] "+mwrapper.method +" : " + mwrapper.obj +"  : " + newArgs);
        if (newArgs!=null && newArgs.args != null){
            for (Object o: newArgs.args)
                System.err.println("[Error] object=" + o);
        }
        Log.err( "[Error] Cause=" + e.getCause());
        if (e.getCause() != null){
            e.getCause().printStackTrace();
            Log.printStackTrace(e.getCause());
        }
        Log.err( "[Error] Trace Continued ");
        Log.printStackTrace(e);
    }

    protected Arguments verifyArgs(MethodWrapper mwrapper, MCCommand cmd, CommandSender sender,
                                   Command command, String label, String[] args, int startIndex) throws IllegalArgumentException{
        if (DEBUG){
            Log.info(" method="+mwrapper.method.getName() + " verifyArgs " + cmd +" sender=" +sender+
                    ", label=" + label+" args="+ Arrays.toString(args));
            for (String arg: args){
                Log.info(" -- arg=" +arg);}
            for (Class<?> t: mwrapper.method.getParameterTypes()){
                Log.info(" -- type=" +t);}
        }
        final int paramLength = mwrapper.method.getParameterTypes().length;

        /// Check our permissions
        if (!cmd.perm().isEmpty() && !sender.hasPermission(cmd.perm()) && !(cmd.admin() && sender.isOp()))
            throw new IllegalArgumentException("You don't have permission to use this command");

        /// Verify min number of arguments
        if (args.length < cmd.min()){
            throw new IllegalArgumentException("You need at least "+cmd.min()+" arguments");
        }
        /// Verfiy max number of arguments
        if (args.length > cmd.max()){
            throw new IllegalArgumentException("You need less than "+cmd.max()+" arguments");
        }
        /// Verfiy max number of arguments
        if (cmd.exact()!= -1 && args.length != cmd.exact()){
            throw new IllegalArgumentException("You need exactly "+cmd.exact()+" arguments");
        }
        final boolean isPlayer = sender instanceof Player;
        final boolean isOp = (isPlayer && sender.isOp()) || sender == null || sender instanceof ConsoleCommandSender;

        if (cmd.op() && !isOp)
            throw new IllegalArgumentException("You need to be op to use this command");

        if (cmd.admin() && !isOp && (isPlayer && !sender.isOp()))
            throw new IllegalArgumentException("You need to be an Admin to use this command");

        Class<?> types[] = mwrapper.method.getParameterTypes();

        //		/// In game check
        if (types[0] == Player.class && !isPlayer){
            throw new IllegalArgumentException(ONLY_INGAME);
        }
        int strIndex = startIndex/*skip the label*/, objIndex = 1;

        Arguments newArgs = new Arguments(); /// Our return value
        Object[] objs = new Object[paramLength]; /// Our new array of castable arguments

        newArgs.args = objs; /// Set our return object with the new castable arguments
        objs[0] = verifySender(sender, types[0]);
        AtomicInteger numUsedStrings = new AtomicInteger(0);
        for (int i=1;i<types.length;i++){
            Class<?> clazz = types[i];
            try{
                if (CommandSender.class == clazz){
                    objs[objIndex] = sender;
                } else if (String[].class == clazz){
                    objs[objIndex] = args;
                } else if (Object[].class == clazz){
                    objs[objIndex] =args;
                } else {
                    objs[objIndex] = verifyArg(sender, clazz, command, args, strIndex, numUsedStrings);
                    if (objs[objIndex] == null){
                        throw new IllegalArgumentException("Argument " + args[strIndex] + " can not be null");
                    }
                }
                if (DEBUG) Log.info("   " + objIndex + " : " + strIndex + "  " +
                        (args.length > strIndex ? args[strIndex] : null ) + " <-> " + objs[objIndex] +" !!! Cs = " +
                        clazz.getCanonicalName());
                if (numUsedStrings.get() > 0){
                    strIndex+=numUsedStrings.get();}
            } catch (ArrayIndexOutOfBoundsException e){
                throw new IllegalArgumentException("You didnt supply enough arguments for this method");
            }
            objIndex++;
        }

        /// Verify alphanumeric
        if (cmd.alphanum().length > 0){
            for (int index: cmd.alphanum()){
                if (index >= args.length)
                    throw new IllegalArgumentException("String Index out of range. ");
                if (!args[index].matches("[a-zA-Z0-9_]*")) {
                    throw new IllegalArgumentException("argument '" + args[index] + "' can only be alphanumeric with underscores");
                }
            }
        }
        return newArgs; 
    }

    protected Object verifySender(CommandSender sender, Class<?> clazz) {
        if (!clazz.isAssignableFrom(sender.getClass())){
            throw new IllegalArgumentException("sender must be a " + clazz.getSimpleName());}
        return sender;
    }

    protected Object verifyArg( CommandSender sender, Class<?> clazz, Command command, String[] args, 
                                            int curIndex, AtomicInteger numUsedStrings) {
        numUsedStrings.set(0);
        
        if ( Command.class == clazz ) return command;

        String string = args[curIndex];
        
        if (string == null) throw new ArrayIndexOutOfBoundsException();
        numUsedStrings.set(1);
        
        if (Player.class == clazz) return verifyPlayer(string);
        else if (OfflinePlayer.class == clazz) return verifyOfflinePlayer(string);
        else if (String.class == clazz) return string;
        else if (Integer.class == clazz || int.class == clazz) return verifyInteger(string);
        else if (Boolean.class == clazz || boolean.class == clazz) return Boolean.parseBoolean(string);
        else if (Object.class == clazz) return string;
        else if (Float.class == clazz || float.class == clazz) return verifyFloat(string);
        else if (Double.class == clazz || double.class == clazz) return verifyDouble(string);
        
        return null;
    }

    private OfflinePlayer verifyOfflinePlayer(String name) throws IllegalArgumentException {
        OfflinePlayer p = ServerUtil.findOfflinePlayer(name);
        if (p == null)
            throw new IllegalArgumentException("Player " + name+" can not be found");
        return p;
    }


    private Player verifyPlayer(String name) throws IllegalArgumentException {
        Player p = ServerUtil.findPlayer(name);
        if (p == null || !p.isOnline())
            throw new IllegalArgumentException(name+" is not online ");
        return p;
    }


    private Integer verifyInteger(Object object) throws IllegalArgumentException {
        try {
            return Integer.parseInt(object.toString());
        }catch (NumberFormatException e){
            throw new IllegalArgumentException(ChatColor.RED+(String)object+" is not a valid integer.");
        }
    }

    private Float verifyFloat(Object object) throws IllegalArgumentException {
        try {
            return Float.parseFloat(object.toString());
        }catch (NumberFormatException e){
            throw new IllegalArgumentException(ChatColor.RED+(String)object+" is not a valid float.");
        }
    }

    private Double verifyDouble(Object object) throws IllegalArgumentException {
        try {
            return Double.parseDouble(object.toString());
        }catch (NumberFormatException e){
            throw new IllegalArgumentException(ChatColor.RED+(String)object+" is not a valid double.");
        }
    }

    /**
     * When no arguments are supplied, no method is found
     * What to display when this happens
     * @param sender the sender
     */
    protected void showHelp(CommandSender sender, Command command){
        showHelp( sender, command, null );
    }

    protected void showHelp(CommandSender sender, Command command, String[] args) {
        int page = 1;

        if ( args != null && args.length > 1 ) {
            try {
                page = Integer.parseInt( args[1] );
            } 
            catch ( NumberFormatException e ){
                MessageUtil.sendMessage( sender, 
                        ChatColor.RED + " " + args[1] + " is not a number, showing help for page 1." );
            }
            if ( page <= 0 ) {
                MessageUtil.sendMessage( sender, ChatColor.RED + " you can only use values > 0" );
                return;
            }
        }
        List<String> available = new ArrayList<>();
        List<String> restricted = new ArrayList<>();
        Set<Method> noDups = new HashSet<>();
        
        for ( MethodWrapper mw : usage ) {
            
            if ( !noDups.add( mw.method ) ) continue;
            
            MCCommand cmd = mw.getCommand();
            
            final String usageMsg = "&6/" + command.getName() + " " + mw.usage;
            
            if ( cmd.op() && sender.isOp() ) 
                restricted.add( ChatColor.AQUA + "[OP only] &6" + usageMsg );
            
            else if (   cmd.admin() 
                        && (    sender.hasPermission( Permissions.ADMIN_NODE ) 
                                || sender.isOp() ) )                        
                restricted.add( ChatColor.AQUA + "[Admins only] &6" + usageMsg );
            
            else if (   cmd.perm().isEmpty() 
                        || (    !cmd.perm().isEmpty() 
                                && sender.hasPermission( cmd.perm() ) ) ) 
                available.add( usageMsg );

        }       
        if ( sender.isOp() || sender.hasPermission( Permissions.ADMIN_NODE ) )
            available.addAll( restricted );

        int npages = available.size();
        
        npages = (int) Math.ceil( (double) npages / LINES_PER_PAGE ); 
        
        if ( npages <= 0 ) {
            MessageUtil.sendMessage(sender, "&4There are no available sub-commands" );
            return;
        }
        if ( page > npages ) {
            MessageUtil.sendMessage(sender, "&4That page doesnt exist, try 1-" + npages );
            return;
        }
      
        if ( command != null && command.getAliases() != null && !command.getAliases().isEmpty() ) {
            
            String aliases = String.join( ", ", command.getAliases() );
            MessageUtil.sendMessage(
                    sender, "&eShowing page &6" + page + "/" + npages + "&6 : /" + command.getName() + " help <page number>");
            MessageUtil.sendMessage(
                    sender, "&e    command &6" + command.getName() + "&e has aliases: &6" + aliases );
        } 
        else {
            MessageUtil.sendMessage( sender, "&eShowing page &6" + page + "/" + npages + "&6 : /cmd help <page number>" );
        }
        
        for ( String each : available.subList( LINES_PER_PAGE * ( page - 1 ), 
                                               Math.min( available.size() -1, page * LINES_PER_PAGE ) ) ) {
            MessageUtil.sendMessage( sender, each );
        }
    }
}
