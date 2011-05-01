package jip.monome.serialosc;

public interface EncListener {
	/**
	 * called when an encoder changes
	 * @param enc
	 * @param delta
	 */
	public void delta (int enc, int delta);
	
	/**
	 * called when an encoder is pressed
	 * @param enc
	 * @param state
	 */
	 public void press (int enc, int state);
	 

}
