package dev.greto.nightminigames.client.terror;

/** One shared cadence for the whole cosmetic terror system, measured in Minecraft ticks. */
public final class TerrorTickCadence {
    public static final int INTERVAL=4;
    private int pending;
    public boolean advance(){return ++pending>=INTERVAL&&resetDue();}
    private boolean resetDue(){pending=0;return true;}
    public void reset(){pending=0;}
}
