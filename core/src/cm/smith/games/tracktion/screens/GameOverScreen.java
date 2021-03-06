package cm.smith.games.tracktion.screens;

import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;

import java.text.DecimalFormat;

import aurelienribon.tweenengine.Timeline;
import aurelienribon.tweenengine.Tween;
import aurelienribon.tweenengine.TweenEquations;
import cm.smith.games.tracktion.MainGame;
import cm.smith.games.tracktion.Tweens;
import cm.smith.games.tracktion.controllers.GameController;
import cm.smith.games.tracktion.ui.LabelButton;
import cm.smith.games.tracktion.ui.UILabel;

/**
 * Created by anthony on 2016-12-04.
 */

public class GameOverScreen extends BaseScreen {

    MainGame game;
    GameController gameController;
    DecimalFormat secondsFormatter;
    String newText;
    boolean showRetry;

    UILabel gameOverText;
    UILabel timeLastedText;
    UILabel timeText;
    LabelButton retryBtn;
    LabelButton menuBtn;

    public GameOverScreen(MainGame game, GameController gameController) {
        super(game);
        this.game = game;
        this.gameController = gameController;
        this.newText = "nice try!";
        this.showRetry = true;
        setup();
    }

    public GameOverScreen(MainGame game, GameController gameController, String newText) {
        super(game);
        this.game = game;
        this.gameController = gameController;
        this.newText = newText;
        this.showRetry = false;
        setup();
    }

    private void setup() {
        secondsFormatter = new DecimalFormat("00");

        setupUiElements();
        configureUiContainers();
        transitionIntoScreen();
    }

    @Override
    public void render(float delta) {
        super.render(delta);

        updateTimer(gameController.time);

        switch (gameController.getState()) {
            case DEAD:
                updateDead(delta);
                break;

            case GAME_OVER:
                updateGameOver(delta);
                break;
        }


        if (gameController.shouldDisconnect) {
            GameOverScreen.this.transitionOutScreen(new TitleScreen(this.game));
        }
    }

    private void updateDead(float delta) {
        this.gameController.updateDead(delta);
    }

    private void updateGameOver(float delta) {
        this.gameController.updateGameOver(delta);

        if (gameController.getRole() == GameController.ROLE.DRIVER) {
            if (gameController.restartDriver) {
                retryBtn.setText("- retry? -");
                menuBtn.setText("return to menu");
            }
        }

        if (gameController.getRole() == GameController.ROLE.BUILDER) {
            if (gameController.restartBuilder) {
                retryBtn.setText("- retry? -");
                menuBtn.setText("return to menu");
            }
        }

        if (gameController.time <= 0) {
            if (gameController.restartDriver && gameController.restartBuilder) {
                this.game.setScreen(new GameScreen(this.game, gameController.getRole()));
            } else {
                gameController.shouldDisconnect = true;
            }
        }
    }

    private void updateTimer(float timeToShow) {
        int minutes = (int)(timeToShow / 60);
        int seconds = (int)(timeToShow % 60);
        timeText.setText(minutes + ":" + secondsFormatter.format(seconds));
    }

    private void setupUiElements() {
        timeText = UILabel.makeLabel(this.game, "0:00", 75);
        timeText.setInvisible(true);

        gameOverText = UILabel.makeLabel(this.game, newText, 75);
        gameOverText.setInvisible(true);

        float time = this.gameController.finishedGameTime;
        String timeString = this.gameController.hud.getTimer(time);
        timeLastedText = UILabel.makeLabel(this.game, timeString, 75);
        timeLastedText.setInvisible(true);

        retryBtn = LabelButton.makeButton(this.game, "retry?", new LabelButton.Callback() {
            @Override
            public void onClick() {
                if (gameController.getRole() == GameController.ROLE.DRIVER) {
                    gameController.restartDriver = true;
                }
                if (gameController.getRole() == GameController.ROLE.BUILDER) {
                    gameController.restartBuilder = true;
                }
            }
        });
        retryBtn.setInvisible(true);

        menuBtn = LabelButton.makeButton(this.game, "return to menu", new LabelButton.Callback() {
            @Override
            public void onClick() {
                gameController.shouldDisconnect = true;
                GameOverScreen.this.transitionOutScreen(new TitleScreen(GameOverScreen.this.game));
            }
        });
        menuBtn.setInvisible(true);
    }

