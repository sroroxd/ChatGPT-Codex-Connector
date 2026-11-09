package de.sprachpilot;

/** Pure sample-clock state machine: 16 kHz, 512 samples = 32 ms. */
public final class TurnDetector {
    public enum Event { NONE, START, END, LIMIT }
    private final int silenceFrames;
    private int voiced, quiet, duration;
    private boolean active;
    public TurnDetector(int silenceMs) { silenceFrames = (silenceMs + 31) / 32; }
    public Event accept(boolean speech) {
        if (!active) {
            voiced = speech ? voiced + 1 : 0;
            if (voiced < 3) return Event.NONE;
            active = true; duration = voiced; quiet = 0;
            return Event.START;
        }
        duration++;
        quiet = speech ? 0 : quiet + 1;
        if (quiet >= silenceFrames) { reset(); return Event.END; }
        if (duration >= 625) { reset(); return Event.LIMIT; }
        return Event.NONE;
    }
    public boolean active() { return active; }
    public void reset() { active = false; voiced = quiet = duration = 0; }
}
