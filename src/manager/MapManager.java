package manager;

import model.GameObject;
import model.Map;
import model.brick.Brick;
import model.brick.OrdinaryBrick;
import model.enemy.Enemy;
import model.hero.Fireball;
import model.hero.Mario;
import model.prize.BoostItem;
import model.prize.Coin;
import model.prize.Prize;
import view.ImageLoader;

import java.awt.*;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;

public class MapManager {

    private Map map;
    private Camera camera;
    private ArrayList<GameObject> marioCollidersTop,
                                marioCollidersBottom,
                                marioCollidersLeft,
                                marioCollidersRight;

    public MapManager(Camera camera) {
        this.camera = camera;
        marioCollidersTop = new ArrayList<GameObject>();
        marioCollidersBottom = new ArrayList<GameObject>();
        marioCollidersLeft = new ArrayList<GameObject>();
        marioCollidersRight = new ArrayList<GameObject>();
    }

    public void clearColliderLists() {
        marioCollidersTop.clear();
        marioCollidersBottom.clear();
        marioCollidersLeft.clear();
        marioCollidersRight.clear();
    }

    /*public void clearColliderTop() {
        marioCollidersTop.clear();
    }

    public void clearColliderBottom() {
        marioCollidersBottom.clear();
    }

    public void clearColliderLeft() {
        marioCollidersLeft.clear();
    }

    public void clearColliderRight() {
        marioCollidersRight.clear();
    }*/

    public void addTopCollider(GameObject obj) {
        if (!marioCollidersTop.contains(obj))
            marioCollidersTop.add(obj);
    }

    public void addBottomCollider(GameObject obj) {
        if (!marioCollidersBottom.contains(obj))
            marioCollidersBottom.add(obj);
    }

    public void addRightCollider(GameObject obj) {
        if (!marioCollidersRight.contains(obj))
            marioCollidersRight.add(obj);
    }

    public void addLeftCollider(GameObject obj) {
        if (!marioCollidersLeft.contains(obj))
            marioCollidersLeft.add(obj);
    }

    public void updateLocations() {
        if (map == null)
            return;

        map.updateLocations();
    }

    public void updateSoarInput(MarioSoarLink soarLink) {
        if (soarLink == null)
            return;

        // Clear old input
        soarLink.cleanInputWMEs();

        // Update new input
        soarLink.addInput_mario(map.getMario(), map.getRemainingTime());
        soarLink.addInput_touching(marioCollidersTop, marioCollidersBottom, marioCollidersLeft, marioCollidersRight);
        soarLink.addInput_enemies(map.getEnemies());
        soarLink.addInput_bricks(map.getAllBricks());

        soarLink.getAgent().Commit();
    }

    public void resetCurrentMap(GameEngine engine) {
        Mario mario = getMario();
        mario.resetLocation();
        createMap(engine.getImageLoader(), map.getPath());
        map.setMario(mario);
        engine.resetCamera();
        engine.restartBackgroundMusic();
    }

    public boolean createMap(ImageLoader loader, String path) {
        MapCreator mapCreator = new MapCreator(loader, camera);
        map = mapCreator.createMap("/maps/" + path, 400);

        return map != null;
    }

    public void acquirePoints(int point) {
        map.getMario().acquirePoints(point);
    }

    public Mario getMario() {
        return map.getMario();
    }

    public void setMarioDead(boolean isDead) {
        map.setMarioDead(isDead);
    }

    public boolean isMarioDead() {
        return map.isMarioDead();
    }

    public void fire(GameEngine engine) {
        Fireball fireball = getMario().fire();
        if (fireball != null) {
            map.addFireball(fireball);
            engine.playFireball();
        }
    }

    public boolean isGameOver() {
        return getMario().getRemainingLives() == 0 || map.isTimeOver();
    }

    public int getScore() {
        return getMario().getPoints();
    }

    public int getRemainingLives() {
        return getMario().getRemainingLives();
    }

    public int getCoins() {
        return getMario().getCoins();
    }

    public Map getMap() {
        return map;
    }

    public void drawMap(Graphics2D g2) {
        map.drawMap(g2);
    }

