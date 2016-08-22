package mc.alk.arena.serializers;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import mc.alk.arena.BattleArena;
import mc.alk.arena.modules.Grenades;
import mc.alk.arena.modules.Paintballs;
import mc.alk.arena.objects.modules.ArenaModule;
import mc.alk.arena.util.Log;

public class ModuleLoader {

    public void loadModules(File moduleDirectory) {
        
        List<String> loadedModules = new ArrayList<>();
        
        for ( ArenaModule mod : new ArenaModule[]{ new Paintballs(), new Grenades() } ) {
            BattleArena.addModule(mod);
            loadedModules.add(mod.getName());
            mod.setEnabled(true);
        }
        if (!moduleDirectory.exists()) return;

        for ( File mod : moduleDirectory.listFiles( 
                       (dir, name) -> {
                                        int period = name.lastIndexOf('.');
                                        final String extension = name.substring(period + 1);
                                        return period != -1 && extension.equals("class") || extension.equals("jar");
                                      } ) ) {
            ArenaModule am = null;
            try {
                am = loadModule(moduleDirectory, mod);
                loadedModules.add(am.getName());
                am.setEnabled(true);
                am.onEnable();
                BattleArena.addModule(am);
            } 
            catch ( ClassNotFoundException | InstantiationException | IllegalAccessException | IOException ex ) {
                Log.err("[BA Error] Error loading the module " + mod.toString());
                Log.printStackTrace(ex);
            }
        }
        Log.info( Log.colorChat( "[BattleArena] Modules (" + loadedModules.size() + ") [&a" + 
                                    StringUtils.join( loadedModules, "&f, &a" ) + "&f]" ) );

    }

    private ArenaModule loadModule(File dir, File mod) 
            throws ClassNotFoundException, InstantiationException, IllegalAccessException, IOException {

        try ( URLClassLoader ucl = new URLClassLoader( new URL[]{dir.toURI().toURL()}, getClass().getClassLoader()) ) {

            String shortName = mod.getName().substring(0, mod.getName().indexOf('.'));
            System.out.println("ArenaModule::loadModule(" + mod.getName() + "); // shortName = " + shortName);
            System.out.println("dir.toURI().toURL() = " + dir.toURI().toURL());
            Class<?> clazz = ucl.loadClass(shortName);
            Class<? extends ArenaModule> moduleClass = clazz.asSubclass(ArenaModule.class);
            return moduleClass.newInstance();
        }
    }
}
