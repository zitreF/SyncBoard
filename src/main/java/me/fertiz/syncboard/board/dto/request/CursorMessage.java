package me.fertiz.syncboard.board.dto.request;

public record CursorMessage(Integer x, Integer y) implements ClientMessage {

    @Override
    public boolean isValid() {
        return isHidden() || isOnBoard();
    }

    private boolean isHidden() {
        return x == null && y == null;
    }

    private boolean isOnBoard() {
        return x != null && y != null && Coordinates.isAllowed(x) && Coordinates.isAllowed(y);
    }
}
