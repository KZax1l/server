package tigase.server;

/**
 * Represent one XMPP packet.
 */
public class Packet {
    public Packet() {
    }

    /**
     * Returns packet destination address.
     */
    public String getTo() {
        return null;
    }

    /**
     * Returns packet source address.
     */
    public String getFrom() {
        return null;
    }

    /**
     * Tells whether data kept by this packet has been already parsed or if they in raw format.
     */
    public boolean isParsed() {
        return false;
    }

    public byte[] getData() {
        return null;
    }
}