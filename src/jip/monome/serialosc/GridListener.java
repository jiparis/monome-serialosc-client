package jip.monome.serialosc;

public interface GridListener {
    /**
     * called when a button is pressed or released
     * 
     * @param x
     * @param y
     * @param state
     */
    public void press(int x, int y, int state);
}
