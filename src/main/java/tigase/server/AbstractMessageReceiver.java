package tigase.server;

import tigase.annotations.TODO;
import tigase.stats.StatRecord;
import tigase.stats.StatisticType;
import tigase.stats.StatisticsContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public abstract class AbstractMessageReceiver extends Thread implements MessageReceiver, StatisticsContainer {
    private MessageReceiver parent = null;
    private String[] addresses = null;
    private int maxQueueSize = Integer.MAX_VALUE;

    private LinkedBlockingQueue<Packet> queue = null;
    private boolean stopped = false;

    private long statAddedMessagesOk = 0;
    private long statAddedMessagesEr = 0;

    public AbstractMessageReceiver(String[] addresses, int maxQueueSize, MessageReceiver parent) {
        this.addresses = addresses;
        if (maxQueueSize > 0) {
            this.maxQueueSize = maxQueueSize;
        } // end of if (maxQueueSize > 0)
        this.parent = parent;
    }

    @Override
    public String[] routingAddresses() {
        return addresses;
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
}
