package manager;

import model.hero.Mario;
import sml.smlRunStepSize;
import view.ImageLoader;
import view.StartScreenSelection;
import view.UIManager;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.ListIterator;

public class GameEngine implements Runnable {

    private final static int WIDTH = 1268, HEIGHT = 708,
                             FPS = 60;

    private MapManager mapManager;
    private UIManager uiManager;
    private SoundManager soundManager;
    private GameStatus gameStatus;
    private boolean isRunning;
    private Camera camera;
    private ImageLoader imageLoader;
    private Thread thread;
    private StartScreenSelection startScreenSelection = StartScreenSelection.START_GAME;
    private int selectedMap = 0;
    private JFrame frame;

    private boolean soarControlled = false;
    private boolean useSoarDebugger = false;
    private String soarAgentPath = null;
    private MarioSoarLink soarLink;

    private JTextField //jtext_humanInput,
                        jtext_agentOutput;

    private ArrayList<EventTimer> eventTimers = null;        // Can be used to add delayed custom events
    private long gameTickTime = 0;

    private GameEngine(String agentPath, boolean openDebugger) {
        if (agentPath != null) {
            this.soarAgentPath = agentPath;
            this.soarControlled = true;
            if (openDebugger) {
                this.useSoarDebugger = true;
            }
        }

        init();
    }

    private void init() {
        imageLoader = new ImageLoader();
        InputManager inputManager = new InputManager(this);
        gameStatus = GameStatus.START_SCREEN;
        camera = new Camera(WIDTH, HEIGHT);
        uiManager = new UIManager(this, WIDTH, HEIGHT, !useSoarDebugger);
        soundManager = new SoundManager();
        mapManager = new MapManager(camera);

        frame = new JFrame("Super Mario Bros.");
        frame.add(uiManager, BorderLayout.CENTER);
        frame.addKeyListener(inputManager);
        frame.addMouseListener(inputManager);

        // Create text areas for Soar to interact with, even if not initially SoarControlled, in case control is enabled during the run
        //jtext_humanInput = new JTextField();
        //jtext_humanInput.setEditable(true);
        //jtext_humanInput.setBounds(50, height-30, 200, 30);
        //this.add(jtext_humanInput);
        jtext_agentOutput = new JTextField();
        jtext_agentOutput.setEditable(false);
        jtext_agentOutput.setBounds(8, HEIGHT-30, WIDTH/2, 30);
        frame.add(jtext_agentOutput, BorderLayout.SOUTH);
        
        setAgentTextBoxVisible(soarControlled);

        frame.pack();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // Inserted code to make game start on first map
        if (soarControlled) {
            selectMapViaIndex(0);
        }

        eventTimers = new ArrayList<EventTimer>();

        start();
    }

    private synchronized void start() {
        if (isRunning)
            return;

        isRunning = true;
        thread = new Thread(this);
        thread.start();
    }

    private void reset() {
        if (isSoarControlled()) {
            setAgentTextBoxVisible(false);
            if (useSoarDebugger) {
                soarLink.getAgent().KillDebugger();
            }
        }
        resetCamera();
        soundManager.stopAllMusic();
        eventTimers.clear();
        setGameStatus(GameStatus.START_SCREEN);
    }

    public void resetCamera(){
        //camera = new Camera(WIDTH, HEIGHT);
        camera.setLocation(0, 0);
    }

    public void restartBackgroundMusic() {
        soundManager.restartBackground();
    }

    public void selectMapViaMouse() {
        String path = uiManager.selectMapViaMouse(uiManager.getMousePosition());
        if (path != null) {
            createMap(path);
        }
    }

    /**
     * Get the path to the .png map that defines the map. The valid .png names are kept by uiManager.
     * Then pass this .png path to createMap, which will generate the level from the .png data.
     */
    public void selectMapViaKeyboard(){
        String path = uiManager.selectMapViaKeyboard(selectedMap);
        if (path != null) {
            createMap(path);
        }
    }

    /**
     * Use the given index of map kept by uiManager. Get the corresponding .png path.
     * Then pass this .png path to createMap, which will generate the level from the .png data.
     */
    public void selectMapViaIndex(int index){
        String path = uiManager.selectMapViaKeyboard(index);
        if (path != null) {
            createMap(path);
        }
    }

    public void changeSelectedMap(boolean up){
        selectedMap = uiManager.changeSelectedMap(selectedMap, up);
    }

