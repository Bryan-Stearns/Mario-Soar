package model.hero;

import manager.GameEngine;
import manager.GameStatus;
import view.Animation;
import model.GameObject;
import view.ImageLoader;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Mario extends GameObject{

    private int remainingLives;
    private int coins;
    private int points;
    //private double invincibilityTimer;
    private MarioForm marioForm;
    private boolean toRight = true;

    private boolean keyPressed_moveLeft = false,
                    keyPressed_moveRight = false,
                    keyPressed_jump = false;
    private double jumpStartY;
    private boolean invisible = false;

    public Mario(double x, double y) {
        super(x, y, null);
        setDimension(48,48);

        remainingLives = 3;
        points = 0;
        coins = 0;
        //invincibilityTimer = 0;
        jumpStartY = y;

        ImageLoader imageLoader = new ImageLoader();
        BufferedImage[] leftFrames = imageLoader.getLeftFrames(MarioForm.SMALL);
        BufferedImage[] rightFrames = imageLoader.getRightFrames(MarioForm.SMALL);

        Animation animation = new Animation(leftFrames, rightFrames);
        marioForm = new MarioForm(animation, false, false);
        setStyle(marioForm.getCurrentStyle(toRight, false, false));
    }

    @Override
    public void draw(Graphics g) {
        if (invisible)
            return;

        boolean movingInX = (getVelX() != 0);
        boolean movingInY = (getVelYAbs() != 0);

        setStyle(marioForm.getCurrentStyle(toRight, movingInX, movingInY));

        super.draw(g);
    }

    @Override
    public void updateLocation() {
        if (jumping && velY <= 0) {
            jumping = false;
            falling = true;
        }
        else if (jumping) {
            // Automatically restore gravity if Mario jumps a certain distance
            if (jumpStartY-y > 96.0) {
                resetGravity();
            }

            velY = velY - gravityAcc;
            y = y - velY;
        }

        if (falling) {
            y = y + velY;
            velY = velY + gravityAcc;
        }

        if (keyPressed_moveLeft) {
            if (!keyPressed_moveRight) {
                // Move left
                velX = -5;
                toRight = false;
            }
            else {
                // Left and Right cancel out
                velX = 0;
            }
        }
        else if (keyPressed_moveRight) {
            // Move right
            velX = 5;
            toRight = true;
        }
        else {
            // Standing still
            velX = 0;
        }

        x += velX;

        if (x < 0)
            x = 0;
    }

    /*@Override
    public void resetGravity() {
        gravityAcc = 0.1;
    }*/

    public void setKeypress_moveLeft(boolean move) {
        keyPressed_moveLeft = move;
    }

    public void setKeypress_moveRight(boolean move) {
        keyPressed_moveRight = move;
    }

    public void setKeypress_jump(boolean jump) {
        keyPressed_jump = jump;
    }

    public boolean getKeypress_jump() {
        return keyPressed_jump;
    }

    public void jump(GameEngine engine) {
        if(!isJumping() && !isFalling()){
            setJumping(true);
            setGravityAcc(0.0);
            setVelYAbs(6);   // Without the mechanic of holding down Jump to remove gravity, a good value is 10.5
            jumpStartY = getY();
            engine.playJump();
        }
    }

    /*public void move(boolean toRight, Camera camera) {
        if (toRight) {
            setVelX(5);
        }
        else if(camera.getX() < getX()) {
            setVelX(-5);
        }
        else {
            setVelX(0);
        }

        this.toRight = toRight;
    }*/

    public double getRelativeX(double xOther) {
        return (xOther - getX());
    }

    public double getRelativeY(double yOther) {
        return (yOther - getY());
    }

    public boolean onTouchEnemy(GameEngine engine) {

        if (engine.getGameStatus() == GameStatus.RUNNING && !engine.isCameraShaking()) {
            if (!marioForm.isSuper() && !marioForm.isFire()) {
                // Lose a life and reset
                remainingLives--;
                engine.killMario();
                return true;
            }
            else {
                // Shrink
                int startHeight = getBounds().height;
                engine.shakeCamera();
                marioForm = marioForm.onTouchEnemy(engine.getImageLoader());
                setDimension(48, 48);
                setY(getY()+(startHeight-48));
                return false;
            }
        }
        return false;
    }

    public Fireball fire(){
        return marioForm.fire(toRight, getX(), getY());
    }

    public void acquireCoin() {
        coins++;
    }

    public void acquirePoints(int point){
        points = points + point;
    }

    public int getRemainingLives() {
        return remainingLives;
    }

    public void setRemainingLives(int remainingLives) {
        this.remainingLives = remainingLives;
    }

    public int getPoints() {
        return points;
    }

    public int getCoins() {
        return coins;
    }

    public MarioForm getMarioForm() {
        return marioForm;
    }

    public void setMarioForm(MarioForm marioForm) {
        this.marioForm = marioForm;
    }

    public boolean isSuper() {
        return marioForm.isSuper();
    }

    public boolean getToRight() {
        return toRight;
    }

    public void setIsVisible(boolean visible) {
        this.invisible = !visible;
    }

    public void resetLocation() {
        setVelX(0);
        setVelYAbs(0);
        setX(50);
        setJumping(false);
        setFalling(true);
        setIsVisible(true);
    }
}
