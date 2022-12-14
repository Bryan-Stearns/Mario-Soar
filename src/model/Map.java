package model;

import model.brick.Brick;
import model.brick.OrdinaryBrick;
import model.enemy.Enemy;
import model.hero.Fireball;
import model.hero.Mario;
import model.prize.BoostItem;
import model.prize.Coin;
import model.prize.Prize;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;

import manager.Camera;

public class Map {

    private double remainingTime;
    private Mario mario;
    private ArrayList<Brick> bricks = new ArrayList<Brick>();
    private ArrayList<Enemy> enemies = new ArrayList<Enemy>();
    private ArrayList<Brick> groundBricks = new ArrayList<Brick>();
    private ArrayList<Prize> revealedPrizes = new ArrayList<Prize>();
    private ArrayList<BoostItem> revealedBoosts = new ArrayList<BoostItem>();
    private ArrayList<Brick> revealedBricks = new ArrayList<Brick>();
    private ArrayList<Fireball> fireballs = new ArrayList<Fireball>();
    private EndFlag endPoint;
    private BufferedImage backgroundImage;
    private double bottomBorder = 720 - 96;
    private String path;
    private Camera camera;

    private boolean marioDead = false;


    public Map(double remainingTime, BufferedImage backgroundImage, Camera camera) {
        this.backgroundImage = backgroundImage;
        this.remainingTime = remainingTime;
        this.camera = camera;
    }


    public Mario getMario() {
        return mario;
    }

    public void setMario(Mario mario) {
        this.mario = mario;
    }

    public void setMarioDead(boolean isDead) {
        if (isDead) {
            mario.setKeypress_jump(false);
            mario.setKeypress_moveLeft(false);
            mario.setKeypress_moveRight(false);
        }
        this.marioDead = isDead;
    }

    public boolean isMarioDead() {
        return marioDead;
    }

    public boolean isWithinCamera(GameObject obj) {
        return (obj.getX()+obj.getDimension().width > camera.getX() && obj.getX() < camera.getX()+camera.getWidth()+obj.getDimension().width);
    }

    public ArrayList<Enemy> getEnemies() {
        return enemies;
    }

    /*public ArrayList<Enemy> getVisibleEnemies() {
        ArrayList<Enemy> retVal = new ArrayList<Enemy>();
        for (Enemy e : enemies) {
            if (e != null && isWithinCamera(e)) {
                retVal.add(e);
            }
        }
        return retVal;
    }*/

    public ArrayList<Fireball> getFireballs() {
        return fireballs;
    }

    public ArrayList<Prize> getRevealedPrizes() {
        return revealedPrizes;
    }

    public ArrayList<BoostItem> getRevealedBoosts() {
        return revealedBoosts;
    }

    public ArrayList<Brick> getAllBricks() {
        ArrayList<Brick> allBricks = new ArrayList<Brick>();

        allBricks.addAll(bricks);
        allBricks.addAll(groundBricks);

        return allBricks;
    }

    /*public ArrayList<Brick> getAllVisibleBricks(boolean includeGround) {
        ArrayList<Brick> visBricks = new ArrayList<>();

        for (Brick b : bricks) {
            // Treat a brick as visisble if it's within the horizontal scope of the camera
            if (b != null && isWithinCamera(b)) {
                visBricks.add(b);
            }
        }
        if (includeGround) {
            for (Brick b : groundBricks) {
                if (b != null && isWithinCamera(b)) {
                    visBricks.add(b);
                }
            }
        }

        return visBricks;
    }*/

    public void addBrick(Brick brick) {
        this.bricks.add(brick);
    }

    public void addGroundBrick(Brick brick) {
        this.groundBricks.add(brick);
    }

    public void addEnemy(Enemy enemy) {
        this.enemies.add(enemy);
    }

    public void drawMap(Graphics2D g2){
        drawBackground(g2);
        drawPrizes(g2);
        drawBricks(g2);
        drawEnemies(g2);
        drawFireballs(g2);
        drawMario(g2);
        endPoint.draw(g2);
    }

