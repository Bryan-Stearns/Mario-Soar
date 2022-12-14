package manager;

public class Camera {

    private double x, y;
    private int width, height;
    private int frameNumber;
    private boolean shaking;

    public Camera(int width, int height){
        this.x = 0;
        this.y = 0;
        this.width = width;
        this.height = height;
        this.frameNumber = 25;
        this.shaking = false;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void shakeCamera() {
        shaking = true;
        frameNumber = 60;
    }

    public boolean isShaking() {
        return shaking;
    }

    public void setLocation(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void moveCam(double xAmount, double yAmount){
        if(shaking && frameNumber > 0){
            int direction = (frameNumber%2 == 0)? 1 : -1;
            x = x + 4 * direction;
            frameNumber--;
        }
        else {
            x = x + xAmount;
            y = y + yAmount;
        }

        if (frameNumber <= 0)
            shaking = false;
    }
}
