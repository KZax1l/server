package tigase.server;

import tigase.annotations.TODO;
import tigase.conf.Configurable;
import tigase.stats.StatRecord;
import tigase.stats.StatisticType;
import tigase.stats.StatisticsContainer;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

public abstract class AbstractMessageReceiver extends Thread implements MessageReceiver, StatisticsContainer, Configurable {
    public static final String MAX_QUEUE_SIZE_PROP_NAME = "max-queue-size";
    public static final Integer MAX_QUEUE_SIZE_PROP_VALUE = Integer.MAX_VALUE;
    public static final String ROUTING_ADDRESSES_PROP_NAME = "routing-addresses";
    public static final String[] ROUTING_ADDRESSES_PROP_VALUE = {"*"};

    private MessageReceiver parent = null;
    private String[] routingAddresses = ROUTING_ADDRESSES_PROP_VALUE;
    private int maxQueueSize = MAX_QUEUE_SIZE_PROP_VALUE;

    private LinkedBlockingQueue<Packet> queue = null;
    private boolean stopped = false;

    private long statAddedMessagesOk = 0;
    private long statAddedMessagesEr = 0;

    public AbstractMessageReceiver(MessageReceiver parent) {
        this.parent = parent;
    }

    /**
     * Method <code>routingAddresses</code> returns array of Strings.
     * Each String should be a regular expression
     * defining destination addresses for which this receiver can process
     * messages. There can be more than one message receiver for each messages.
     *
     * @return a <code>String</code> value
     */
    public String[] getRoutingAddresses() {
        return routingAddresses;
    }

    @Override
    public boolean addMessage(Packet packet, boolean blocking) {
        boolean result = true;
        if (blocking) {
            try {
                queue.put(packet);
            } // end of try
            catch (InterruptedException e) {
                result = false;
            } // end of try-catch
        } // end of if (blocking)
        else {
            result = queue.offer(packet);
        } // end of if (blocking) else
        if (result) ++statAddedMessagesOk;
        else ++statAddedMessagesEr;
        return result;
    }

    @TODO(note = "Consider better implementation for a case when addMessage fails, maybe packet which couldn't be handled should be put back to input queue.")
    public boolean addMessages(Queue<Packet> packets, boolean blocking) {
        if (packets == null || packets.size() == 0) {
            return false;
        } // end of if (packets != null && packets.size() > 0)
        Packet packet = null;
        boolean result = true;
        while (result && ((packet = packets.poll()) != null)) {
            result = addMessage(packet, blocking);
        } // end of while (result && (packet = packets.poll()) != null)
        return result;
    }

    public void run() {
        queue = new LinkedBlockingQueue<Packet>(maxQueueSize);
        while (!stopped) {
            try {
                Packet packet = queue.take();
                Queue<Packet> result = processPacket(packet);
                parent.addMessages(result, true);
            } // end of try
            catch (InterruptedException e) {
                stopped = true;
            } // end of try-catch
        } // end of while (! stopped)
    }

    public abstract Queue<Packet> processPacket(Packet packet);

    public int queueSize() {
        return queue.size();
    }

    /**
     * Returns defualt configuration settings for this object.
     */
    public List<StatRecord> getStatistics() {
        List<StatRecord> stats = new ArrayList<StatRecord>();
        stats.add(new StatRecord(StatisticType.QUEUE_SIZE, queue.size()));
        stats.add(new StatRecord(StatisticType.MSG_RECEIVED_OK, statAddedMessagesOk));
        stats.add(new StatRecord(StatisticType.QUEUE_OVERFLOW, statAddedMessagesEr));
        return stats;
    }

    /**
     * Sets all configuration properties for object.
     */
    public void setProperties(Map<String, ?> properties) {
        Integer queueSize = (Integer) properties.get(MAX_QUEUE_SIZE_PROP_NAME);
        if (queueSize != null) {
            setMaxQueueSize(queueSize);
        } // end of if (queueSize == null)
        String[] addresses = (String[]) properties.get(ROUTING_ADDRESSES_PROP_VALUE);
        if (addresses != null) {
            setRoutingAddresses(addresses);
        } // end of if (addresses != null)
    }

    public void setMaxQueueSize(int maxQueueSize) {
        if (this.maxQueueSize != maxQueueSize) {
            this.maxQueueSize = maxQueueSize;
            if (queue != null) {
                LinkedBlockingQueue<Packet> newQueue = new LinkedBlockingQueue<Packet>(maxQueueSize);
                newQueue.addAll(queue);
                queue = newQueue;
            } // end of if (queue != null)
        } // end of if (this.maxQueueSize != maxQueueSize)
    }

    public void setRoutingAddresses(String[] addresses) {
        routingAddresses = addresses;
    }

    /**
     * Returns defualt configuration settings for this object.
     */
    public Map<String, ?> getDefaults() {
        Map<String, Object> defs = new TreeMap<String, Object>();
        defs.put(MAX_QUEUE_SIZE_PROP_NAME, MAX_QUEUE_SIZE_PROP_VALUE);
        defs.put(ROUTING_ADDRESSES_PROP_NAME, ROUTING_ADDRESSES_PROP_VALUE);
        return defs;
    }
}
