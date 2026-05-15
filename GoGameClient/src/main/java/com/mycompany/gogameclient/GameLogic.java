/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.gogameclient;

/**
 *
 * @author abdel
 */
public class GameLogic {

    public static final int SIZE = 9;

    public static final int EMPTY = 0;
    public static final int BLACK = 1;
    public static final int WHITE = 2;

    int[][] board = new int[SIZE][SIZE];
    boolean[][] visited = new boolean[SIZE][SIZE];

    int[] dr = { -1, 1, 0, 0 };
    int[] dc = { 0, 0, -1, 1 };

    // Default constructor
    public GameLogic() {
        // board is already initialized to EMPTY (0)
    }

    // Check if inside board
    boolean isInside(int r, int c) {
        return r >= 0 && r < SIZE && c >= 0 && c < SIZE;
    }

    // Reset visited array
    void resetVisited() {
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                visited[i][j] = false;
            }
        }
    }

    // DFS Liberty Check
    boolean hasLiberty(int r, int c, int color) {
        visited[r][c] = true;

        for (int i = 0; i < 4; i++) {
            int nr = r + dr[i];
            int nc = c + dc[i];

            if (!isInside(nr, nc)) {
                continue;
            }

            // liberty found
            if (board[nr][nc] == EMPTY) {
                return true;
            }

            // continue DFS
            if (board[nr][nc] == color && !visited[nr][nc]) {
                if (hasLiberty(nr, nc, color)) {
                    return true;
                }
            }
        }
        return false;
    }

    // Remove connected group
    void removeGroup(int r, int c, int color) {
        board[r][c] = EMPTY;

        for (int i = 0; i < 4; i++) {
            int nr = r + dr[i];
            int nc = c + dc[i];

            if (isInside(nr, nc) && board[nr][nc] == color) {
                removeGroup(nr, nc, color);
            }
        }
    }

    // Check captures around placed stone
    void checkCaptures(int r, int c, int player) {
        int enemy;

        if (player == BLACK) {
            enemy = WHITE;
        } else {
            enemy = BLACK;
        }

        for (int i = 0; i < 4; i++) {
            int nr = r + dr[i];
            int nc = c + dc[i];

            if (!isInside(nr, nc)) {
                continue;
            }

            if (board[nr][nc] == enemy) {
                resetVisited();

                if (!hasLiberty(nr, nc, enemy)) {
                    removeGroup(nr, nc, enemy);
                }
            }
        }
    }

    // Place Stone
    public boolean placeStone(int r, int c, int player) {
        // occupied
        if (board[r][c] != EMPTY) {
            return false;
        }

        // place stone
        board[r][c] = player;

        // capture enemies
        checkCaptures(r, c, player);

        // suicide check
        resetVisited();

        if (!hasLiberty(r, c, player)) {
            board[r][c] = EMPTY;
            return false;
        }

        return true;
    }

    // Constructor with size parameter
    // (alias for default — SIZE is fixed at 9)
    public GameLogic(int size) {
        // SIZE is a compile-time constant; this constructor
        // exists for compatibility with GameScreen
    }

    // Board accessor
    public int[][] getBoard() {
        return board;
    }

    // Reset the board for a new match
    public void resetBoard() {
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                board[i][j] = EMPTY;
            }
        }
    }

    // Serialize board to 81-char string
    // (row-major order, chars '0','1','2')
    public String serializeBoard() {
        StringBuilder sb = new StringBuilder(SIZE * SIZE);
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                sb.append(board[i][j]);
            }
        }
        return sb.toString();
    }

    // Deserialize 81-char string into board
    public void deserializeBoard(String data) {
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                board[i][j] = data.charAt(i * SIZE + j) - '0';
            }
        }
    }

    // Territory scoring (Chinese-style)
    // Stones on board + surrounded empty area
    public int countTerritory(int color) {
        boolean[][] counted = new boolean[SIZE][SIZE];
        int score = 0;

        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                // count stones of this color
                if (board[i][j] == color) {
                    score++;
                }
                // flood-fill empty regions
                if (board[i][j] == EMPTY && !counted[i][j]) {
                    java.util.List<int[]> region = new java.util.ArrayList<>();
                    java.util.Set<Integer> borders = new java.util.HashSet<>();
                    floodFillTerritory(i, j, counted, region, borders);

                    // if region is bordered only by this color, count it
                    if (borders.size() == 1 && borders.contains(color)) {
                        score += region.size();
                    }
                }
            }
        }
        return score;
    }

    // Flood-fill helper for territory counting
    private void floodFillTerritory(int r, int c, boolean[][] counted,
            java.util.List<int[]> region, java.util.Set<Integer> borders) {

        counted[r][c] = true;
        region.add(new int[] { r, c });

        for (int i = 0; i < 4; i++) {
            int nr = r + dr[i];
            int nc = c + dc[i];

            if (!isInside(nr, nc)) {
                continue;
            }
            if (board[nr][nc] != EMPTY) {
                borders.add(board[nr][nc]);
            } else if (!counted[nr][nc]) {
                floodFillTerritory(nr, nc, counted, region, borders);
            }
        }
    }

    // Print board
    public void printBoard() {
        System.out.println();
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (board[i][j] == EMPTY) {
                    System.out.print(". ");
                } else if (board[i][j] == BLACK) {
                    System.out.print("B ");
                } else {
                    System.out.print("W ");
                }
            }
            System.out.println();
        }
        System.out.println();
    }
}
