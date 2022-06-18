package model.enemy;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Goomba extends Enemy{

    private BufferedImage rightImage;
    private boolean animateRightImage = false;

    public Goomba(double x, double y, BufferedImage style) {
        super(x, y, style);
        setVelX(3);
    }

    @Override
    public void draw(Graphics g) {
        if (checkAndDecrementAnimationTimer()) {
            animateRightImage = !animateRightImage;
            setAnimationTimer((short)60);
        }
        if (animateRightImage) { //(getVelX() > 0){
            g.drawImage(rightImage, (int)getX(), (int)getY(), null);
        }
        else
            super.draw(g);
    }

    public void setRightImage(BufferedImage rightImage) {
        this.rightImage = rightImage;
    }
}
