package model.brick;

import manager.GameEngine;
import model.GameObject;
import model.prize.Prize;

import java.awt.image.BufferedImage;

public abstract class Brick extends GameObject{

    private boolean breakable;

    private boolean empty;

    private String type;

    public Brick(double x, double y, BufferedImage style){
        super(x, y, style);
        setDimension(48, 48);
    }

    public boolean isBreakable() {
        return breakable;
    }

    public void setBreakable(boolean breakable) {
        this.breakable = breakable;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return this.type;
    }

    public boolean isEmpty() {
        return empty;
    }

    public void setEmpty(boolean empty) {
        this.empty = empty;
    }

    public Prize reveal(GameEngine engine) { return null;}

    public Prize getPrize() {
        return null;
    }
}
