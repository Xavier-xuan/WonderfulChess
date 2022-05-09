package Archive;

import Archive.DataModel.ChessDataModel;
import Archive.Exception.ArchiveException;
import Archive.Exception.DuplicatedChessException;
import Archive.Exception.InvalidChessboardSizeException;
import Archive.Exception.InvalidStepException;
import Model.EmptySlotComponent;
import com.google.gson.*;
import Model.ChessColor;
import Model.ChessComponent;
import Archive.DataModel.ChessDataModelDeserializer;
import View.Chessboard;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;

public class Archive {
    private Date createdAt;
    private String name;
    private ArrayList<Step> steps;
    private ChessColor currentColor;
    private String path;

    public Archive() {
        createdAt = new Date();
    }

    public void stepTrigger(Chessboard chessboard, ChessComponent chess1, ChessComponent chess2) {
        this.steps.add(
                new Step(chessboard, chess1, chess2)
        );
        currentColor = chess1.getChessColor().equals(ChessColor.WHITE) ? ChessColor.BLACK : ChessColor.WHITE;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

    public void save() {
        if (isFresh())
            return;

        File file = new File(path);
        try {
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(this.toString());
            fileWriter.close();
        } catch (IOException e) {
            System.out.println("Failed to save archive.");
        }

    }

    public ChessComponent[][] withdraw() {
        if (this.steps.size() > 1) {
            steps.remove(steps.size() - 1);
        }
        return getChessComponents();
    }

    public void initialize() {
        this.steps = new ArrayList<>();
        this.createdAt = new Date();
        this.currentColor = ChessColor.WHITE;
    }

    public static Gson getGson() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(ChessDataModel.class, new ChessDataModelDeserializer());
        return gsonBuilder.create();
    }

    public static Archive getArchiveFromPath(String path) throws FileNotFoundException,
            JsonParseException {
        BufferedReader in = new BufferedReader(new FileReader(path));
        Archive archive = getGson().fromJson(in, Archive.class);
        archive.validate();
        return archive;

    }

    public boolean isEmpty() {
        return steps.size() < 1;
    }

    public Step lastStep() {
        if (isEmpty()) return null;
        return steps.get(steps.size() - 1);
    }

    public ChessComponent[][] getChessComponents() {
        return getChessComponents(Chessboard.getInstance());
    }

    public ChessComponent[][] getChessComponents(Chessboard chessboard) {
        if (!isEmpty()) {
            return lastStep().getChessComponents(chessboard);
        } else {
            return null;
        }
    }

    public ChessDataModel[][] getChessDataModels() {
        if (!isEmpty()) {
            return lastStep().getChessDataModels();

        } else {
            return null;
        }
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isFresh() {
        return this.path == null;
    }

    public ChessColor getCurrentColor() {
        return currentColor;
    }

    public void validate() throws JsonParseException {
        if (getChessDataModels().length > 8) {
            throw new InvalidChessboardSizeException("Invalid chessboard size:" + getChessDataModels().length);
        }

        ArrayList<String> existedChess = new ArrayList<>();
        for (int m = 0; m < getChessDataModels().length; m++) {
            if (getChessDataModels()[m].length > 8) {
                throw new InvalidChessboardSizeException("Invalid chessboard size:" + getChessDataModels()[m].length);
            }

            for (int n = 0; n < getChessDataModels()[m].length; n++) {
                ChessDataModel chessDataModel = getChessDataModels()[m][n];
                if (chessDataModel.getX() != m && chessDataModel.getY() != n) {
                    throw new ArchiveException("Chess's index doesn't match its position");
                }

                int x = chessDataModel.getX(), y = chessDataModel.getY();
                String positionString = String.format("%d%d", x, y);
                if (existedChess.contains(positionString)) {
                    throw new DuplicatedChessException(String.format("Duplicated Chess: x:%d y:%d", x, y));
                }

                existedChess.add(positionString);
            }
        }

        // 检查落子合法性
        // 新建一个虚拟棋盘
        Chessboard chessboard = new Chessboard(8, 8);
        chessboard.setVisible(false);
        ChessComponent[][] chessComponents = chessboard.getChessComponents();

        // 从第一步开始遍历
        for (int i = 0; i < steps.size(); i++) {
            Step step = steps.get(i);
            ChessDataModel chess1 = step.getChess1(), chess2 = step.getChess2();
            ChessComponent chess1Component = chessboard.getChessComponents()[chess1.getX()][chess1.getY()];
            ChessComponent chess2Component = chessboard.getChessComponents()[chess2.getX()][chess2.getY()];

            if (!(chess1Component instanceof EmptySlotComponent) && !chess1Component.canMoveTo(chessComponents, chess2Component.getChessboardPoint())) {
                throw new InvalidStepException(String.format("Invalid step: %s %s(%d,%d) -> %s %s(%d,%d)",
                        chess1.getChessColor().getName(), chess1.getChessType(), chess1.getX(), chess1.getY(),
                        chess2.getChessColor().getName(), chess2.getChessType(), chess2.getX(), chess2.getY()));
            }

            chessComponents = step.getChessComponents(chessboard);

        }

    }
}
