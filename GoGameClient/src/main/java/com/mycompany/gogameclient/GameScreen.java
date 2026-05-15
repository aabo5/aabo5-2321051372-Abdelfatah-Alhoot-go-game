/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.mycompany.gogameclient;

/**
 *
 * @author abdel
 */
public class GameScreen extends javax.swing.JFrame {
    private javax.swing.DefaultListModel<String> movesModel;

    private GameLogic game;
    private static final java.util.logging.Logger logger = java.util.logging.Logger
            .getLogger(GameScreen.class.getName());
    private int[][] board = new int[9][9];
    private int currentPlayer = 1;

    // Networking fields
    private NetworkClient networkClient;
    private String myColor; // "BLACK" or "WHITE"
    private volatile boolean isMyTurn = false;
    private int moveCount = 0;

    // These store our custom pictures for the mouse cursor
    private java.awt.Cursor cursorBlack = java.awt.Cursor.getDefaultCursor();
    private java.awt.Cursor cursorWhite = java.awt.Cursor.getDefaultCursor();
    private java.awt.Cursor cursorEmpty = java.awt.Cursor.getDefaultCursor();

    // Default constructor for NetBeans designer
    public GameScreen() {
        initComponents();
        game = new GameLogic(9);
        movesModel = new javax.swing.DefaultListModel<>();
        jList1_Moves.setModel(movesModel);
        FontUtil.setCustomFont(this);
        initCursors();
    }

    // Networked constructor called when the game starts
    public GameScreen(NetworkClient client, String color) {
        initComponents();
        game = new GameLogic(9);
        movesModel = new javax.swing.DefaultListModel<>();
        jList1_Moves.setModel(movesModel);
        FontUtil.setCustomFont(this);
        initCursors();

        this.networkClient = client;
        this.myColor = color;
        this.isMyTurn = "BLACK".equals(color); // BLACK always goes first

        // Update status labels
        jLabel_ConnectionStatus.setText("Connection : Connected");
        jLabel_CurrentTurn.setText("Current Turn : Black");
        updateTurnIndicator();

        // Register this screen as the network listener
        networkClient.setListener(new GameScreenListener());

        // Wire the PASS button
        jButton_RestartGame.addActionListener(e -> {
            if (networkClient != null) {
                networkClient.sendPass();
            }
        });
    }

    // Update the turn indicator and mouse cursor
    private void updateTurnIndicator() {
        if (isMyTurn) {
            jLabel2.setText("> Your Turn (" + myColor + ")");

            // If it is my turn, change the mouse cursor to my stone color
            if ("BLACK".equals(myColor)) {
                jPanel_Board.setCursor(cursorBlack);
            } else {
                jPanel_Board.setCursor(cursorWhite);
            }
        } else {
            jLabel2.setText("  Opponent's Turn");
            // When it's the opponent's turn, make my hand empty
            jPanel_Board.setCursor(cursorEmpty);
        }
    }

    private void initCursors() {
        try {
            // Toolkit helps us make custom cursors
            java.awt.Toolkit toolkit = java.awt.Toolkit.getDefaultToolkit();

            // We want the cursor to click exactly in the middle of the 32x32 image (16, 16)
            java.awt.Point middle = new java.awt.Point(16, 16);

            // 1. Load the Black Hand picture
            java.io.File blackFile = new java.io.File("resources/images/hand_black.png");
            if (blackFile.exists()) {
                // Read the image
                java.awt.Image blackImg = javax.imageio.ImageIO.read(blackFile);
                // Make it smaller (32x32 pixels) so it looks good as a cursor
                java.awt.Image smallBlack = blackImg.getScaledInstance(32, 32, java.awt.Image.SCALE_SMOOTH);
                // Save it to our variable
                cursorBlack = toolkit.createCustomCursor(smallBlack, middle, "black");
            }

            // 2. Load the White Hand picture
            java.io.File whiteFile = new java.io.File("resources/images/hand_white.png");
            if (whiteFile.exists()) {
                java.awt.Image whiteImg = javax.imageio.ImageIO.read(whiteFile);
                java.awt.Image smallWhite = whiteImg.getScaledInstance(32, 32, java.awt.Image.SCALE_SMOOTH);
                cursorWhite = toolkit.createCustomCursor(smallWhite, middle, "white");
            }

            // 3. Load the Empty Hand picture
            java.io.File emptyFile = new java.io.File("resources/images/hand_empty.png");
            if (emptyFile.exists()) {
                java.awt.Image emptyImg = javax.imageio.ImageIO.read(emptyFile);
                java.awt.Image smallEmpty = emptyImg.getScaledInstance(32, 32, java.awt.Image.SCALE_SMOOTH);
                cursorEmpty = toolkit.createCustomCursor(smallEmpty, middle, "empty");
            }

        } catch (Exception e) {
            // If something goes wrong, just print the error and use the normal mouse cursor
            System.out.println("Could not load cursor images: " + e.getMessage());
        }
    }