    private void configureUiContainers() {
        Table timeTable = new Table();
        timeTable.setFillParent(true);
        timeTable.add(timeText).top();

        Table infoTable = new Table();
        infoTable.padBottom(100f * BaseScreen.SCALE_Y)
                .add(gameOverText)
                .size(512 * SCALE_X, 110 * SCALE_Y)
                .width(MainGame.VIEW_WIDTH / 2)
                .align(Align.right);
        infoTable.row();
        infoTable.padTop(100f * BaseScreen.SCALE_Y)
                .add(timeLastedText)
                .size(512 * SCALE_X, 110 * SCALE_Y)
                .width(MainGame.VIEW_WIDTH / 2)
                .align(Align.center);

        // Configure the two play buttons
        Table buttonTable = new Table();
        if (showRetry) {
            buttonTable.padBottom(100f * BaseScreen.SCALE_Y)
                    .add(retryBtn)
                    .size(512 * SCALE_X, 110 * SCALE_Y)
                    .width(MainGame.VIEW_WIDTH / 2)
                    .align(Align.right);
            buttonTable.row();
            buttonTable.padTop(100f * BaseScreen.SCALE_Y)
                    .add(menuBtn)
                    .size(512 * SCALE_X, 110 * SCALE_Y)
                    .width(MainGame.VIEW_WIDTH / 2)
                    .align(Align.right);
        } else {
            buttonTable.add(menuBtn)
                    .size(512 * SCALE_X, 110 * SCALE_Y)
                    .width(MainGame.VIEW_WIDTH / 2)
                    .align(Align.right);
        }

        // Place game logo and two play buttons side by side
        Table horGroup = new Table();
        horGroup.add(infoTable);
        horGroup.columnDefaults(2);
        horGroup.add(buttonTable);

        Stack stack = new Stack();
        stack.setFillParent(true);

        // Piece it all together into awesomeness
        stack.add(horGroup);
        stack.add(timeTable.top());
        this.uiStage.addActor(stack);
    }

    private void transitionIntoScreen() {
        // Initial intro tween animation
        Timeline.createSequence()
                .beginParallel()
                .push(Tween.from(timeText, Tweens.POSITION_Y, 2f) .targetRelative(-500) .ease(TweenEquations.easeInOutCubic))
                .push(Tween.to(timeText, Tweens.ALPHA, 2.5f) .target(1) .ease(TweenEquations.easeInBack))

                .push(Tween.from(gameOverText, Tweens.POSITION_X, 2f) .targetRelative(-500) .ease(TweenEquations.easeInOutCubic))
                .push(Tween.to(gameOverText, Tweens.ALPHA, 2.5f) .target(1) .ease(TweenEquations.easeInBack))

                .push(Tween.from(timeLastedText, Tweens.POSITION_X, 2f) .targetRelative(-500) .ease(TweenEquations.easeInOutCubic))
                .push(Tween.to(timeLastedText, Tweens.ALPHA, 2.5f) .target(1) .ease(TweenEquations.easeInBack))

                .push(Tween.from(retryBtn, Tweens.POSITION_X, 2f) .targetRelative(500) .ease(TweenEquations.easeInOutCubic) .delay(1f))
                .push(Tween.to(retryBtn, Tweens.ALPHA, 2.5f) .target(1) .ease(TweenEquations.easeInBack) .delay(0.5f))

                .push(Tween.from(menuBtn, Tweens.POSITION_X, 2f) .targetRelative(500) .ease(TweenEquations.easeInOutCubic) .delay(1.5f))
                .push(Tween.to(menuBtn, Tweens.ALPHA, 2.5f) .target(1) .ease(TweenEquations.easeInBack) .delay(1f))
                .end()
                .start(this.tweenManager);
    }

    public void transitionOutScreen(final BaseScreen screen) {
        Timeline tl = Timeline.createSequence()
                .beginParallel()
                .push(Tween.to(timeText, Tweens.ALPHA, 0.5f) .target(0) .ease(TweenEquations.easeOutCubic))
                .push(Tween.to(gameOverText, Tweens.ALPHA, 0.5f) .target(0) .ease(TweenEquations.easeOutCubic))
                .push(Tween.to(timeLastedText, Tweens.ALPHA, 0.5f) .target(0) .ease(TweenEquations.easeOutCubic))
                .push(Tween.to(retryBtn, Tweens.ALPHA, 0.5f) .target(0) .ease(TweenEquations.easeOutCubic))
                .push(Tween.to(menuBtn, Tweens.ALPHA, 0.5f) .target(0) .ease(TweenEquations.easeOutCubic))
                .end();

        this.transitionOut(tl, screen);
    }

}
