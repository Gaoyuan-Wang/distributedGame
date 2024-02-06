package top.gaoyuanwang.distributedsystem.game;

/**
 * GameException
 * This Exception is used to handle the exception and record user input when the Game thread is crashed.
 */
public class GameException extends Exception {
    public String userOrder;
    public GameException(String input, Exception e) {
        super(e.getMessage(), e);
        this.userOrder = input;
    }
}