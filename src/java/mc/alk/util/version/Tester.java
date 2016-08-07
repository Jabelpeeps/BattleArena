package mc.alk.util.version;


/**
 * The Predicate Tester is used to optionally pass an additional isEnabled() check to the Version object. <br/><br/>
 *
 * It is a wrapper for the predicate and the object to be tested:
 * in other words, Tester.test() merely uses predicate.test(object).
 *
 * Notice that if the test returns false, then the compatibility check will stop, (as intended) 
 * because if the plugin is not enabled, then it's not compatible.
 *
 * @author Nikolai
 */
public class Tester<T> {

    Predicate<T> predicate;
    T object;

    public Tester(Predicate<T> tester, T testee) {
        this.predicate = tester;
        this.object = testee;
    }


    public boolean test() {
        return predicate.test(object);
    }

}