    // Handle clicking on the board to place a stone
    private void handleClick(java.awt.event.MouseEvent evt) {
        // Only allow clicks when it is this player's turn
        if (!isMyTurn || networkClient == null) {
            return;
        }

        int size = 9;
        float cellWidth = (float) jPanel_Board.getWidth() / (size - 1);
        float cellHeight = (float) jPanel_Board.getHeight() / (size - 1);

        int i = Math.round(evt.getX() / cellWidth);
        int j = Math.round(evt.getY() / cellHeight);

        // Bounds check
        if (i < 0 || i >= size || j < 0 || j >= size) {
            return;
        }

        // Send the move request to the server
        networkClient.sendMove(i, j);
        jPanel_Board.setCursor(cursorEmpty);
    }

    // Listens for game updates from the server
    private class GameScreenListener implements NetworkClient.ServerMessageListener {

        @Override
        public void onWelcome(String color) {
        }

        @Override
        public void onGameStarted() {
        }

        @Override
        public void onBoardUpdate(String boardData) {
            javax.swing.SwingUtilities.invokeLater(() -> {
                game.deserializeBoard(boardData);
                jPanel_Board.repaint();
            });
        }

        @Override
        public void onTurnChange(String whoseTurn) {
            javax.swing.SwingUtilities.invokeLater(() -> {
                isMyTurn = whoseTurn.equals(myColor);
                jLabel_CurrentTurn.setText("Current Turn : " + whoseTurn);
                updateTurnIndicator();
            });
        }

        @Override
        public void onMoveOk(int row, int col, String color) {
            javax.swing.SwingUtilities.invokeLater(() -> {
                moveCount++;
                movesModel.addElement(moveCount + ". " + color + " → (" + row + "," + col + ")");
                jList1_Moves.ensureIndexIsVisible(movesModel.size() - 1);
            });
        }

        @Override
        public void onPassOk(String color) {
            javax.swing.SwingUtilities.invokeLater(() -> {
                moveCount++;
                movesModel.addElement(moveCount + ". " + color + " — PASS");
                jList1_Moves.ensureIndexIsVisible(movesModel.size() - 1);
            });
        }

        @Override
        public void onInvalidMove() {
            javax.swing.SwingUtilities.invokeLater(() -> jLabel2.setText("  Invalid move! Try again."));
        }

        @Override
        public void onGameOver(String winner) {
            javax.swing.SwingUtilities.invokeLater(() -> {
                EndScreen endScreen = new EndScreen(networkClient, winner, myColor);
                endScreen.setVisible(true);
                GameScreen.this.dispose();
            });
        }

        @Override
        public void onOpponentDisconnected() {
            javax.swing.SwingUtilities.invokeLater(() -> {
                javax.swing.JOptionPane.showMessageDialog(GameScreen.this,
                        "Your opponent has disconnected. Returning to Start Screen.",
                        "Opponent Left", javax.swing.JOptionPane.WARNING_MESSAGE);
                if (networkClient != null)
                    networkClient.disconnect();
                new StartScreen().setVisible(true);
                GameScreen.this.dispose();
            });
        }

        @Override
        public void onMessage(String text) {
            javax.swing.SwingUtilities.invokeLater(() -> jLabel_ConnectionStatus.setText(text));
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated
    // <editor-fold defaultstate="collapsed" desc="Generated
    // Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jPanel_SideBar = new javax.swing.JPanel();
        jPanel_ButtonsBar = new javax.swing.JPanel();
        jButton_RestartGame = new javax.swing.JButton();
        jButton_ExitGame = new javax.swing.JButton();
        jPanel_MovesBar = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jList1_Moves = new javax.swing.JList<>();
        jLabel1 = new javax.swing.JLabel();
        jPanel_StatusBar = new javax.swing.JPanel();
        jLabel_ConnectionStatus = new javax.swing.JLabel();
        jLabel_CurrentTurn = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jPanel_Board = new javax.swing.JPanel() {
            @Override
            protected void paintComponent(java.awt.Graphics g) {
                super.paintComponent(g);

                int size = 9;
                float cellWidth = (float) getWidth() / (size - 1);
                float cellHeight = (float) getHeight() / (size - 1);

                // grid
                g.setColor(java.awt.Color.GRAY);
                for (int i = 0; i < size; i++) {
                    int x = Math.round(i * cellWidth);
                    int y = Math.round(i * cellHeight);

                    g.drawLine(x, 0, x, getHeight());
                    g.drawLine(0, y, getWidth(), y);
                }

                // stones (squares)
                for (int i = 0; i < size; i++) {
                    for (int j = 0; j < size; j++) {

                        if (game.getBoard()[i][j] != 0) {

                            int px = Math.round(i * cellWidth);
                            int py = Math.round(j * cellHeight);

                            int squareSize = Math.round(Math.min(cellWidth, cellHeight) * 0.8f);

                            // fill
                            // fill
                            if (game.getBoard()[i][j] == 1)
                                g.setColor(java.awt.Color.BLACK);
                            else
                                g.setColor(java.awt.Color.WHITE);

                            g.fillRect(px - squareSize / 2, py - squareSize / 2, squareSize, squareSize);

                            // outline
                            g.setColor(java.awt.Color.BLACK);
                            g.drawRect(px - squareSize / 2, py - squareSize / 2, squareSize, squareSize);
                        }
                    }
                }
            }
        };
        jLabel2 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("GO Game");
        setResizable(false);

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));

