/** 
 * Encodes images into tic tac toe boards where each pixel is a board , all boards should be saved into a file to allow decoding back into the original image
 * @author TADIWABNYAKS 
 */

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
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
    private int numBoardsWide;
    private int numBoardsHigh;

    public ImageEncoder(BufferedImage image, int startX, int startY, int endX, int endY, int numBoardsWide, int numBoardsHigh) {
        this.image = image;
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
        this.numBoardsWide = numBoardsWide;
        this.numBoardsHigh = numBoardsHigh;
    }

    @Override
    protected List<String[][]> compute() {
        if ((endX - startX <= BOARD_SIZE) && (endY - startY <= BOARD_SIZE)) {
            // Process a single 3x3 board
            return encodeBoard(startX, startY);
        } else {
            // Split the task into smaller subtasks
            int midX = (startX + endX) / 2;
            int midY = (startY + endY) / 2;

            ImageEncoder topLeft = new ImageEncoder(image, startX, startY, midX, midY, numBoardsWide, numBoardsHigh);
            ImageEncoder topRight = new ImageEncoder(image, midX, startY, endX, midY, numBoardsWide, numBoardsHigh);
            ImageEncoder bottomLeft = new ImageEncoder(image, startX, midY, midX, endY, numBoardsWide, numBoardsHigh);
            ImageEncoder bottomRight = new ImageEncoder(image, midX, midY, endX, endY, numBoardsWide, numBoardsHigh);

            invokeAll(topLeft, topRight, bottomLeft, bottomRight);

            List<String[][]> result = new ArrayList<>();
            result.addAll(topLeft.join());
            result.addAll(topRight.join());
            result.addAll(bottomLeft.join());
            result.addAll(bottomRight.join());

            return result;
        }
    }

    // Encode 3x3 board based on pixel values
    private List<String[][]> encodeBoard(int startX, int startY) {
        List<String[][]> boards = new ArrayList<>();
        String[][] board = new String[BOARD_SIZE][BOARD_SIZE];

        for (int y = 0; y < BOARD_SIZE; y++) {
            for (int x = 0; x < BOARD_SIZE; x++) {
                int globalX = startX + x;
                int globalY = startY + y;
                if (globalX < image.getWidth() && globalY < image.getHeight()) {
                    int pixelValue = image.getRGB(globalX, globalY) & 0xFF;  // Grayscale value
                    board[y][x] = (pixelValue > 128) ? "X" : "O";  // X for bright, O for dark
                } else {
                    board[y][x] = "O";  // Default to O
                }
            }
        }
        boards.add(board);
        return boards;
    }

    // Encodes the image in parallel
    public static List<String[][]> parallelEncode(BufferedImage image, int numBoardsWide, int numBoardsHigh) {
        ForkJoinPool pool = new ForkJoinPool();
        ImageEncoder task = new ImageEncoder(image, 0, 0, image.getWidth(), image.getHeight(), numBoardsWide, numBoardsHigh);
        return pool.invoke(task); 
    }

    // Main method inside the class to handle encoding
    public static void main(String[] args) throws Exception {


        BufferedImage image = ImageIO.read(new File("C://Users//tadiw//Desktop//TicTacToe//test1.jpg"));
        int numBoardsWide = image.getWidth() / BOARD_SIZE;
        int numBoardsHigh = image.getHeight() / BOARD_SIZE;

        // Encode the image into Tic-Tac-Toe boards 
        List<String[][]> encodedBoards = ImageEncoder.parallelEncode(image, numBoardsWide, numBoardsHigh);

        //REPLACE 
        // Print out the boards , REPLACE with saving boards into either .csv or .txt file for the decoding. 
        for (String[][] board : encodedBoards) {
            for (String[] row : board) {
                System.out.println(String.join(" ", row));
            }
            System.out.println("---");
        }
        //END REPLACE

        System.out.println("Image successfully Tic Tac Toed (Encoded) ");
    }
}