    public int passMission() {
        if(getMario().getX() >= map.getEndPoint().getX() && !map.getEndPoint().isTouched()){
            map.getEndPoint().setTouched(true);
            int height = (int)getMario().getY();
            return height * 2;
        }
        else
            return -1;
    }

    public boolean endLevel(){
        return getMario().getX() >= map.getEndPoint().getX() + 320;
    }

    public void checkCollisions(GameEngine engine) {
        if (map == null) {
            return;
        }

        //clearColliderLists();

        //checkMarioBottomCollisions(engine);
        //checkMarioTopCollisions(engine);
        //checkMarioHorizontalCollision(engine);
        checkMarioCollisions(engine);
        checkEnemyCollisions();
        checkPrizeCollision();
        checkMarioPrizeContact(engine);
        checkFireballContact();
    }

    private void checkMarioCollisions(GameEngine engine) {
        Mario mario = getMario();
        ArrayList<Brick> bricks = map.getAllBricks();
        ArrayList<Enemy> enemies = map.getEnemies();
        ArrayList<GameObject> toBeRemoved = new ArrayList<>();
        Rectangle marioBounds = mario.getBounds();
        //int collisionBufferDist = 8;

        if (!mario.isJumping())
            mario.setFalling(true);
        
        // Check the floor
        if (mario.getY() + mario.getDimension().height >= map.getFloorY()) {
            mario.setY(map.getFloorY() - mario.getDimension().height);
            mario.setFalling(false);
            mario.setVelYAbs(0);
        }

        // Check other bricks
        for (Brick brick : bricks) {
            if (!map.isWithinCamera(brick))
                continue;
                
            // Check hitting from the left
            if (mario.getRightBounds().intersects(brick.getLeftBounds())) {
                mario.setVelX(0);
                mario.setX(brick.getX() - mario.getDimension().width);
                addRightCollider(brick);
            }
            // Check hitting from the right
            if (mario.getLeftBounds().intersects(brick.getRightBounds())) {
                mario.setVelX(0);
                mario.setX(brick.getX() + brick.getDimension().width);
                addLeftCollider(brick);
            }
            // Check hitting from above
            else if (mario.isFalling() && mario.getBottomBounds().intersects(brick.getTopBounds())) {
                mario.setY(brick.getY() - mario.getDimension().height + 1);
                mario.setFalling(false);
                mario.setVelYAbs(0);
                addBottomCollider(brick);
            }
            // Check hitting from below
            else if (mario.isJumping() && mario.getTopBounds().intersects(brick.getBottomBounds())) {
                mario.setVelYAbs(0);
                mario.resetGravity();
                mario.setY(brick.getY() + brick.getDimension().height);
                Prize prize = brick.reveal(engine);
                if (prize != null) {
                    map.addRevealedPrize(prize);
                    if (prize instanceof Coin)
                        prize.onTouch(mario, engine);
                }
                addTopCollider(brick);
            }
            /*Rectangle brickBounds = brick.getBounds();
            if (marioBounds.intersects(brickBounds)) {
                // Check hitting from above
                if (mario.isFalling() && mario.getY()+marioBounds.height < brick.getY()+collisionBufferDist) {
                    mario.setY(brick.getY() - marioBounds.height + 1);
                    mario.setFalling(false);
                    mario.setVelYAbs(0);
                }
                // Check hitting from underneath
                else if (mario.isJumping() && mario.getY() > brick.getY()+brickBounds.height-collisionBufferDist) {
                    mario.setVelYAbs(0);
                    mario.setY(brick.getY() + brickBounds.height - 1);

                    Prize prize = brick.reveal(engine);
                    if (prize != null)
                        map.addRevealedPrize(prize);
                }
                // Check hitting from left side
                else if (mario.getVelX() > 0 && mario.getX()+marioBounds.width < brick.getX()+collisionBufferDist) {
                    mario.setX(brick.getX()-marioBounds.width);
                    mario.setVelX(0);
                }
                // Check hitting from right side
                else if (mario.getVelX() < 0 && mario.getX() > brick.getX()+brickBounds.width-collisionBufferDist) {
                    mario.setX(brick.getX()+brickBounds.width);
                    mario.setVelX(0);
                }
            }*/
        }

        // Check hitting enemies
        boolean stompedEnemy = false;
        for(Enemy enemy : enemies) {
            if (!map.isWithinCamera(enemy))
                continue;
            
            Rectangle enemyBounds = enemy.getBounds();
            if (marioBounds.intersects(enemyBounds)) {
                if (mario.isFalling()) { // && mario.getVelYAbs() > 0) {
                    // Mario beats the enemy
                    stompedEnemy = true;
                    toBeRemoved.add(enemy);
                    mario.acquirePoints(100);
                    addBottomCollider(enemy);
                }
                else if (mario.getY()+marioBounds.height > enemy.getY()+8) {
                    // The enemy beats Mario
                    mario.onTouchEnemy(engine);

                    if (enemy.getY()+enemyBounds.height < mario.getY())
                        addTopCollider(enemy);
                    else if (enemy.getX()+enemyBounds.width < mario.getX())
                        addLeftCollider(enemy);
                    else
                        addRightCollider(enemy);
                }
            }
        }
        removeObjects(toBeRemoved);
        if (stompedEnemy) {
            engine.playStomp();
            mario.setVelYAbs(3);
            mario.setJumping(true);
            mario.setFalling(false);
        }
    }

