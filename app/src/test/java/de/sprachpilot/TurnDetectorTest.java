package de.sprachpilot;

import org.junit.Test;
import static org.junit.Assert.*;

public final class TurnDetectorTest {
    @Test public void shortNoiseDoesNotStartTurn() {
        TurnDetector d = new TurnDetector(700);
        for (int i=0;i<20;i++) {
            assertEquals(TurnDetector.Event.NONE,d.accept(true));
            assertEquals(TurnDetector.Event.NONE,d.accept(true));
            assertEquals(TurnDetector.Event.NONE,d.accept(false));
        }
        assertFalse(d.active());
    }
    @Test public void endpointWaitsForFullPauseAndResetsOnSpeech() {
        TurnDetector d = new TurnDetector(700);
        d.accept(true); d.accept(true); assertEquals(TurnDetector.Event.START,d.accept(true));
        for (int i=0;i<21;i++) assertEquals(TurnDetector.Event.NONE,d.accept(false));
        assertEquals(TurnDetector.Event.NONE,d.accept(true));
        for (int i=0;i<21;i++) assertEquals(TurnDetector.Event.NONE,d.accept(false));
        assertEquals(TurnDetector.Event.END,d.accept(false));
        assertFalse(d.active());
    }
    @Test public void continuousSpeechIsBoundedAndNextTurnCanStart() {
        TurnDetector d = new TurnDetector(400);
        d.accept(true); d.accept(true); d.accept(true);
        for (int i=3;i<624;i++) assertEquals(TurnDetector.Event.NONE,d.accept(true));
        assertEquals(TurnDetector.Event.LIMIT,d.accept(true));
        d.accept(true); d.accept(true); assertEquals(TurnDetector.Event.START,d.accept(true));
    }
}
