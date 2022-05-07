package view;


import model.*;
import controller.ClickController;
import model.KingChessComponent;
import model.KnightChessComponent;
import model.PawnChessComponent;
import model.QueenChessComponent;
import store.archive.Archive;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * 这个类表示面板上的棋盘组件对象
 */
public class Chessboard extends JComponent {
    /**
     * CHESSBOARD_SIZE： 棋盘是8 * 8的
     * <br>
     * BACKGROUND_COLORS: 棋盘的两种背景颜色
     * <br>
     * chessListener：棋盘监听棋子的行动
     * <br>
     * chessboard: 表示8 * 8的棋盘
     * <br>
     * currentColor: 当前行棋方
     */
    private static final int CHESSBOARD_SIZE = 8;

    private final ChessComponent[][] chessComponents = new ChessComponent[CHESSBOARD_SIZE][CHESSBOARD_SIZE];
    private ChessColor currentColor = ChessColor.WHITE;
    //all chessComponents in this chessboard are shared only one model controller
    private final ClickController clickController = new ClickController(this);
    private final int CHESS_SIZE;
    // 等待执行的任务列表
    HashMap<String, Runnable> asyncTasks = new HashMap<String, Runnable>();
    HashMap<String, Integer> asyncTasksSteps = new HashMap<String, Integer>();
    protected static Chessboard chessboardInstance;
    protected Archive archive;
    private JLabel statusLabel = new JLabel();

    public void setStatusLabelText(String text) {
        this.statusLabel.setText(text);
    }

    public void setStatusLabel(JLabel statusLabel) {
        this.statusLabel = statusLabel;
    }

    public Chessboard(int width, int height, Archive archive) {
        setLayout(null); // Use absolute layout.
        setSize(width, height);
        CHESS_SIZE = width / 8;
        System.out.printf("chessboard size = %d, chess size = %d\n", width, CHESS_SIZE);

        initiateEmptyChessboard();

        chessboardInstance = this;
        this.archive = archive;

        recoverFromArchive();
    }

    public Chessboard(int width, int height) {
        setLayout(null); // Use absolute layout.
        setSize(width, height);
        CHESS_SIZE = width / 8;
        System.out.printf("chessboard size = %d, chess size = %d\n", width, CHESS_SIZE);
        chessboardInstance = this;

        archive = new Archive();
        archive.initialize();

        initialAllChess();
    }

    public ChessComponent[][] getChessComponents() {
        return chessComponents;
    }

    public ChessColor getCurrentColor() {
        return currentColor;
    }

    public void putChessOnBoard(ChessComponent chessComponent) {
        int row = chessComponent.getChessboardPoint().getX(), col = chessComponent.getChessboardPoint().getY();

        if (chessComponents[row][col] != null) {
            remove(chessComponents[row][col]);
        }
        add(chessComponents[row][col] = chessComponent);
        this.repaint();
    }