    private void createMap(String path) {
        boolean loaded = mapManager.createMap(imageLoader, path);
        if (loaded) {
            setGameStatus(GameStatus.RUNNING);
            soundManager.restartBackground();
            if (isSoarControlled()) {
                setAgentTextBoxVisible(true);
                System.out.println("Running game controlled by Soar agent: "+soarAgentPath);
                soarLink = new MarioSoarLink(soarAgentPath, this);
                if (useSoarDebugger) {
                    soarLink.getAgent().SpawnDebugger();
                    soarLink.getAgent().RunSelf(1); // Run once to get things initted
                    //frame.requestFocus();
                }
            }
        }
        else
            setGameStatus(GameStatus.START_SCREEN);
    }

    public void setTimer(EventTimer eventTimer) {
        this.eventTimers.add(eventTimer);
    }

    public long getCurrentTickTime() { return gameTickTime; }

    public long millisecToTicks(long milli) { return (long) ((long) FPS * (milli / 1000.0)); }

    public long getFutureTickFromMillisec(long milli) { return gameTickTime+millisecToTicks(milli); }

    @Override
    public void run() {
        long lastTime = System.nanoTime();
        double ns = 1000000000 / (double) FPS;
        double delta = 0;
        long timer = System.currentTimeMillis();
        gameTickTime = 0;

        while (isRunning && !thread.isInterrupted()) {

            long now = System.nanoTime();
            delta += (now - lastTime) / ns;
            //if (delta > 1)  // Disable catch-up
            //    delta = 1;
            lastTime = now;

            // Handle timers first
            updateEventTimers(now);

            // Update the game to catch up to the present
            while (delta >= 1) {
                if (gameStatus == GameStatus.RUNNING) {
                    gameLoop();
                }
                else if (gameStatus == GameStatus.MARIO_DEAD) {
                    mapManager.getMario().updateLocation();
                }
                delta--;
                gameTickTime++;
            }
            render();

            // Update Soar only once per render
            if (gameStatus == GameStatus.RUNNING && soarControlled) {
                mapManager.updateSoarInput(soarLink);
                soarLink.runAgent(1);
            }

            if(gameStatus != GameStatus.RUNNING) {
                timer = System.currentTimeMillis();
            }

            if (System.currentTimeMillis() - timer > 1000) {
                timer += 1000;
                mapManager.updateTime();
            }
        }
    }

    private void updateEventTimers(long now) {
        ArrayList<EventTimer> timersToRing = new ArrayList<EventTimer>();
        ListIterator<EventTimer> timerIter = eventTimers.listIterator();
        while (timerIter.hasNext()) {
            EventTimer et = timerIter.next();
            if (gameTickTime >= et.getEndTickTime()) {
                timersToRing.add(et);
                timerIter.remove();
            }
        }
        //eventTimers.removeIf(x -> x.hasRung());
        for (EventTimer et : timersToRing) {
            et.ring(this);
        }
    }

    private void render() {
        uiManager.repaint();
        if (soarControlled)
            uiManager.revalidate();     // Force redraw
    }

    private void gameLoop() {
        updateLocations();
        checkCollisions();
        if (gameStatus != GameStatus.MARIO_DEAD)
            updateCamera();

        /*if (isGameOver()) {
            setGameStatus(GameStatus.GAME_OVER);
        }*/

        int missionPassed = passMission();
        if (missionPassed > -1) {
            mapManager.acquirePoints(missionPassed);
        } 
        else if (mapManager.endLevel()) {
            setGameStatus(GameStatus.MISSION_PASSED);
            mapManager.getMario().setIsVisible(false);
            setEnemiesAnimating(false);
            setTimer(new EventTimer(getFutureTickFromMillisec(4000), new RingEventInterface() {
                @Override
                public void ring(GameEngine engine) {
                    // Restart game
                    engine.reset();
                }
            }));
        }
    }

    private void updateCamera() {
        Mario mario = mapManager.getMario();
        double marioVelocityX = mario.getVelX();
        double shiftAmount = 0;

        if (marioVelocityX > 0 && mario.getX() > camera.getX()+600
         || marioVelocityX < 0 && mario.getX() < camera.getX()+400) {
            shiftAmount = marioVelocityX;

            if (camera.getX()+shiftAmount < 0)
                shiftAmount = -camera.getX();
        }

        camera.moveCam(shiftAmount, 0);
    }

    private void updateLocations() {
        mapManager.updateLocations();
    }

