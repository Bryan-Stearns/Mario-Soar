package manager;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;


public class InputManager implements KeyListener, MouseListener {

    private GameEngine engine;

    InputManager(GameEngine engine) {
        this.engine = engine; }

    @Override
    public void keyPressed(KeyEvent event) {
        int keyCode = event.getKeyCode();
        GameStatus status = engine.getGameStatus();
        ButtonAction currentAction = ButtonAction.NO_ACTION;

        if (status != GameStatus.RUNNING && status != GameStatus.PAUSED) {
            if (keyCode == KeyEvent.VK_UP) {
                currentAction = ButtonAction.GO_UP;
            }
            else if(keyCode == KeyEvent.VK_DOWN) {
                currentAction = ButtonAction.GO_DOWN;
            }
            else if (keyCode == KeyEvent.VK_ENTER) {
                currentAction = ButtonAction.SELECT;
            }
            else if (keyCode == KeyEvent.VK_ESCAPE) {
                currentAction = ButtonAction.GO_TO_START_SCREEN;
            }
        }
        else if (status == GameStatus.RUNNING) {
            if (!engine.isSoarControlled()) {
                if (keyCode == KeyEvent.VK_UP) {
                    currentAction = ButtonAction.JUMP;
                }
                else if (keyCode == KeyEvent.VK_RIGHT) {
                    currentAction = ButtonAction.M_RIGHT;
                }
                else if (keyCode == KeyEvent.VK_LEFT) {
                    currentAction = ButtonAction.M_LEFT;
                }
                else if (keyCode == KeyEvent.VK_SPACE){
                    currentAction = ButtonAction.FIRE;
                }
            }
            
            if (keyCode == KeyEvent.VK_ESCAPE) {
                currentAction = ButtonAction.PAUSE_RESUME;
            }
        }
        else if (status == GameStatus.PAUSED) {
            if (keyCode == KeyEvent.VK_ESCAPE) {
                currentAction = ButtonAction.PAUSE_RESUME;
            }
        }

        notifyInput(currentAction);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (engine.getGameStatus() == GameStatus.MAP_SELECTION) {
            engine.selectMapViaMouse();
        }
    }

    @Override
    public void keyReleased(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.VK_LEFT)
            notifyInput(ButtonAction.LEFT_RELEASED);
        else if (event.getKeyCode() == KeyEvent.VK_RIGHT)
            notifyInput(ButtonAction.RIGHT_RELEASED);
        else if (event.getKeyCode() == KeyEvent.VK_UP)
            notifyInput(ButtonAction.JUMP_RELEASED);
    }

    private void notifyInput(ButtonAction action) {
        if (action != ButtonAction.NO_ACTION)
            engine.receiveInput(action);
    }

    @Override
    public void keyTyped(KeyEvent arg0) {}

    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mouseReleased(MouseEvent e) {}

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {}
}