    public void swapChessComponents(ChessComponent chess1, ChessComponent chess2) {
        // Note that chess1 has higher priority, 'destroys' chess2 if exists.
        if (!(chess2 instanceof EmptySlotComponent)) {
            remove(chess2);
            add(chess2 = new EmptySlotComponent(chess2.getChessboardPoint(), chess2.getLocation(), clickController, CHESS_SIZE));
        }

        if (chess1.getChessboardPoint().getY() != 0) {
            if (chessComponents[chess1.getChessboardPoint().getX()][chess1.getChessboardPoint().getY() - 1] instanceof PawnChessComponent) {
                if (((PawnChessComponent) chessComponents[chess1.getChessboardPoint().getX()][chess1.getChessboardPoint().getY() - 1]).isCanBeEnAsPassant() && chess2.getChessboardPoint().getY() == chess1.getChessboardPoint().getY() - 1) {
                    remove(chessComponents[chess1.getChessboardPoint().getX()][chess1.getChessboardPoint().getY() - 1]);
                    putChessOnBoard(new EmptySlotComponent(new ChessboardPoint(chess1.getChessboardPoint().getX(), chess1.getChessboardPoint().getY() - 1), calculatePoint(chess1.getChessboardPoint().getX(), chess1.getChessboardPoint().getY() - 1), clickController, CHESS_SIZE));
                }
            }
        }

        if (chess1.getChessboardPoint().getY() != CHESSBOARD_SIZE - 1) {
            if (chessComponents[chess1.getChessboardPoint().getX()][chess1.getChessboardPoint().getY() + 1] instanceof PawnChessComponent) {
                if (((PawnChessComponent) chessComponents[chess1.getChessboardPoint().getX()][chess1.getChessboardPoint().getY() + 1]).isCanBeEnAsPassant() && chess2.getChessboardPoint().getY() == chess1.getChessboardPoint().getY() + 1) {
                    remove(chessComponents[chess1.getChessboardPoint().getX()][chess1.getChessboardPoint().getY() + 1]);
                    putChessOnBoard(new EmptySlotComponent(new ChessboardPoint(chess1.getChessboardPoint().getX(), chess1.getChessboardPoint().getY() + 1), calculatePoint(chess1.getChessboardPoint().getX(), chess1.getChessboardPoint().getY() + 1), clickController, CHESS_SIZE));
                }
            }
        }


        chess1.swapLocation(chess2);
        int row1 = chess1.getChessboardPoint().getX(), col1 = chess1.getChessboardPoint().getY();
        chessComponents[row1][col1] = chess1;
        int row2 = chess2.getChessboardPoint().getX(), col2 = chess2.getChessboardPoint().getY();
        chessComponents[row2][col2] = chess2;

        chess1.repaint();
        chess2.repaint();

        checkAndInvoke();
        archive.stepTrigger(this, chess1, chess2);
    }

    public void initiateEmptyChessboard() {
        for (int i = 0; i < chessComponents.length; i++) {
            for (int j = 0; j < chessComponents[i].length; j++) {
                putChessOnBoard(new EmptySlotComponent(new ChessboardPoint(i, j), calculatePoint(i, j), clickController, CHESS_SIZE));
            }
        }
    }

    public void swapColor() {
        currentColor = currentColor == ChessColor.BLACK ? ChessColor.WHITE : ChessColor.BLACK;
        statusLabel.setText("Current Color: " + currentColor.getName());
        statusLabel.repaint();
    }

    private void initRookOnBoard(int row, int col, ChessColor color) {
        ChessComponent chessComponent = new RookChessComponent(new ChessboardPoint(row, col), calculatePoint(row, col), color, clickController, CHESS_SIZE);
        chessComponent.setVisible(true);
        putChessOnBoard(chessComponent);
    }

    private void initBishopOnBoard(int row, int col, ChessColor color) {
        ChessComponent chessComponent = new BishopChessComponent(new ChessboardPoint(row, col), calculatePoint(row, col), color, clickController, CHESS_SIZE);
        chessComponent.setVisible(true);
        putChessOnBoard(chessComponent);
    }

    private void initKnightOnBoard(int row, int col, ChessColor color) {
        ChessComponent chessComponent = new KnightChessComponent(new ChessboardPoint(row, col), calculatePoint(row, col), color, clickController, CHESS_SIZE);
        chessComponent.setVisible(true);
        putChessOnBoard(chessComponent);
    }

    private void initPawnOnBoard(int row, int col, ChessColor color) {
        ChessComponent chessComponent = new PawnChessComponent(new ChessboardPoint(row, col), calculatePoint(row, col), color, clickController, CHESS_SIZE);
        chessComponent.setVisible(true);
        putChessOnBoard(chessComponent);
    }

    private void initQueenOnBoard(int row, int col, ChessColor color) {
        ChessComponent chessComponent = new QueenChessComponent(new ChessboardPoint(row, col), calculatePoint(row, col), color, clickController, CHESS_SIZE);
        chessComponent.setVisible(true);
        putChessOnBoard(chessComponent);
    }

    private void initKingOnBoard(int row, int col, ChessColor color) {
        ChessComponent chessComponent = new KingChessComponent(new ChessboardPoint(row, col), calculatePoint(row, col), color, clickController, CHESS_SIZE);
        chessComponent.setVisible(true);
        putChessOnBoard(chessComponent);
    }