    private void checkCollisions() {
        mapManager.checkCollisions(this);
    }

    public void receiveInput(ButtonAction input) {

        if (gameStatus == GameStatus.START_SCREEN) {
            if (input == ButtonAction.SELECT && startScreenSelection == StartScreenSelection.START_GAME) {
                startGame();
            } else if (input == ButtonAction.SELECT && startScreenSelection == StartScreenSelection.VIEW_ABOUT) {
                setGameStatus(GameStatus.ABOUT_SCREEN);
            } else if (input == ButtonAction.SELECT && startScreenSelection == StartScreenSelection.VIEW_HELP) {
                setGameStatus(GameStatus.HELP_SCREEN);
            } else if (input == ButtonAction.GO_UP) {
                selectOption(true);
            } else if (input == ButtonAction.GO_DOWN) {
                selectOption(false);
            }
        }
        else if(gameStatus == GameStatus.MAP_SELECTION){
            if(input == ButtonAction.SELECT){
                selectMapViaKeyboard();
            }
            else if(input == ButtonAction.GO_UP){
                changeSelectedMap(true);
            }
            else if(input == ButtonAction.GO_DOWN){
                changeSelectedMap(false);
            }
        } else if (gameStatus == GameStatus.RUNNING) {
            
            Mario mario = mapManager.getMario();
            if (input == ButtonAction.JUMP) {
                mario.jump(this);
                mapManager.clearColliderLists();
            } else if (input == ButtonAction.JUMP_RELEASED) {
                mario.setKeypress_jump(false);
                mario.resetGravity();
            } else if (input == ButtonAction.M_RIGHT) {
                mario.setKeypress_moveRight(true);
                //mario.move(true, camera);
                mapManager.clearColliderLists();
            } else if (input == ButtonAction.M_LEFT) {
                mario.setKeypress_moveLeft(true);
                //mario.move(false, camera);
                mapManager.clearColliderLists();
            } else if (input == ButtonAction.RIGHT_RELEASED) {
                mario.setKeypress_moveRight(false);
                //mario.setVelX(0);
            } else if (input == ButtonAction.LEFT_RELEASED) {
                mario.setKeypress_moveLeft(false);
                //mario.setVelX(0);
            } else if (input == ButtonAction.FIRE) {
                mapManager.fire(this);
            } else if (input == ButtonAction.PAUSE_RESUME) {
                pauseGame();
            }

        } else if (gameStatus == GameStatus.PAUSED) {
            if (input == ButtonAction.PAUSE_RESUME) {
                pauseGame();
            }
        } else if(gameStatus == GameStatus.GAME_OVER && input == ButtonAction.GO_TO_START_SCREEN){
            reset();
        } else if(gameStatus == GameStatus.MISSION_PASSED && input == ButtonAction.GO_TO_START_SCREEN){
            reset();
        }

        if(input == ButtonAction.GO_TO_START_SCREEN){
            if (isSoarControlled()) {
                setAgentTextBoxVisible(false);
            }
            setGameStatus(GameStatus.START_SCREEN);
        }
    }

    private void selectOption(boolean selectUp) {
        startScreenSelection = startScreenSelection.select(selectUp);
    }

    private void startGame() {
        if (gameStatus != GameStatus.GAME_OVER) {
            setGameStatus(GameStatus.MAP_SELECTION);
        }
    }

    private void pauseGame() {
        if (gameStatus == GameStatus.RUNNING) {
            setGameStatus(GameStatus.PAUSED);
            soundManager.pauseBackground();
            setEnemiesAnimating(false);
        } else if (gameStatus == GameStatus.PAUSED) {
            setGameStatus(GameStatus.RUNNING);
            soundManager.resumeBackground();
            setEnemiesAnimating(true);
        }
    }

    public void shakeCamera() {
        camera.shakeCamera();
    }

    public boolean isCameraShaking() {
        return camera.isShaking();
    }

    private boolean isGameOver() {
        if(gameStatus == GameStatus.RUNNING || gameStatus == GameStatus.MARIO_DEAD)
            return mapManager.isGameOver();
        return false;
    }

    public ImageLoader getImageLoader() {
        return imageLoader;
    }

    public SoundManager getSoundManager() {
        return soundManager;
    }

    public GameStatus getGameStatus() {
        return gameStatus;
    }

    public StartScreenSelection getStartScreenSelection() {
        return startScreenSelection;
    }

