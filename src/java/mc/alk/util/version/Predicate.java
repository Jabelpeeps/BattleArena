package mc.alk.util.version;


/**
 * The Predicate preforms tests on objects of type T and returns true or false.
 * 
 * @author Nikolai
 * @param <T> The type of objects to be tested.
 */
public interface Predicate<T> {
    
    public boolean test(T t);
    
    /**
     * Always returns true.
     */
    public static final Predicate TRUE = new Predicate() {

        @Override
        public boolean test(Object t) {
            return true;
        }
    };
    
    /**
     * Always returns false.
     */
    public static final Predicate FALSE = new Predicate() {

        @Override
        public boolean test(Object t) {
            return false;
        }
    };

}