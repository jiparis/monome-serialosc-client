package jip.monome.serialosc;

public interface TiltListener {
    /**
     * called when a sensor changes
     * 
     * @param sensor
     * @param x
     * @param y
     * @param z
     */
    public void tilt(int sensor, int x, int y, int z);
}