    public void setGameStatus(GameStatus gameStatus) {
        this.gameStatus = gameStatus;
    }

    public int getScore() {
        return mapManager.getScore();
    }

    public int getRemainingLives() {
        return mapManager.getRemainingLives();
    }

    public int getCoins() {
        return mapManager.getCoins();
    }

    public int getSelectedMap() {
        return selectedMap;
    }

    public void drawMap(Graphics2D g2) {
        mapManager.drawMap(g2);

        // DEBUG
        /*ArrayList<Enemy> enemies = mapManager.getMap().getEnemies();
        ArrayList<Brick> bricks = mapManager.getMap().getAllBricks();
        for (Enemy enemy : enemies) {
            for (Brick brick : bricks) {
                Rectangle enemyBounds = enemy.getBounds();
                enemyBounds = new Rectangle(enemyBounds.x+2,enemyBounds.y+2, enemyBounds.width-4, enemyBounds.width-4);
                Rectangle brickBounds = brick.getBounds();

                if (enemyBounds.intersects(brickBounds)) {
                    g2.drawString("BOUNCE", (int)enemy.getX(), (int)enemy.getY()-16);
                }
            }
        }
        //if (soarControlled) {
            mapManager.drawCollidersList(g2, 16,16);
        //}*/
    }

    public Point getCameraLocation() {
        return new Point((int)camera.getX(), (int)camera.getY());
    }

    public void killMario() {
        if (mapManager.isMarioDead())
            return;
        
        playMarioDies();
        setGameStatus(GameStatus.MARIO_DEAD);
        mapManager.setMarioDead(true);
        setEnemiesAnimating(false);

        setTimer(new EventTimer(getFutureTickFromMillisec(3000), new RingEventInterface() {
            @Override
            public void ring(GameEngine engine) {
                // Restart or game over
                if (isGameOver()) {
                    engine.triggerGameOver();
                }
                else {
                    engine.setGameStatus(GameStatus.RUNNING);
                    engine.getMapManager().resetCurrentMap(engine);
                }
                
            }
        }));

        Mario mario = mapManager.getMario();
        mario.setJumping(true);
        mario.resetGravity();
        mario.setVelYAbs(10);
        mario.setVelX(0);
    }

    public void triggerGameOver() {
        setGameStatus(GameStatus.GAME_OVER);
        playGameOver();
        setTimer(new EventTimer(getFutureTickFromMillisec(4000), new RingEventInterface() {
            @Override
            public void ring(GameEngine engine) {
                // Return to main menu after game over music/screen
                engine.reset();
            }
        }));
    }

    private int passMission() {
        return mapManager.passMission();
    }

    public void playCoin() {
        soundManager.playCoin();
    }

    public void playOneUp() {
        soundManager.playOneUp();
    }

    public void playSuperMushroom() {
        soundManager.playSuperMushroom();
    }

    public void playMarioDies() {
        soundManager.playMarioDies();
    }

    public void playGameOver() {
        soundManager.playGameOver();
    }

    public void playJump() {
        soundManager.playJump();
    }

    public void playFireFlower() {
        soundManager.playFireFlower();
    }

    public void playFireball() {
        soundManager.playFireball();
    }

    public void playStomp() {
        soundManager.playStomp();
    }

    public MapManager getMapManager() {
        return mapManager;
    }

    public boolean isSoarControlled() {
        return soarControlled;
    }

    public void setAgentMessage(String s) {
        jtext_agentOutput.setText("AGENT: "+s);
    }

    public void setAgentTextBoxVisible(boolean show) {
        jtext_agentOutput.setVisible(show);
        // This update caused the text box to steal focus, so request focus back to the frame so that the key listener works
        frame.requestFocus();
    }

    public static void main(String... args) {
        if (args.length > 0) {
            // If there is at least one arg, assume it's the path to a Soar agent
            String agentPath = args[0];
            boolean openDebugger = false;
            // If there is more than one arg, check for commands
            if (args.length > 1) {
                if (args[1].equals("--open-debugger")) {
                    openDebugger = true;
                }
            }
            new GameEngine(agentPath, openDebugger);
        }
        else {
            // If there are no args, run with human control
            new GameEngine(null, false);
        }
        
    }

    public int getRemainingTime() {
        return mapManager.getRemainingTime();
    }

    public void setEnemiesAnimating(boolean animate) {
        mapManager.setEnemiesAnimating(animate);
    }
}
