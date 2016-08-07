package mc.alk.util.version;


import java.util.Collection;

import org.bukkit.plugin.Plugin;

/**
 * Provides for easy construction of IPlugin Testers. <br/><br/>
 * 
 * @author Nikolai
 */
public class TesterFactory<T extends Plugin> {
    
    /**
     * Creates a new Tester object of Type IPlugin that checks if the IPlugin.isEnabled().
     */
    public static Tester<Plugin> getNewTester(Plugin iplugin) {

        if (iplugin == null) {
            return getShortCircuitTester();
        }
        
        Predicate<Plugin> predicate = new Predicate<Plugin>() {

            @Override
            public boolean test(Plugin t) {
                return t.isEnabled();
            }
        };

        return new Tester<>(predicate, iplugin);
    }

    /**
     * The default Tester always succeeds (returns true).
     * 
     * @return A new Tester object where its test() method always returns true.
     */
    public static Tester<Plugin> getDefaultTester() {
        return new Tester<Plugin>(Predicate.TRUE, null);
    }
    
    /**
     * The ShortCircuit Tester always fails (returns false).
     * 
     * @return A new Tester object where its test() method always returns false;
     */
    public static Tester<Plugin> getShortCircuitTester() {
        return new Tester<Plugin>(Predicate.FALSE, null);
    }
    
    /**
//     * A Tester that checks if the fields of T are not null.
//     * This method was added because Admins were installing broken plugins.
//     * These plugins would cause errors during onEnable() which meant that 
//     * their fields weren't fully initialized.
//     * 
//     * So merely checking that the plugin is installed & version checking is not enough:
//     * We should also verify that they're not broken (fields are not null).
//     * @param <T>
//     * @param t
//     * @return -- true if all fields are initialized (not null), false if any field is null.
//     * @since v3.0.0-SNAPSHOT
//     */
//    public static <T> Tester<T> getUnitTester(T t) {
//        return new Tester<>(getFieldTester(t), t);
//    }
//    
//    public static <T> Predicate<T> getFieldTester(T t) {
//        
//        return new Predicate<T>() {
//
//            @Override
//            public boolean test(T testee) {
//                if (testee == null) return false;
//                Class<?> klass = testee.getClass();
//                String cname = klass.getCanonicalName();
//                String sname = klass.getSimpleName();
//                // Logger.getLogger(cname).info("[TEST]" + cname);
//                Collection<String> nullFields = getNullFields(klass, testee);
//                if (!nullFields.isEmpty()) {
//                    String failure = TesterFactory.toString(nullFields).insert(0, sname).toString();
//                    // Logger.getLogger(cname).severe(failure);
//                    return false;
//                }
//                String success = sname + " is fully initialized";
//                // Logger.getLogger(cname).info(success);
//                return true;
//            }
//        };
//    }
//    
//    /**
//     * This method checks ALL fields, including inherited fields.
//     * @param <T> The object Type.
//     * @param t The object whose fields will be tested for null.
//     * @return A Tester object whose test() method will return true is all fields are not null.
//     *         And test() will return false if any fields are null.
//     * @since v3.0.0-SNAPSHOT
//     */
//    public static <T> Tester<T> getInheritanceTester(T t) {
//        return new Tester<>(getSuperFieldTester(t), t);
//    }
//    
//    /**
//     * This method checks ALL fields, including inherited fields from the Superclass.
//     * @param <T>
//     * @param t
//     * @return False if the object or any of its fields are null.
//     *         True if the object is fully initialized (does not have any null fields).
//     * Null fields labeled with the @Nullable annotation are deemed okay.
//     * @since v3.0.0-SNAPSHOT
//     */
//    public static <T> Predicate<T> getSuperFieldTester(T t) {
//        
//        return new Predicate<T>() {
//
//            @Override
//            public boolean test(T testee) {
//                if (testee == null) return false;
//                Class<?> klass = testee.getClass();
//                String cname = klass.getCanonicalName();
//                String sname = klass.getSimpleName();
//                // Logger.getLogger(cname).info("[TEST]" + cname);
//                Collection<String> nullFields = new ArrayList<>();
//                while (klass != null) {
//                    nullFields.addAll(getNullFields(klass, testee));
//                    klass = klass.getSuperclass();
//                }
//                if (!nullFields.isEmpty()) {
//                    String failure = TesterFactory.toString(nullFields).insert(0, sname).toString();
//                    // Logger.getLogger(cname).severe(failure);
//                    return false;
//                }
//                String success = sname + " is fully initialized";
//                // Logger.getLogger(cname).log(Level.INFO, success);
//                return true;
//            }
//        };
//    }
//    
//    /**
//     * This will return all the null fields of a Class with the exception of 
//     * fields labeled with the @Nullable annotation.
//     * @since v3.0.0-SNAPSHOT
//     */
//    private static <T> Collection<String> getNullFields(Class<?> c, T object) {
//        Collection<String> nullFields = new ArrayList<>();
//        if (object == null) return nullFields;
//        String name = c.getName();
//        Field[] fields = c.getDeclaredFields();
//        for (Field field : fields) {
//            field.setAccessible(true);
//            try {
//                
//                if (field.get(object) == null && !field.isAnnotationPresent(Nullable.class)) {
//                    nullFields.add(field.getName());
//                }
//            } catch (IllegalArgumentException ex) {
//                Logger.getLogger(name).log(Level.SEVERE, null, ex);
//            } catch (IllegalAccessException ex) {
//                Logger.getLogger(name).log(Level.SEVERE, null, ex);
//            }
//        }
//        return nullFields;
//    }
//    
    private static StringBuilder toString(Collection<String> nullFields) {
        StringBuilder msg = new StringBuilder();
        msg.append(" has ").append(nullFields.size()).append(" null fields: ");
        for (String field : nullFields) {
            msg.append(field).append(", ");
        }
        msg.deleteCharAt(msg.length() - 1);
        msg.deleteCharAt(msg.length() - 1);
        return msg;
    }

}