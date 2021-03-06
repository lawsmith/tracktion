package cm.smith.games.tracktion.controllers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Queue;

import java.awt.Point;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import cm.smith.games.tracktion.entities.GameBoard;
import cm.smith.games.tracktion.entities.TrackSegment;
import cm.smith.games.tracktion.entities.Vehicle;
import cm.smith.games.tracktion.ui.Hud;

/**
 * Created by anthony on 2016-09-18.
 */
public class GameController {

    public enum ROLE {
        DRIVER(0x1),
        BUILDER(0x2);

        private final long role;
        ROLE(long role) { this.role = role; }
        public long getValue() { return role; }
        private static final Map<Long, ROLE> lookup = new HashMap<Long, ROLE>();

        static {
            for (ROLE d : ROLE.values()) {
                lookup.put(d.getValue(), d);
            }
        }

        public static ROLE get(long abbreviation) {
            return lookup.get(abbreviation);
        }
    }

    public enum STATE {
        PRE_GAME((byte)'P'),
        PLAYING((byte)'L'),
        DEAD((byte)'D'),
        GAME_OVER((byte)'G'),
        DISCONNECT((byte)'T');

        private final byte state;
        STATE(byte state) { this.state = state; }
        public byte getValue() { return state; }
        private static final Map<Byte, STATE> lookup = new HashMap<Byte, STATE>();

        static {
            for (STATE d : STATE.values()) {
                lookup.put(d.getValue(), d);
            }
        }

        public static STATE get(byte abbreviation) {
            return lookup.get(abbreviation);
        }
    }

    public boolean isGameRunning;
    public STATE currentState;
    private ROLE currentRole;
    public boolean firstTimeState;     // tracks the first time update of state
    public boolean sentCrashMsg;
    public boolean shouldDisconnect;
    public boolean sentDisconnectMsg;
    public boolean restartDriver;
    public boolean restartBuilder;
    public boolean peerDisconnected;
    public boolean peerFailed;

    public static float TIME_PREGAME = 5f;
    public static float TIME_PLAYING = 0f;
    public static float TIME_DEAD = 3f;
    public static float TIME_GAMEOVER = 20f;

    // Store all of the network shared entities in the controller
    public float time = TIME_PREGAME;    // time left for the current state
    public float finishedGameTime = 0;
    public Vehicle vehicle;
    public Hud hud;
    public GameBoard gameBoard;
    public Stack<Vector2> segments;

    public GameController(ROLE role, Vehicle vehicle, Hud hud, GameBoard gameBoard) {
        segments = new Stack<Vector2>();
        isGameRunning = false;
        currentState = STATE.PRE_GAME;
        firstTimeState = true;
        sentCrashMsg = false;
        shouldDisconnect = false;
        sentDisconnectMsg = false;
        restartDriver = false;
        restartBuilder = false;
        peerDisconnected = false;
        peerFailed = false;

        this.currentRole = role;
        this.vehicle = vehicle;
        this.hud = hud;
        this.gameBoard = gameBoard;
    }

    public ROLE getRole() {
        return this.currentRole;
    }

    public STATE getState() {
        return this.currentState;
    }

    public void addSegment(float x, float y) {
        Vector2 point = new Vector2(x, y);
        this.segments.push(point);
    }

    public void updatePreGame(float delta) {
        time -= delta;
        if (firstTimeState) {
            time = TIME_PREGAME;
            firstTimeState = false;
            hud.playText("get ready!");
        }
        if (time <= 0) {
            currentState = STATE.PLAYING;
            firstTimeState = true;
        }
    }

    public void updatePlaying(float delta) {
        time += delta;
        if (firstTimeState) {
            time = TIME_PLAYING;
            firstTimeState = false;
            hud.playText("GO");
        }
        if (getRole() == ROLE.DRIVER && vehicle.getSpeedKMH() <= 20 && time > 2) {
            vehicle.setDead(true);
        }
        if (vehicle.isDead()) {
            finishedGameTime = time;
            currentState = STATE.DEAD;
            firstTimeState = true;
        }
    }

    public void updateDead(float delta) {
        time -= delta;
        if (firstTimeState) {
            time = TIME_DEAD;
            firstTimeState = false;
            hud.playText("nice try!");
        }
        if (time <= 0) {
            currentState = STATE.GAME_OVER;
            firstTimeState = true;
        }
    }

    public void updateGameOver(float delta) {
        time -= delta;
        if (firstTimeState) {
            time = TIME_GAMEOVER;
            firstTimeState = false;
        }
        if (time <= 0) {
            time = 0;
        }
    }

}
