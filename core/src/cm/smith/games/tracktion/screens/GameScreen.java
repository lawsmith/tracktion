package cm.smith.games.tracktion.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.maps.tiled.TiledMapRenderer;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.Iterator;

import cm.smith.games.tracktion.MainGame;
import cm.smith.games.tracktion.controllers.GameController;
import cm.smith.games.tracktion.entities.GameBoard;
import cm.smith.games.tracktion.entities.TrackSegment;
import cm.smith.games.tracktion.entities.Vehicle;
import cm.smith.games.tracktion.systems.AnimationSystem;
import cm.smith.games.tracktion.systems.RenderingSystem;
import cm.smith.games.tracktion.ui.Hud;

/**
 * Created by anthony on 2016-11-03.
 */

public class GameScreen extends BaseScreen {

    public static final float LERP = 10f;
    private Vector3 touchPoint;
    private GameController gameController;
    private Vehicle vehicle;
    private Hud hud;
    private GameBoard gameBoard;
    private TiledMapRenderer gameBoardRenderer;

    public GameScreen(MainGame game, GameController.ROLE role) {
        super(game);

        touchPoint = new Vector3();

        // Add systems to ashley ECS engine
        this.engine.addSystem(new RenderingSystem(this.game.gameBatch));
        this.engine.addSystem(new AnimationSystem());

        // Setup HUD
        hud = new Hud(this, role);
        uiStage.addActor(hud);

        gameBoard = new GameBoard(this);
        gameBoardRenderer = new OrthogonalTiledMapRenderer(gameBoard, 1 / (float)BaseScreen.PIXELS_PER_METER);

        float vehicleX = this.gameCamera.viewportWidth / 2 + gameBoard.waterLayer.getWidth() / 2;
        float vehicleY = this.gameCamera.viewportHeight / 2 + gameBoard.waterLayer.getHeight() / 2;
        vehicle = new Vehicle(this.game, this.physicsWorld, 1.2f, 2.4f,
                new Vector2(vehicleX, vehicleY), (float) Math.PI * 0.5f, 60, 15, 25, 45);

        // Setup middleman that deals with google play services
        gameController = new GameController(role, vehicle, hud, gameBoard);

        // Do some track stuff
        TrackSegment track01 = new TrackSegment(this.game, vehicleX, vehicleY);
        TrackSegment track02 = new TrackSegment(this.game, vehicleX + (track01.texture.getWidth() / BaseScreen.PIXELS_PER_METER), vehicleY);

        this.engine.addEntity(track01);
        this.engine.addEntity(track02);
        this.engine.addEntity(vehicle);

        float newX = vehicle.transformComponent.pos.x;
        float newY = vehicle.transformComponent.pos.y;
        this.gameCamera.position.x = newX;
        this.gameCamera.position.y = newY;
    }

    @Override
    public void show() {
        super.show();

        game.multiplayerServices.setGameManager(gameController);
        game.multiplayerServices.findGame(gameController.getRole().getValue());
    }

    @Override
    public void render(float delta) {
        super.render(delta);

        if (gameController.getRole() == GameController.ROLE.BUILDER) {
            this.gameCamera.zoom = 3f;
        }

        if (gameController.peerFailed) {
            this.gameController.finishedGameTime = this.gameController.time;
            this.gameController.currentState = GameController.STATE.GAME_OVER;
            this.gameController.firstTimeState = true;
            this.gameController.time = GameController.TIME_DEAD;
            this.game.setScreen(new TitleScreen(this.game));
        }
        else if (gameController.peerDisconnected) {
            this.gameController.finishedGameTime = this.gameController.time;
            this.gameController.currentState = GameController.STATE.GAME_OVER;
            this.gameController.firstTimeState = true;
            this.gameController.time = GameController.TIME_DEAD;
            this.game.setScreen(new GameOverScreen(this.game, this.gameController, "Peer Left"));
        }
        else if (gameController.isGameRunning) {
            this.game.multiplayerServices.broadcastMessage();

            // Update the game camera
            float newX = (vehicle.transformComponent.pos.x - this.gameCamera.position.x) * LERP * delta;
            float newY = (vehicle.transformComponent.pos.y - this.gameCamera.position.y) * LERP * delta;
            this.gameCamera.translate(newX, newY);

            switch (gameController.getState()) {
                case PRE_GAME:
                    updatePreGame(delta);
                    break;

                case PLAYING:
                    updatePlaying(delta);
                    break;

                case DEAD:
                    updateDead(delta);
                    break;

                case GAME_OVER:
                    updateGameOver(delta);
                    break;
            }
        }

    }

    @Override
    public void renderBefore(float delta) {
        gameBoardRenderer.setView(this.gameCamera);
        gameBoardRenderer.render();
    }

    private void updatePreGame(float delta) {
        gameController.updatePreGame(delta);
        hud.updateTimer(gameController.time);
    }

    private void updatePlaying(float delta) {
        gameController.updatePlaying(delta);
        hud.updateTimer(gameController.time);

        if (gameController.getRole() == GameController.ROLE.DRIVER) {
            if (hud.isLeftDown) {
                vehicle.setSteer(Vehicle.STEER_LEFT);
            } else if (hud.isRightDown) {
                vehicle.setSteer(Vehicle.STEER_RIGHT);
            } else {
                vehicle.setSteer(Vehicle.STEER_NONE);
            }

            if (hud.isAccelerateDown) {
                vehicle.setAccelerate(Vehicle.ACC_ACCELERATE);
            } else {
                vehicle.setAccelerate(Vehicle.ACC_NONE);
            }

            while (!gameController.segments.isEmpty()) {
                Vector2 point = gameController.segments.pop();
                this.engine.addEntity(new TrackSegment(this.game, point.x, point.y));
            }
        }

        if(gameController.getRole() == GameController.ROLE.BUILDER && Gdx.input.justTouched())
        {
            this.gameCamera.unproject(touchPoint.set(Gdx.input.getX(),Gdx.input.getY(), 0));
            TrackSegment track = new TrackSegment(this.game, touchPoint.x, touchPoint.y);
            this.engine.addEntity(track);

            this.game.multiplayerServices.builderMessage(touchPoint.x, touchPoint.y);
        }

        vehicle.update(delta);
    }

    private void updateDead(float delta) {
        gameController.updateDead(delta);
        hud.updateTimer(gameController.finishedGameTime);

        if (gameController.time <= 0) {
            this.game.setScreen(new GameOverScreen(this.game, this.gameController));
        }
    }

    private void updateGameOver(float delta) {
        gameController.updateGameOver(delta);
        hud.updateTimer(gameController.finishedGameTime);
    }
}
