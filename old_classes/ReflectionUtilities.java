//package mc.alk.arena.util;
//
//import java.lang.reflect.Field;
//
//public class ReflectionUtilities {
//
//    /**
//     * sets a value of an {@link Object} via reflection
//     *
//     * @param instance instance the class to use
//     * @param fieldName the name of the {@link Field} to modify
//     * @param value the value to set
//     * @throws SecurityException 
//     * @throws NoSuchFieldException 
//     * @throws IllegalAccessException 
//     * @throws IllegalArgumentException 
//     * @throws Exception
//     */
//    public static void setValue(Object instance, String fieldName, Object value) 
//            throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException { 
//        Field field = instance.getClass().getDeclaredField(fieldName);
//        field.setAccessible(true);
//        field.set(instance, value);
//    }
//
//    /**
//     * get a value of an {@link Object}'s {@link Field}
//     *
//     * @param instance the target {@link Object}
//     * @param fieldName name of the {@link Field}
//     * @return the value of {@link Object} instance's {@link Field} with the
//     * name of fieldName
//     * @throws SecurityException 
//     * @throws NoSuchFieldException 
//     * @throws IllegalAccessException 
//     * @throws IllegalArgumentException 
//     * @throws Exception
//     */
//    public static Object getValue(Object instance, String fieldName) 
//            throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
//        Field field = instance.getClass().getDeclaredField(fieldName);
//        field.setAccessible(true);
//        return field.get(instance);
//    }
//}