    /*private void checkMarioBottomCollisions(GameEngine engine) {
        Mario mario = getMario();
        ArrayList<Brick> bricks = map.getAllBricks();
        ArrayList<Enemy> enemies = map.getEnemies();
        ArrayList<GameObject> toBeRemoved = new ArrayList<>();

        Rectangle marioBottomBounds = mario.getBottomBounds();

        if (!mario.isJumping())
            mario.setFalling(true);

        for (Brick brick : bricks) {
            Rectangle brickTopBounds = brick.getTopBounds();
            if (marioBottomBounds.intersects(brickTopBounds)) {
                mario.setY(brick.getY() - mario.getDimension().height + 1);
                mario.setFalling(false);
                mario.setVelYAbs(0);
            }
        }

        for (Enemy enemy : enemies) {
            Rectangle enemyTopBounds = enemy.getTopBounds();
            if (marioBottomBounds.intersects(enemyTopBounds)) {
                mario.acquirePoints(100);
                toBeRemoved.add(enemy);
                engine.playStomp();
            }
        }

        if (mario.getY() + mario.getDimension().height >= map.getFloorY()) {
            mario.setY(map.getFloorY() - mario.getDimension().height);
            mario.setFalling(false);
            mario.setVelYAbs(0);
        }

        removeObjects(toBeRemoved);
    }

    private void checkMarioTopCollisions(GameEngine engine) {
        Mario mario = getMario();
        ArrayList<Brick> bricks = map.getAllBricks();

        Rectangle marioTopBounds = mario.getTopBounds();
        for (Brick brick : bricks) {
            Rectangle brickBottomBounds = brick.getBottomBounds();
            if (marioTopBounds.intersects(brickBottomBounds)) {
                mario.setVelYAbs(0);
                mario.setY(brick.getY() + brick.getDimension().height);
                Prize prize = brick.reveal(engine);
                if(prize != null)
                    map.addRevealedPrize(prize);
            }
        }
    }

    private void checkMarioHorizontalCollision(GameEngine engine){
        Mario mario = getMario();
        ArrayList<Brick> bricks = map.getAllBricks();
        ArrayList<Enemy> enemies = map.getEnemies();
        //ArrayList<GameObject> toBeRemoved = new ArrayList<>();

        boolean marioDies = false;
        boolean toRight = mario.getToRight();

        Rectangle marioBounds = toRight ? mario.getRightBounds() : mario.getLeftBounds();

        for (Brick brick : bricks) {
            Rectangle brickBounds = !toRight ? brick.getRightBounds() : brick.getLeftBounds();
            if (marioBounds.intersects(brickBounds)) {
                mario.setVelX(0);
                if(toRight)
                    mario.setX(brick.getX() - mario.getDimension().width);
                else
                    mario.setX(brick.getX() + brick.getDimension().width);
            }
        }

        for(Enemy enemy : enemies){
            Rectangle enemyBounds = !toRight ? enemy.getRightBounds() : enemy.getLeftBounds();
            if (marioBounds.intersects(enemyBounds)) {
                marioDies = mario.onTouchEnemy(engine);
                //toBeRemoved.add(enemy);
            }
        }
        //removeObjects(toBeRemoved);


        //if (mario.getX() <= engine.getCameraLocation().getX() && mario.getVelX() < 0) {
        //    mario.setVelX(0);
        //    mario.setX(engine.getCameraLocation().getX());
        //}

        //if(marioDies) {
        //    engine.killMario();
        //    //resetCurrentMap(engine);
        //}
    }*/

