package tigase.server;

/**
 * Interface ComponentRegistratorIfc
 * Collects information about all ServerComponentsIfc connected to MessageRouter
 */
public interface ComponentRegistratorIfc extends ServerComponentIfc {
    /**
     * @param component
     */
    void addComponent(ServerComponentIfc component);

    void deleteComponent(ServerComponentIfc component);
}