        jPanel_SideBar.setBackground(new java.awt.Color(204, 204, 204));
        jPanel_SideBar.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 51, 0)));

        jPanel_ButtonsBar.setBackground(new java.awt.Color(255, 255, 255));
        jPanel_ButtonsBar.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        jButton_RestartGame.setBackground(new java.awt.Color(102, 51, 0));
        jButton_RestartGame.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jButton_RestartGame.setForeground(new java.awt.Color(255, 255, 255));
        jButton_RestartGame.setText("PASS");
        jButton_RestartGame.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createMatteBorder(0, 0, 4, 4, java.awt.Color.BLACK),
                javax.swing.BorderFactory.createCompoundBorder(
                        javax.swing.BorderFactory.createLineBorder(java.awt.Color.BLACK, 2),
                        javax.swing.BorderFactory.createEmptyBorder(8, 16, 8, 16))));

        jButton_ExitGame.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jButton_ExitGame.setText("EXIT GAME");
        jButton_ExitGame.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createMatteBorder(0, 0, 4, 4, java.awt.Color.BLACK),
                javax.swing.BorderFactory.createCompoundBorder(
                        javax.swing.BorderFactory.createLineBorder(java.awt.Color.BLACK, 2),
                        javax.swing.BorderFactory.createEmptyBorder(8, 16, 8, 16))));
        jButton_ExitGame.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_ExitGameActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel_ButtonsBarLayout = new javax.swing.GroupLayout(jPanel_ButtonsBar);
        jPanel_ButtonsBar.setLayout(jPanel_ButtonsBarLayout);
        jPanel_ButtonsBarLayout.setHorizontalGroup(
                jPanel_ButtonsBarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel_ButtonsBarLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel_ButtonsBarLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jButton_RestartGame, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jButton_ExitGame, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap()));
        jPanel_ButtonsBarLayout.setVerticalGroup(
                jPanel_ButtonsBarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel_ButtonsBarLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jButton_RestartGame, javax.swing.GroupLayout.PREFERRED_SIZE, 40,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButton_ExitGame, javax.swing.GroupLayout.PREFERRED_SIZE, 40,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));

        jPanel_MovesBar.setBackground(new java.awt.Color(255, 255, 255));
        jPanel_MovesBar.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 51, 0)));
        jPanel_MovesBar.setForeground(new java.awt.Color(102, 51, 0));

        jScrollPane1.setViewportView(jList1_Moves);

        jLabel1.setBackground(new java.awt.Color(255, 255, 255));
        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(102, 51, 0));
        jLabel1.setText("Player Moves");

        javax.swing.GroupLayout jPanel_MovesBarLayout = new javax.swing.GroupLayout(jPanel_MovesBar);
        jPanel_MovesBar.setLayout(jPanel_MovesBarLayout);
        jPanel_MovesBarLayout.setHorizontalGroup(
                jPanel_MovesBarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel_MovesBarLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel_MovesBarLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jScrollPane1)
                                        .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap()));
        jPanel_MovesBarLayout.setVerticalGroup(
                jPanel_MovesBarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel_MovesBarLayout.createSequentialGroup()
                                .addGap(9, 9, 9)
                                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 28,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 350,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap()));

        jPanel_StatusBar.setBackground(new java.awt.Color(255, 255, 255));
        jPanel_StatusBar.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(102, 51, 0), 1, true));

        jLabel_ConnectionStatus.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel_ConnectionStatus.setForeground(new java.awt.Color(51, 153, 0));
        jLabel_ConnectionStatus.setText("Connection : Connected");

        jLabel_CurrentTurn.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel_CurrentTurn.setText("Current Turn : Black");

        jLabel4.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel4.setForeground(new java.awt.Color(102, 51, 0));
        jLabel4.setText("Match Status :");

        javax.swing.GroupLayout jPanel_StatusBarLayout = new javax.swing.GroupLayout(jPanel_StatusBar);
        jPanel_StatusBar.setLayout(jPanel_StatusBarLayout);
        jPanel_StatusBarLayout.setHorizontalGroup(
                jPanel_StatusBarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel_StatusBarLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel_StatusBarLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jLabel_CurrentTurn, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jLabel_ConnectionStatus, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap()));
        jPanel_StatusBarLayout.setVerticalGroup(
                jPanel_StatusBarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel_StatusBarLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 33,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel_ConnectionStatus, javax.swing.GroupLayout.PREFERRED_SIZE, 26,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel_CurrentTurn, javax.swing.GroupLayout.PREFERRED_SIZE, 26,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap()));

        javax.swing.GroupLayout jPanel_SideBarLayout = new javax.swing.GroupLayout(jPanel_SideBar);
        jPanel_SideBar.setLayout(jPanel_SideBarLayout);
        jPanel_SideBarLayout.setHorizontalGroup(
                jPanel_SideBarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel_SideBarLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel_SideBarLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jPanel_MovesBar, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jPanel_ButtonsBar, javax.swing.GroupLayout.Alignment.TRAILING,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jPanel_StatusBar, javax.swing.GroupLayout.Alignment.TRAILING,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap()));
        jPanel_SideBarLayout.setVerticalGroup(
                jPanel_SideBarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel_SideBarLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jPanel_StatusBar, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jPanel_MovesBar, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jPanel_ButtonsBar, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));

        jPanel_Board.setBackground(new java.awt.Color(255, 204, 153));
        jPanel_Board.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), 3));
        jPanel_Board.setPreferredSize(new java.awt.Dimension(550, 550));
        jPanel_Board.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jPanel_BoardMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout jPanel_BoardLayout = new javax.swing.GroupLayout(jPanel_Board);
        jPanel_Board.setLayout(jPanel_BoardLayout);
        jPanel_BoardLayout.setHorizontalGroup(
                jPanel_BoardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 0, Short.MAX_VALUE));
        jPanel_BoardLayout.setVerticalGroup(
                jPanel_BoardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 544, Short.MAX_VALUE));

        jLabel2.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel2.setForeground(new java.awt.Color(102, 51, 0));
        jLabel2.setText("> Your Turn");

        jLabel5.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jLabel5.setForeground(new java.awt.Color(102, 51, 0));
        jLabel5.setText("GO Game");
        jLabel5.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jLabel5.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                .addContainerGap(47, Short.MAX_VALUE)
                                .addGroup(jPanel1Layout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(jPanel_Board, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 180,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jLabel5, javax.swing.GroupLayout.DEFAULT_SIZE, 550,
                                                Short.MAX_VALUE))
                                .addGap(48, 48, 48)
                                .addComponent(jPanel_SideBar, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)));
        jPanel1Layout.setVerticalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jPanel_SideBar, javax.swing.GroupLayout.DEFAULT_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                .addContainerGap(8, Short.MAX_VALUE)
                                .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 33,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jPanel_Board, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel2)
                                .addGap(18, 18, 18)));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton_ExitGameActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jButton_ExitGameActionPerformed
        // Disconnect from server and return to start screen
        if (networkClient != null) {
            networkClient.disconnect();
        }
        StartScreen start = new StartScreen();
        start.setVisible(true);
        this.dispose();
    }// GEN-LAST:event_jButton_ExitGameActionPerformed

    private void jPanel_BoardMouseClicked(java.awt.event.MouseEvent evt) {// GEN-FIRST:event_jPanel_BoardMouseClicked
        handleClick(evt);
    }// GEN-LAST:event_jPanel_BoardMouseClicked

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        // <editor-fold defaultstate="collapsed" desc=" Look and feel setting code
        // (optional) ">
        /*
         * If Nimbus (introduced in Java SE 6) is not available, stay with the default
         * look and feel.
         * For details see
         * http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }
        // </editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new GameScreen().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton_ExitGame;
    private javax.swing.JButton jButton_RestartGame;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel_ConnectionStatus;
    private javax.swing.JLabel jLabel_CurrentTurn;
    private javax.swing.JList<String> jList1_Moves;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel_Board;
    private javax.swing.JPanel jPanel_ButtonsBar;
    private javax.swing.JPanel jPanel_MovesBar;
    private javax.swing.JPanel jPanel_SideBar;
    private javax.swing.JPanel jPanel_StatusBar;
    private javax.swing.JScrollPane jScrollPane1;
    // End of variables declaration//GEN-END:variables
}