package tigase.server;

/**
 * Interface ComponentRegistratorIfc
 * Collects information about all ServerComponentsIfc connected to MessageRouter
 */
public interface ComponentRegistrator extends ServerComponent {
    /**
     * @param component
     */
    void addComponent(ServerComponent component);

    void deleteComponent(ServerComponent component);
}