    private void checkEnemyCollisions() {
        ArrayList<Brick> bricks = map.getAllBricks();
        ArrayList<Enemy> enemies = map.getEnemies();

        for (Enemy enemy : enemies) {
            boolean standsOnBrick = false;

            for (Brick brick : bricks) {
                Rectangle enemyBottomBounds = enemy.getBottomBounds();
                Rectangle brickTopBounds = brick.getTopBounds();

                Rectangle enemyBounds = enemy.getBounds();
                enemyBounds = new Rectangle(enemyBounds.x+2,enemyBounds.y+2, enemyBounds.width-4, enemyBounds.width-4);
                Rectangle brickBounds = brick.getBounds();

                /*Rectangle enemyBounds = enemy.getLeftBounds();
                Rectangle brickBounds = brick.getRightBounds();

                if (enemy.getVelX() > 0) {
                    enemyBounds = enemy.getRightBounds();
                    brickBounds = brick.getLeftBounds();
                }*/

                if (enemyBounds.intersects(brickBounds)) {
                    enemy.setVelX(-enemy.getVelX());
                }

                if (enemyBottomBounds.intersects(brickTopBounds)){
                    enemy.setFalling(false);
                    enemy.setVelYAbs(0);
                    enemy.setY(brick.getY()-enemy.getDimension().height);
                    standsOnBrick = true;
                }
            }

            if(enemy.getY() + enemy.getDimension().height > map.getFloorY()){
                enemy.setFalling(false);
                enemy.setVelYAbs(0);
                enemy.setY(map.getFloorY()-enemy.getDimension().height);
            }

            if (!standsOnBrick && enemy.getY() < map.getFloorY()){
                enemy.setFalling(true);
            }
        }
    }

    private void checkPrizeCollision() {
        ArrayList<Prize> prizes = map.getRevealedPrizes();
        ArrayList<Brick> bricks = map.getAllBricks();

        for (Prize prize : prizes) {
            if (prize instanceof BoostItem) {
                BoostItem boost = (BoostItem) prize;
                Rectangle prizeBottomBounds = boost.getBottomBounds();
                Rectangle prizeRightBounds = boost.getRightBounds();
                Rectangle prizeLeftBounds = boost.getLeftBounds();
                boost.setFalling(true);

                for (Brick brick : bricks) {
                    Rectangle brickBounds;

                    if (boost.isFalling()) {
                        brickBounds = brick.getTopBounds();

                        if (brickBounds.intersects(prizeBottomBounds)) {
                            boost.setFalling(false);
                            boost.setVelYAbs(0);
                            boost.setY(brick.getY() - boost.getDimension().height + 1);
                            if (boost.getVelX() == 0)
                                boost.setVelX(2);
                        }
                    }

                    if (boost.getVelX() > 0) {
                        brickBounds = brick.getLeftBounds();

                        if (brickBounds.intersects(prizeRightBounds)) {
                            boost.setVelX(-boost.getVelX());
                        }
                    } else if (boost.getVelX() < 0) {
                        brickBounds = brick.getRightBounds();

                        if (brickBounds.intersects(prizeLeftBounds)) {
                            boost.setVelX(-boost.getVelX());
                        }
                    }
                }

                if (boost.getY() + boost.getDimension().height > map.getFloorY()) {
                    boost.setFalling(false);
                    boost.setVelYAbs(0);
                    boost.setY(map.getFloorY() - boost.getDimension().height);
                    if (boost.getVelX() == 0)
                        boost.setVelX(2);
                }

            }
        }
    }