    private void drawFireballs(Graphics2D g2) {
        for(Fireball fireball : fireballs){
            fireball.draw(g2);
        }
    }

    private void drawPrizes(Graphics2D g2) {
        for(Prize prize : revealedPrizes) {
            if(prize instanceof Coin){
                ((Coin) prize).draw(g2);
            }
            else if(prize instanceof  BoostItem){
                ((BoostItem) prize).draw(g2);
            }
        }
    }

    private void drawBackground(Graphics2D g2){
        g2.drawImage(backgroundImage, 0, 0, null);
    }

    private void drawBricks(Graphics2D g2) {
        for (Brick brick : getAllBricks()) {
            if (brick != null && isWithinCamera(brick))
                brick.draw(g2);
        }

        /*for(Brick brick : bricks){
            if(brick != null)
                brick.draw(g2);
        }

        for(Brick brick : groundBricks){
            brick.draw(g2);
        }*/
    }

    private void drawEnemies(Graphics2D g2) {
        for (Enemy enemy : enemies) {
            if (enemy != null && enemy.isRevealed())
                enemy.draw(g2);
        }
    }

    private void drawMario(Graphics2D g2) {
        mario.draw(g2);
    }

    public void updateLocations() {
        mario.updateLocation();
        
        for (Enemy enemy : enemies) {
            if (!enemy.isRevealed()) {
                if (isWithinCamera(enemy)) {
                    enemy.reveal();
                }
            }
            else {
                enemy.updateLocation();
            }
        }

        for(Iterator<Prize> prizeIterator = revealedPrizes.iterator(); prizeIterator.hasNext();){
            Prize prize = prizeIterator.next();
            if(prize instanceof Coin){
                ((Coin) prize).updateLocation();
                if(((Coin) prize).getRevealBoundary() > ((Coin) prize).getY()){
                    prizeIterator.remove();
                }
            }
            else if (prize instanceof BoostItem){
                ((BoostItem) prize).updateLocation();
            }
        }

        for (Iterator<Fireball> fireballIterator=fireballs.iterator(); fireballIterator.hasNext(); ) {
            Fireball fireball = (Fireball)fireballIterator.next();
            fireball.updateLocation();
            if (!isWithinCamera(fireball)) {
                fireballIterator.remove();
            }
        }

        for(Iterator<Brick> brickIterator = revealedBricks.iterator(); brickIterator.hasNext();){
            OrdinaryBrick brick = (OrdinaryBrick)brickIterator.next();
            brick.animate();
            if (brick.getFrames() < 0) {
                bricks.remove(brick);
                brickIterator.remove();
            }
        }

        endPoint.updateLocation();
    }

    public double getFloorY() {
        return bottomBorder;
    }

    public void addRevealedPrize(Prize prize) {
        revealedPrizes.add(prize);
        if (prize instanceof BoostItem)
            revealedBoosts.add((BoostItem) prize);
    }

    public void addFireball(Fireball fireball) {
        fireballs.add(fireball);
    }

    public void setEndPoint(EndFlag endPoint) {
        this.endPoint = endPoint;
    }

    public EndFlag getEndPoint() {
        return endPoint;
    }

    public void addRevealedBrick(OrdinaryBrick ordinaryBrick) {
        revealedBricks.add(ordinaryBrick);
    }

    public void removeFireball(Fireball object) {
        fireballs.remove(object);
    }

    public void removeEnemy(Enemy object) {
        enemies.remove(object);
    }

    public void removePrize(Prize object) {
        revealedPrizes.remove(object);
        if (object instanceof BoostItem)
            revealedBoosts.remove(object);
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void updateTime(double passed){
        remainingTime = remainingTime - passed;
    }

    public boolean isTimeOver(){
        return remainingTime <= 0;
    }

    public double getRemainingTime() {
        return remainingTime;
    }
}
