package net.covers1624.classloader.api;

/**
 * A bouncer.
 * Used by LaunchBouncer.
 * By default the 'id' for your IBounceClass is its Class name.
 * By using {@link BounceId}, you can provide a custom name.
 *
 * Created by covers1624 on 5/11/18.
 */
public interface IBounceClass {

    /**
     * Replacement for your standard static main method.
     *
     * @param args The rest of the args passed into the program.
     * @throws Throwable Makes things simpler. This will just propagate out LaunchBouncer.
     */
    void main(String[] args) throws Throwable;
}
