package tigase.server;

/**
 * Interface ComponentRegistratorIfc
 * Collects information about all ServerComponentsIfc connected to MessageRouter
 */
public interface ComponentRegistrator extends ServerComponent {
    /**
     * @param component
     */
    boolean addComponent(ServerComponent component);

    boolean deleteComponent(ServerComponent component);
}