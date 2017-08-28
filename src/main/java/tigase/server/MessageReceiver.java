package tigase.server;

/**
 * Objects of this type can receive messages. They can be in fact routing destination depending on target address.
 * Message are routed to proper destination in MessageRouter class.
 */
public interface MessageReceiver extends ServerComponentIfc {
    /**
     * Returns array of Strings. Each String should be a regular expression defining destination addresses for which this receiver can process messages.
     * There can be more than one message receiver for each messages.
     */
    String routingAddresses();

    /**
     * @param packet
     */
    void addMessage(Packet packet);
}