    public void initialAllChess() {
        initiateEmptyChessboard();

        initRookOnBoard(0, 0, ChessColor.BLACK);
        initRookOnBoard(0, CHESSBOARD_SIZE - 1, ChessColor.BLACK);
        initRookOnBoard(CHESSBOARD_SIZE - 1, 0, ChessColor.WHITE);
        initRookOnBoard(CHESSBOARD_SIZE - 1, CHESSBOARD_SIZE - 1, ChessColor.WHITE);

        initBishopOnBoard(0, 2, ChessColor.BLACK);
        initBishopOnBoard(0, CHESSBOARD_SIZE - 3, ChessColor.BLACK);
        initBishopOnBoard(CHESSBOARD_SIZE - 1, 2, ChessColor.WHITE);
        initBishopOnBoard(CHESSBOARD_SIZE - 1, CHESSBOARD_SIZE - 3, ChessColor.WHITE);

        initKnightOnBoard(0, 1, ChessColor.BLACK);
        initKnightOnBoard(0, CHESSBOARD_SIZE - 2, ChessColor.BLACK);
        initKnightOnBoard(CHESSBOARD_SIZE - 1, 1, ChessColor.WHITE);
        initKnightOnBoard(CHESSBOARD_SIZE - 1, CHESSBOARD_SIZE - 2, ChessColor.WHITE);

        initQueenOnBoard(0, 3, ChessColor.BLACK);
        initQueenOnBoard(CHESSBOARD_SIZE - 1, 3, ChessColor.WHITE);

        initKingOnBoard(0, 4, ChessColor.BLACK);
        initKingOnBoard(CHESSBOARD_SIZE - 1, 4, ChessColor.WHITE);

        for (int i = 0; i < 8; i++) {
            initPawnOnBoard(1, i, ChessColor.BLACK);
            initPawnOnBoard(CHESSBOARD_SIZE - 2, i, ChessColor.WHITE);
        }

        this.currentColor = ChessColor.WHITE;
        statusLabel.setText("Current Color: " + currentColor.getName());

        archive.initialize();

        ChessGameFrame.getInstance().setSaveButtonEnabled(false);
        addAsyncTask(() -> {
            ChessGameFrame.getInstance().setSaveButtonEnabled(true);
        }, 1);
    }


    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }


    public Point calculatePoint(int row, int col) {
        return new Point(col * CHESS_SIZE, row * CHESS_SIZE);
    }

    public void loadGame(List<String> chessData) {
        chessData.forEach(System.out::println);
    }

    public void addAsyncTask(Runnable task, int steps) {
        // 打标签
        String uuid = UUID.randomUUID().toString();
        asyncTasks.put(uuid, task);
        asyncTasksSteps.put(uuid, steps);
    }

    public static void invokeLater(Runnable task, int steps) {
        getInstance().addAsyncTask(task, steps);
    }

    private void checkAndInvoke() {

        Iterator<Map.Entry<String, Integer>> iterator = asyncTasksSteps.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Integer> entry = iterator.next();

            String id = entry.getKey();
            int steps = entry.getValue();
            steps--;

            if (steps <= 0) {
                Runnable task = asyncTasks.get(id);
                task.run();
                iterator.remove();
                asyncTasks.remove(id);
            } else {
                asyncTasksSteps.put(id, steps);
            }
        }
    }

    private void recoverFromArchive() {
        ChessComponent[][] chessComponents = archive.getChessComponents();

        for (int m = 0; m < chessComponents.length; m++) {
            for (int n = 0; n < chessComponents[m].length; n++) {
                ChessComponent chessComponent = chessComponents[m][n];
                chessComponent.setVisible(true);
                putChessOnBoard(chessComponent);
            }
        }

        this.currentColor = archive.getCurrentColor();
        ChessGameFrame.getInstance().setSaveButtonEnabled(true);

    }

    public static Chessboard getInstance() {
        return chessboardInstance;
    }

    public Archive getArchive() {
        return archive;
    }

    public ClickController getClickController() {
        return clickController;
    }

    public int getCHESS_SIZE() {
        return CHESS_SIZE;
    }


}
