package mc.alk.util.factory;

import mc.alk.util.version.VersionFactory;

public class HandlerFactory<T> {

    public T getNewInstance(String handlerName) {
        Object object = null;
        String version = VersionFactory.getNmsPackage();
        Class clazz = null;
        Class<?>[] args = {};
        try {
            clazz = Class.forName("mc.alk.util.compat." + version + "." + handlerName);
            object = clazz.getConstructor(args).newInstance((Object[]) args);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return (T) object;
    }

}
