package model.enemy;

import model.GameObject;

import java.awt.image.BufferedImage;


public abstract class Enemy extends GameObject{

    protected boolean revealed = false;
    private boolean animate = true;
    protected short animationTimer = 60;

    public Enemy(double x, double y, BufferedImage style) {
        super(x, y, style);
        setFalling(false);
        setJumping(false);
    }

    public void reveal() {
        revealed = true;
    }

    public boolean isRevealed() {
        return revealed;
    }

    protected void setAnimationTimer(short ticks) {
        animationTimer = ticks;
    }

    protected boolean checkAndDecrementAnimationTimer() {
        if (!animate) {
            return false;
        }
        return (--animationTimer == 0);
    }

    public void setAnimating(boolean animate) {
        this.animate = animate;
    }

}
