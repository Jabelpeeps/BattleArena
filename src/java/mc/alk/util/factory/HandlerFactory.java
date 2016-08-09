package mc.alk.util.factory;

import org.bukkit.Bukkit;

public class HandlerFactory<T> {

    public T getNewInstance(String handlerName) {
        
        Object object = null;
        String version = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];
        Class<?> clazz = null;
        
        Class<?>[] args = {};
        try {
            clazz = Class.forName( "mc.alk.util.compat." + version + "." + handlerName );
            object = clazz.getConstructor(args).newInstance( (Object[]) args );
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return (T) object;
    }

}
