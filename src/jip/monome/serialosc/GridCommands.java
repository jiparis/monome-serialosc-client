package jip.monome.serialosc;

import java.io.IOException;

import com.illposed.osc.OSCMessage;

public class GridCommands {
	public static final String MSG_SET = "/grid/led/set";
	public static final String MSG_ALL = "/grid/led/all";
	public static final String MSG_MAP = "/grid/led/map";
	public static final String MSG_ROW = "/grid/led/row";
	public static final String MSG_COL = "/grid/led/col";
	public static final String MSG_INT = "/grid/led/intensity";
	
	MonomeDevice grid;
	
	public GridCommands(MonomeDevice grid){
		this.grid = grid;
	}
	
	public void set (int x, int y, int s){
		OSCMessage msg= new OSCMessage();
		msg.setAddress(grid.prefix + MSG_SET);
		msg.addArgument(new Integer(x));
		msg.addArgument(new Integer(y));
		msg.addArgument(new Integer(s));
		send(msg);
	}
	
	public void all (int s){
		OSCMessage msg= new OSCMessage();
		msg.setAddress(grid.prefix + MSG_ALL);
		msg.addArgument(new Integer(s));
		send(msg);
	}
	
	public void map (int xoff, int yoff, int[] masks){
		OSCMessage msg= new OSCMessage();
		msg.setAddress(grid.prefix + MSG_MAP);
		msg.addArgument(new Integer(xoff));
		msg.addArgument(new Integer(yoff));
		for (int mask: masks)
			msg.addArgument(new Integer(mask));
		send(msg);		
	}
	
	public void row (int xoff, int y, byte[] masks){
		OSCMessage msg= new OSCMessage();
		msg.setAddress(grid.prefix + MSG_ROW);
		msg.addArgument(new Integer(xoff));
		msg.addArgument(new Integer(y));
		for (int mask: masks)
			msg.addArgument(new Integer(mask));
		send(msg);		
	}
	
	public void col (int x, int yoff, byte[] masks){
		OSCMessage msg= new OSCMessage();
		msg.setAddress(grid.prefix + MSG_COL);
		msg.addArgument(new Integer(x));
		msg.addArgument(new Integer(yoff));
		for (int mask: masks)
			msg.addArgument(new Integer(mask));
		send(msg);		
	}
	
	public void intensity (int i){
		OSCMessage msg= new OSCMessage();
		msg.setAddress(grid.prefix + MSG_INT);
		msg.addArgument(new Integer(i));
		send(msg);
	}
	
	private void send (OSCMessage msg){
		try {
			grid.OSCout.send(msg);
		} catch (IOException e) {
			// Do nothing
		}
	}
}