    private void checkMarioPrizeContact(GameEngine engine) {
        ArrayList<Prize> prizes = map.getRevealedPrizes();
        ArrayList<GameObject> toBeRemoved = new ArrayList<>();

        Rectangle marioBounds = getMario().getBounds();
        for (Prize prize : prizes) {
            Rectangle prizeBounds = prize.getBounds();
            if (prizeBounds.intersects(marioBounds)) {
                prize.onTouch(getMario(), engine);
                toBeRemoved.add((GameObject) prize);
            } /*else if(prize instanceof Coin){
                // There aren't yet stand-alone Coin objects that do not come from '?' boxes, but if there were they should be a different class than the '?' box coins
                prize.onTouch(getMario(), engine);
            }*/
        }

        removeObjects(toBeRemoved);
    }

    private void checkFireballContact() {
        ArrayList<Fireball> fireballs = map.getFireballs();
        ArrayList<Enemy> enemies = map.getEnemies();
        ArrayList<Brick> bricks = map.getAllBricks();
        ArrayList<GameObject> toBeRemoved = new ArrayList<>();

        for(Fireball fireball : fireballs){
            Rectangle fireballBounds = fireball.getBounds();

            for(Enemy enemy : enemies){
                Rectangle enemyBounds = enemy.getBounds();
                if (fireballBounds.intersects(enemyBounds)) {
                    acquirePoints(100);
                    toBeRemoved.add(enemy);
                    toBeRemoved.add(fireball);
                }
            }

            for(Brick brick : bricks){
                Rectangle brickBounds = brick.getBounds();
                if (fireballBounds.intersects(brickBounds)) {
                    toBeRemoved.add(fireball);
                }
            }
        }

        removeObjects(toBeRemoved);
    }

    private void removeObjects(ArrayList<GameObject> list){
        if(list == null)
            return;

        for(GameObject object : list){
            if(object instanceof Fireball){
                map.removeFireball((Fireball)object);
            }
            else if(object instanceof Enemy){
                map.removeEnemy((Enemy)object);
            }
            else if(object instanceof Coin || object instanceof BoostItem){
                map.removePrize((Prize)object);
            }
        }
    }

    public void addRevealedBrick(OrdinaryBrick ordinaryBrick) {
        map.addRevealedBrick(ordinaryBrick);
    }

    public void updateTime(){
        if(map != null)
            map.updateTime(1);
    }

    public int getRemainingTime() {
        return (int)map.getRemainingTime();
    }

    public void drawCollidersList(Graphics2D g2, int x, int y) {
        g2.setColor(Color.WHITE);
        int yy = y;
        int xShift = (int)camera.getX();

        try {
            g2.drawString("TOP:", x+xShift, yy);
            yy += 20;
            for (GameObject obj : marioCollidersTop) {
                g2.drawString(obj.toString(), x+xShift,yy);
                yy += 16;
            }
            xShift += 256;
            yy = y;

            g2.drawString("BOTTOM:", x+xShift, yy);
            yy += 20;
            for (GameObject obj : marioCollidersBottom) {
                g2.drawString(obj.toString(), x+xShift,yy);
                yy += 16;
            }
            xShift += 256;
            yy = y;

            g2.drawString("RIGHT:", x+xShift, yy);
            yy += 20;
            for (GameObject obj : marioCollidersRight) {
                g2.drawString(obj.toString(), x+xShift,yy);
                yy += 16;
            }
            xShift += 256;
            yy = y;

            g2.drawString("LEFT:", x+xShift, yy);
            yy += 20;
            for (GameObject obj : marioCollidersLeft) {
                g2.drawString(obj.toString(), x+xShift,yy);
                yy += 16;
            }
            yy = y;

        }
        catch (ConcurrentModificationException e) {
            e.printStackTrace();
        }
        
    }
}
