package jip.monome.serialosc;

import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Date;

import javax.jmdns.ServiceInfo;

import com.illposed.osc.OSCListener;
import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPortIn;
import com.illposed.osc.OSCPortOut;

/**
 * Represents a monome (grid or arc) device
 * 
 * @author jip
 * 
 */
public class MonomeDevice {
    public static final int DEFAULT_PORT = 8000;
    public static final String DEFAULT_PREFIX = "/monome";

    public static final String MSG_SIZE = "/sys/size";
    public static final String MSG_PORT = "/sys/port";
    public static final String MSG_ID = "/sys/id";
    public static final String MSG_PREFIX = "/sys/prefix";
    public static final String MSG_HOST = "/sys/host";
    public static final String MSG_INFO = "/sys/info";
    public static final String MSG_ROTATION = "/sys/rotation";

    public static final String MSG_IN_GRID = "/grid/key";
    public static final String MSG_IN_TILT = "/tilt/tilt";
    public static final String MSG_IN_ENCD = "/enc/delta";
    public static final String MSG_IN_ENCK = "/enc/key";

    String id, host, prefix;
    int portIn, portOut, sizex, sizey;

    OSCPortIn OSCin;
    OSCPortOut OSCout;

    public final GridCommands grid;
    public final RingCommands ring;

    ArrayList<GridListener> gridListeners = new ArrayList<GridListener>();
    ArrayList<TiltListener> tiltListeners = new ArrayList<TiltListener>();
    ArrayList<EncListener> encListeners = new ArrayList<EncListener>();

    public MonomeDevice(ServiceInfo info) throws IOException {
        this(info, DEFAULT_PREFIX, DEFAULT_PORT);
    }

    public MonomeDevice(ServiceInfo info, String prefix, int portn) throws IOException {

        OSCin = new OSCPortIn(portn);

        SysInfoListener sysListener = new SysInfoListener();
        OSCin.addListener(MSG_SIZE, sysListener);
        OSCin.addListener(MSG_PORT, sysListener);
        OSCin.addListener(MSG_ID, sysListener);
        OSCin.addListener(MSG_PREFIX, sysListener);
        OSCin.addListener(MSG_HOST, sysListener);

        MsgListener msgListener = new MsgListener();
        OSCin.addListener(prefix + MSG_IN_GRID, msgListener);
        OSCin.addListener(prefix + MSG_IN_TILT, msgListener);
        OSCin.addListener(prefix + MSG_IN_ENCD, msgListener);
        OSCin.addListener(prefix + MSG_IN_ENCK, msgListener);

        OSCin.startListening();

        this.portOut = info.getPort();
        OSCout = new OSCPortOut(info.getInetAddresses()[0], this.portOut);

        setPortIn(portn);

        setPrefix(prefix);

        OSCMessage infoMsg = new OSCMessage();
        infoMsg.setAddress(MSG_INFO);
        OSCout.send(infoMsg);
       
        // Device commands
        grid = new GridCommands(this);
        ring = new RingCommands(this);
    }

    /**
     * hooks a listener of monome events.
     * 
     * @param l
     */
    public void addListener(Object l) {
        if (l instanceof GridListener)
            gridListeners.add((GridListener) l);
        if (l instanceof TiltListener)
            tiltListeners.add((TiltListener) l);
        if (l instanceof EncListener)
            encListeners.add((EncListener) l);
    }

    // /sys methods
    /**
     * sets the monome prefix
     * 
     * @param prefix
     * @throws IOException
     */
    public void setPrefix(String prefix) throws IOException {
        this.prefix = prefix;
        OSCMessage prefixMsg = new OSCMessage();
        prefixMsg.setAddress(MSG_PREFIX);
        prefixMsg.addArgument(prefix);
        OSCout.send(prefixMsg);
    }

    /**
     * sends the app port to the monome
     * 
     * @param portIn
     * @throws IOException
     */
    public void setPortIn(int portIn) throws IOException {
        this.portIn = portIn;
        OSCMessage portMsg = new OSCMessage();
        portMsg.setAddress(MSG_PORT);
        portMsg.addArgument(new Integer(portIn));
        OSCout.send(portMsg);
    }

    /**
     * sets the monome rotation
     * 
     * @param rot
     * @throws IOException
     */
    public void setRotation(int rot) throws IOException {
        OSCMessage rotMsg = new OSCMessage();
        rotMsg.setAddress(MSG_ROTATION);
        rotMsg.addArgument(new Integer(rot));
        OSCout.send(rotMsg);
    }

    // some useful methods

    public String getId() {
        return this.id;
    }

    public String getPrefix() {
        return this.prefix;
    }

    public int getSizeX() {
        return this.sizex;
    }

    public int getSizeY() {
        return this.sizey;
    }

    @Override
    protected void finalize() throws Throwable {
        OSCin.close();
        OSCout.close();
        super.finalize();
    }

    /**
     * manages /sys messages
     * 
     */
    class SysInfoListener implements OSCListener {

        // OSCListener interface
        @Override
        public void acceptMessage(Date date, OSCMessage msg) {
            String address = msg.getAddress();
            if (MSG_SIZE.equals(address)) {
                sizex = ((Integer) msg.getArguments()[0]).intValue();
                sizey = ((Integer) msg.getArguments()[1]).intValue();

            } else if (MSG_HOST.equals(address)) {
                host = (String) msg.getArguments()[0];

            } else if (MSG_ID.equals(address)) {
                id = (String) msg.getArguments()[0];

            } else if (MSG_PORT.equals(address)) {
                portIn = ((Integer) msg.getArguments()[0]).intValue();

            } else if (MSG_PREFIX.equals(address)) {
                prefix = (String) msg.getArguments()[0];

            }
        }
    }

    /**
     * manages event messages
     * 
     */
    class MsgListener implements OSCListener {

        // OSCListener interface
        @Override
        public void acceptMessage(Date date, OSCMessage msg) {
            String address = msg.getAddress();
            Object[] args = msg.getArguments();
            if (address.endsWith(MSG_IN_GRID)) {
                Integer x = (Integer) args[0];
                Integer y = (Integer) args[1];
                Integer state = (Integer) args[2];

                for (GridListener l : gridListeners) {
                    l.press(x, y, state);
                }
            } else if (address.endsWith(MSG_IN_TILT)) {
                Integer sensor = (Integer) args[0];
                Integer x = (Integer) args[1];
                Integer y = (Integer) args[2];
                Integer z = (Integer) args[3];

                for (TiltListener l : tiltListeners) {
                    l.tilt(sensor, x, y, z);
                }
            } else if (address.endsWith(MSG_IN_ENCD)) {
                Integer encoder = (Integer) args[0];
                Integer delta = (Integer) args[1];
                for (EncListener l : encListeners) {
                    l.delta(encoder, delta);
                }
            } else if (address.endsWith(MSG_IN_ENCK)) {
                Integer encoder = (Integer) args[0];
                Integer state = (Integer) args[1];
                for (EncListener l : encListeners) {
                    l.press(encoder, state);
                }
            }

        }

    }
}
