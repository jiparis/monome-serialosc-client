package jip.monome.serialosc;

import java.io.IOException;
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
    public static final String DEFAULT_HOST = "localhost";
    public static final String DEFAULT_PREFIX = "/monome";

    public static final String MSG_SIZE = "/sys/size";
    public static final String MSG_PORT = "/sys/port";
    public static final String MSG_ID = "/sys/id";
    public static final String MSG_PREFIX = "/sys/prefix";
    public static final String MSG_HOST = "/sys/host";
    public static final String MSG_INFO = "/sys/info";
    public static final String MSG_ROTATION = "/sys/rotation";

    public static final String MSG_IN_GRID = "/grid/key";
    public static final String MSG_IN_TILT = "/tilt";
    public static final String MSG_IN_ENCD = "/enc/delta";
    public static final String MSG_IN_ENCK = "/enc/key";

    String id, host, prefix;
    int portIn, portOut, sizex, sizey;
    boolean focus;

    OSCPortIn OSCin;
    OSCPortOut OSCout;

    public final GridCommands grid;
    public final RingCommands ring;
    public final TiltCommands tilt;

    ArrayList<GridListener> gridListeners = new ArrayList<GridListener>();
    ArrayList<TiltListener> tiltListeners = new ArrayList<TiltListener>();
    ArrayList<EncListener> encListeners = new ArrayList<EncListener>();

    public MonomeDevice(ServiceInfo info) throws IOException {
        this(info, DEFAULT_PREFIX, DEFAULT_HOST, DEFAULT_PORT);
    }
    
    public MonomeDevice(ServiceInfo info, String prefix) throws IOException {
        this(info, prefix, DEFAULT_HOST, DEFAULT_PORT);
    }
    
    public MonomeDevice(ServiceInfo info, String prefix, int portn) throws IOException {
        this(info, prefix, DEFAULT_HOST, portn);
    }

    public MonomeDevice(ServiceInfo info, String prefix, String host, int portn) throws IOException {

        this.portIn = portn;
        this.prefix = prefix;
        this.host = host;
        this.id = info.getName();
        
        // create and open the input port
        OSCin = new OSCPortIn(portn);

        // add sysinfo listeners
        SysInfoListener sysListener = new SysInfoListener();
        OSCin.addListener(MSG_SIZE, sysListener);
        OSCin.addListener(MSG_PORT, sysListener);
        OSCin.addListener(MSG_ID, sysListener);
        OSCin.addListener(MSG_PREFIX, sysListener);
        OSCin.addListener(MSG_HOST, sysListener);

        // add message listeners
        MsgListener msgListener = new MsgListener();
        OSCin.addListener(prefix + MSG_IN_GRID, msgListener);
        OSCin.addListener(prefix + MSG_IN_TILT, msgListener);
        OSCin.addListener(prefix + MSG_IN_ENCD, msgListener);
        OSCin.addListener(prefix + MSG_IN_ENCK, msgListener);

        OSCin.startListening();

        // configure serialosc port
        this.portOut = info.getPort();
        OSCout = new OSCPortOut(info.getInetAddresses()[0], this.portOut);

        // on creation, this object gets the focus        
        setFocus();      
        
        // get device details (id and size)
        OSCMessage infoMsg = new OSCMessage();
        infoMsg.setAddress(MSG_INFO);
        OSCout.send(infoMsg);
       
        // Device commands
        grid = new GridCommands(this);
        ring = new RingCommands(this);
        tilt = new TiltCommands(this);
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

    /**
     * sends out portIn and prefix to get the focus
     * @throws IOException
     */
    public void setFocus() throws IOException{
        setPortIn();
        setPrefix();
        setHost();
        focus = true;
    }
        
    // /sys methods
    /**
     * sets the monome prefix
     * 
     * @param prefix
     * @throws IOException
     */
    private void setPrefix() throws IOException {
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
    private void setPortIn() throws IOException {
        OSCMessage portMsg = new OSCMessage();
        portMsg.setAddress(MSG_PORT);
        portMsg.addArgument(new Integer(portIn));
        OSCout.send(portMsg);
    }
    
    /**
     * sends the app host to the monome
     * 
     * @param portIn
     * @throws IOException
     */
    private void setHost() throws IOException {
        OSCMessage hostMsg = new OSCMessage();
        hostMsg.setAddress(MSG_HOST);
        hostMsg.addArgument(host);
        OSCout.send(hostMsg);
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
    
    public boolean isFocus(){
        return this.focus;
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

        boolean focusPort = true, 
                focusHost = true, 
                focusPrefix = true;
        
        // OSCListener interface
        @Override
        public void acceptMessage(Date date, OSCMessage msg) {
            String address = msg.getAddress();
            if (MSG_SIZE.equals(address)) {
                sizex = ((Integer) msg.getArguments()[0]).intValue();
                sizey = ((Integer) msg.getArguments()[1]).intValue();

            } else if (MSG_ID.equals(address)) {
                id = (String) msg.getArguments()[0];
                
            // stop sending messages when focus lost
            } else if (MSG_PORT.equals(address)) {
                int newPort = ((Integer) msg.getArguments()[0]).intValue();
                focusPort = (newPort == portIn);               
            } else if (MSG_HOST.equals(address)) {
//                if(host != null){
//                    String newHost = (String) msg.getArguments()[0];                
//                    focusHost = host.equals(newHost);
//                }
            } else if (MSG_PREFIX.equals(address)) {
                String newPrefix = (String) msg.getArguments()[0];
                focusPrefix = prefix.equals(newPrefix);
            }
            focus = (focusPort && focusHost && focusPrefix);
            
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
