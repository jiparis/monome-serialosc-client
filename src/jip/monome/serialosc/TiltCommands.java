package jip.monome.serialosc;

import java.io.IOException;

import com.illposed.osc.OSCMessage;

public class TiltCommands {
    public static final String MSG_SET = "/tilt/set";
    private MonomeDevice device;
    
    public TiltCommands(MonomeDevice m){
        this.device = m;
    }
    
    public void set(int sensor, int active) {
        OSCMessage msg = new OSCMessage();
        msg.setAddress(device.prefix + MSG_SET);
        msg.addArgument(new Integer(sensor));
        msg.addArgument(new Integer(active));
        send(msg);
    } 
    
    private void send(OSCMessage msg) {
        try {
            device.OSCout.send(msg);
        } catch (IOException e) {
            // Do nothing
        }
    }
}
