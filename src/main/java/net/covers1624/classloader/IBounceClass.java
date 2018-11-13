package net.covers1624.classloader;

/**
 * A bouncer.
 * Used by LaunchBouncer.
 * If multiple IBounceClasses are found with the same 'id' an
 * exception will be thrown.
 *
 * Created by covers1624 on 5/11/18.
 */
public interface IBounceClass {

    /**
     * The 'id' for this bouncer.
     * Used as the first parameter for the program when using LaunchBouncer.
     *
     * @return The id.
     */
    String getId();

    /**
     * Replacement for your standard static main method.
     *
     * @param args The rest of the args passed into the program.
     * @throws Throwable Makes things simpler. This will just propagate out LaunchBouncer.
     */
    void main(String[] args) throws Throwable;
}
