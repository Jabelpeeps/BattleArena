package mc.alk.arena.serializers;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import mc.alk.arena.controllers.Modules;
import mc.alk.arena.modules.Grenades;
import mc.alk.arena.modules.Paintballs;
import mc.alk.arena.objects.modules.ArenaModule;
import mc.alk.util.Log;

import org.apache.commons.lang.StringUtils;

public class ModuleLoader {

    public void loadModules(File moduleDirectory) {
        List<String> loadedModules = new ArrayList<String>();
        for (ArenaModule mod : new ArenaModule[]{new Paintballs(), new Grenades()}) {
            Modules.addModule(mod);
            loadedModules.add(mod.getName());
            mod.setEnabled(true);
        }
        if (!moduleDirectory.exists()) {
            return;
        }

        for (File mod : moduleDirectory.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                int period = name.lastIndexOf('.');
                final String extension = name.substring(period + 1);
                return period != -1 && extension.equals("class") || extension.equals("jar");
            }
        })) {
            ArenaModule am = null;
            try {
                am = loadModule(moduleDirectory, mod);
                loadedModules.add(am.getName());
                am.setEnabled(true);
                am.onEnable();
                Modules.addModule(am);
            } catch (Exception ex) {
                Log.err("[BA Error] Error loading the module " + mod.toString());
                if (am != null) {
                    am.setEnabled(false);
                }
                Log.printStackTrace(ex);
            }
        }
        Log.info(Log.colorChat("[BattleArena] Modules (" + loadedModules.size() + ") [&a" + StringUtils.join(loadedModules, "&f, &a") + "&f]"));

    }

    private ArenaModule loadModule(File dir, File mod) throws Exception {
        ClassLoader loader = this.getClass().getClassLoader();
        URL url = dir.toURI().toURL();

        URL[] urls = {url};
        URLClassLoader ucl = new URLClassLoader(urls, loader);

        //Load the class
        String shortName = mod.getName().substring(0, mod.getName().indexOf('.'));
        System.out.println("ArenaModule::loadModule(" + mod.getName() + "); // shortName = " + shortName);
        System.out.println("dir.toURI().toURL() = " + dir.toURI().toURL());
        Class<?> clazz = ucl.loadClass(shortName);
        Class<?>[] args = {};
        Class<? extends ArenaModule> moduleClass = clazz.asSubclass(ArenaModule.class);
        Constructor<?> constructor = moduleClass.getConstructor(args);
        return (ArenaModule) constructor.newInstance((Object[]) args);
    }
}
