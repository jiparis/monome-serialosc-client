package jip.monome.serialosc;

import java.io.IOException;
import com.illposed.osc.OSCMessage;

public class RingCommands {
	public static final String MSG_SET = "/ring/set";
	public static final String MSG_ALL = "/ring/all";
	public static final String MSG_MAP = "/ring/map";
	public static final String MSG_RANGE = "/ring/range";
	
	MonomeDevice ring;
	
	public RingCommands(MonomeDevice grid){
		this.ring = grid;
	}
	
	public void set (int enc, int led, int level){
		OSCMessage msg= new OSCMessage();
		msg.setAddress(ring.prefix + MSG_SET);
		msg.addArgument(new Integer(enc));
		msg.addArgument(new Integer(led));
		msg.addArgument(new Integer(level));
		send(msg);
	}
	
	public void all (int enc, int level){
		OSCMessage msg= new OSCMessage();
		msg.setAddress(ring.prefix + MSG_ALL);
		msg.addArgument(new Integer(enc));
		msg.addArgument(new Integer(level));
		send(msg);
	}
	
	public void map (int enc, byte[] levels){
		OSCMessage msg= new OSCMessage();
		msg.setAddress(ring.prefix + MSG_MAP);
		msg.addArgument(new Integer(enc));
		for (int l: levels)
			msg.addArgument(new Integer(l));
		send(msg);		
	}
	
	public void range (int enc, int x1, int x2, int level){
		OSCMessage msg= new OSCMessage();
		msg.setAddress(ring.prefix + MSG_RANGE);
		msg.addArgument(new Integer(enc));
		msg.addArgument(new Integer(x1));
		msg.addArgument(new Integer(x2));
		msg.addArgument(new Integer(level));
		send(msg);		
	}
	
	private void send (OSCMessage msg){
		try {
			ring.OSCout.send(msg);
		} catch (IOException e) {
			// Do nothing
		}
	}
}
