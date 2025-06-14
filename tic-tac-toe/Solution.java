public class Game {
    private final Player player1;
    private final Player player2;
    private final Board board;
    private final WinCondition winCondition;
    private final DrawCondition drawCondition;

    private Player currentPlayer;

    public Game(String name1, String name2, int boardSize) {
        this.player1 = new Player(name1, PlayerType.CIRCLE);
        this.player2 = new Player(name2, PlayerType.CROSS);
        this.board = new Board(size);
        this.winCondition = WinCondition.getDefault(board);
        this.drawCondition = DrawCondition.getDefault(board);

        this.currentPlayer = null;
    }

    void start() {
        this.currentPlayer = player1;
    }

    void pickCell(int row, int col) {
        if (currentPlayer == null) {
            throw new IllegalStateException();
        }

        board.pickCell(currentPlayer, row, col);

        if (winCondition.test(currentPlayer)) {
            // Show winner banner
            end();
        }

        this.currentPlayer = currentPlayer == player1 ? player2 : player1;
    }

    void end() {
        this.currentPlayer = null;
    }
}

public class Board {
    private final int size;
    private final Cell[][] cells;

    public Board(int size) {
        this.size = size;
        this.cells = new Cell[size][size];
        init();
    }

    private void init() {
        for (var i = 0; i < size; ++i) {
            for (var j = 0; j < size; ++j) {
                cells[i][j] = new Cell();
            }
        }
    }

    public void pickCell(Player player, int row, int col) {
        if (invalid(row, col)) {
            throw new IllegalAccessError();
        }
        cells[row][col].setState(fromPlayerType(player.getType()));
    }

    private boolean invalid() {
        return row < 0 || col < 0 || size <= row || size <= col ||
            cells[row][col].getState() != State.EMPTY;
    }
}

@Value
class Cell {
    private State state;
    public Cell() {
        this.state = State.EMPTY;
    }
}

public enum State {
    EMPTY, CIRCLE, CROSS;
}

public class Player {
    private final PlayerType type;
    public Player(String name, PlayerType playerType)
}

public enum PlayerType {
    CIRCLE, CROSS
}

public class WinCondition {
    private final int numInLine;
    private final Board board;

    private WinCondition(Board board) {
        this.numInLine = 3;
        this.board = board;
    }

    public static getDefault(Board board) {
        return new WinCondition(board);
    }

    public boolean test(Player currentPlayer) {
        final var state = fromPlayerType(currentPlayer.getType());
        final var boardSize = board.getSize();
        final var cells = board.getCells();

        if (horizontal(state) || vertical(state) || diagonal(state)) {
            return true;
        }


        return false;
    }

    private boolean horizontal(State state) {
        var horizontal = 0;
        for (var i = 0; i < boardSize; ++i) {
            for (var j = 0; j < boardSize; ++j) {
                if (state != cells[i][j]) {
                    horizontal = 0;
                }
                if (++horizontal == numInLine) {
                    return true;
                }
            }
            horizontal = 0;
        }
        return false;
    }

    private boolean vertical(State state) {
        var vertical = 0;
        for (var i = 0; i < boardSize; ++i) {
            for (var j = 0; j < boardSize; ++j) {
                if (state != cells[j][i]) {
                    vertical = 0;
                }
                if (++vertical == numInLine) {
                    return true;
                }
            }
            vertical = 0;
        }
        return false;
    }

    private boolean diagonal(State state) {
        for (var k = 1; k < boardSize; ++k) {
            if (checkDiagonalStarting(0, k) || checkDiagonalStarting(k, 0)) {
                return true;
            }
        }
        return checkDiagonalStarting(0, 0);
    }

    private boolean checkDiagonalStarting(int row, int col) {
        var diagonal = 0;
        for (int i = row, j = col; i < boardSize && j < boardSize; ++i, ++j) {
            if (state != cells[i][j]) {
                diagonal = 0;
            }
            if (++diagonal == numInLine) {
                return true;
            }
        }
        return false;
    }
}

public class DrawCondition {}
