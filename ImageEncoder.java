import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import javax.imageio.ImageIO;

public class ImageEncoder extends RecursiveTask<List<String[][]>> {

    private static final int BOARD_SIZE = 3;  // Each board is 3x3
    private BufferedImage image;
    private int startX;
    private int startY;
    private int endX;
    private int endY;

    public ImageEncoder(BufferedImage image, int startX, int startY, int endX, int endY) {
        this.image = image;
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
    }

    @Override
    protected List<String[][]> compute() {
        if ((endX - startX == 1) && (endY - startY == 1)) {
            // Process a single pixel (now represented by 3 boards)
            return encodePixelAsBoards(startX, startY);
        } else {
            // Split the task into smaller subtasks
            int midX = (startX + endX) / 2;
            int midY = (startY + endY) / 2;

            ImageEncoder topLeft = new ImageEncoder(image, startX, startY, midX, midY);
            ImageEncoder topRight = new ImageEncoder(image, midX, startY, endX, midY);
            ImageEncoder bottomLeft = new ImageEncoder(image, startX, midY, midX, endY);
            ImageEncoder bottomRight = new ImageEncoder(image, midX, midY, endX, endY);

            invokeAll(topLeft, topRight, bottomLeft, bottomRight);

            List<String[][]> result = new ArrayList<>();
            result.addAll(topLeft.join());
            result.addAll(topRight.join());
            result.addAll(bottomLeft.join());
            result.addAll(bottomRight.join());

            return result;
        }
    }

    private List<String[][]> encodePixelAsBoards(int x, int y) {
        List<String[][]> boards = new ArrayList<>();

        int rgb = image.getRGB(x, y);
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;

        // Create three boards: one for Red, Green, and Blue
        String[][] redBoard = encodeValueAsBoard(red);
        String[][] greenBoard = encodeValueAsBoard(green);
        String[][] blueBoard = encodeValueAsBoard(blue);

        boards.add(redBoard);
        boards.add(greenBoard);
        boards.add(blueBoard);

        return boards;
    }

    private String[][] encodeValueAsBoard(int value) {
        String[][] board = new String[BOARD_SIZE][BOARD_SIZE];
        String binaryString = String.format("%9s", Integer.toBinaryString(value)).replace(' ', '0');  // Ensure 9-bit binary

        // Map the binary string to a 3x3 board, ignoring the most significant bit (assumed to be 0/O)
        int index = 0;
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (index == 0) {
                    board[i][j] = "O";  // Default most significant bit as 0/O
                } else {
                    board[i][j] = binaryString.charAt(index - 1) == '1' ? "X" : "O";
                }
                index++;
            }
        }
        return board;
    }

    // Encodes the image in parallel
    public static List<String[][]> parallelEncode(BufferedImage image) {
        ForkJoinPool pool = new ForkJoinPool();
        ImageEncoder task = new ImageEncoder(image, 0, 0, image.getWidth(), image.getHeight());
        return pool.invoke(task); 
    }

    // Main method inside the class to handle encoding
    public static void main(String[] args) throws Exception {

        BufferedImage image = ImageIO.read(new File("C://Users//tadiw//Desktop//TicTacToe//test1.png"));

        // Encode the image into Tic-Tac-Toe boards (3 boards per pixel)
        List<String[][]> encodedBoards = ImageEncoder.parallelEncode(image);

        // Save boards into a file
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("C://Users//tadiw//Desktop//New folder//test.tac"))) {
            for (int i = 0; i < encodedBoards.size(); i += 3) {
                // Write Red board
                writeBoard(bw, encodedBoards.get(i));
                bw.write("R;");

                // Write Green board
                writeBoard(bw, encodedBoards.get(i + 1));
                bw.write("G;");

                // Write Blue board
                writeBoard(bw, encodedBoards.get(i + 2));
                bw.write("B;");
                bw.newLine();
            }
        } catch (Exception e) {
            System.out.println(e);
        }

        System.out.println("Image successfully Tic Tac Toed (Encoded)");
    }

    // Helper method to write a board to the file
    private static void writeBoard(BufferedWriter bw, String[][] board) throws Exception {
        for (int i = 0; i < BOARD_SIZE; i++) {
            bw.write(String.join(",", board[i]));
            if (i < BOARD_SIZE - 1) {
                bw.write(";");
            }
        }
    }
}
