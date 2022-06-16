package manager;

public class EventTimer {
    public long endTickTime;
    private boolean hasRung = false;
    private RingEventInterface ringer;

    public EventTimer(long targetTick, RingEventInterface ringer) {
        this.endTickTime = targetTick;
        this.ringer = ringer;
    }

    public long getEndTickTime() { return endTickTime; }
    public boolean hasRung() { return hasRung; }

    public void addTickTime(long ticks) {
        endTickTime += ticks;
    }

    public void ring(GameEngine engine) {
        hasRung = true;
        ringer.ring(engine);
    }
}
