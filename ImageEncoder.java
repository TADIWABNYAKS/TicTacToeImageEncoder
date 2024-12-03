import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.imageio.ImageIO;

public class ImageEncoder {

    public static void encodeImageToTicTacToe(String pngFilePath, String tacFilePath) throws IOException {
        // Read the PNG image
        BufferedImage image = ImageIO.read(new File(pngFilePath));
        int width = image.getWidth();
        int height = image.getHeight();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tacFilePath))) {
            // Write width and height
            writer.write(width + " " + height);
            writer.newLine();

            // Iterate over each pixel
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int rgb = image.getRGB(x, y);
                    // Extract R, G, B components
                    //Bits 24-31: Alpha (transparency)
                    //Bits 16-23: Red
                    //Bits 8-15: Green
                    //Bits 0-7: Blue
                    int red = (rgb >> 16) & 0xFF;
                    int green = (rgb >> 8) & 0xFF;
                    int blue = rgb & 0xFF;

                    // Convert each component to board strings
                    String boardR = byteToBoardString((byte) red);
                    String boardG = byteToBoardString((byte) green);
                    String boardB = byteToBoardString((byte) blue);

                    // Write the board strings to the file
                    writer.write(boardR);
                    writer.newLine();
                    writer.write(boardG);
                    writer.newLine();
                    writer.write(boardB);
                    writer.newLine();
                }
            }
        }
    }

    public static void decodeTicTacToeToImage(String tacFilePath, String outputPngFilePath) throws IOException {
        java.util.List<String> lines = Files.readAllLines(Paths.get(tacFilePath));
        // First line contains width and height
        String[] wh = lines.remove(0).split(" ");
        int width = Integer.parseInt(wh[0]);
        int height = Integer.parseInt(wh[1]);

        // Create a new image to store the decoded data
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        // Iterate over each pixel
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Read R, G, B boards for the current pixel
                String boardR = lines.remove(0);
                String boardG = lines.remove(0);
                String boardB = lines.remove(0);

                // Convert board strings back to bytes
                byte red = boardStringToByte(boardR);
                byte green = boardStringToByte(boardG);
                byte blue = boardStringToByte(boardB);

                // Set the pixel in the image
                int rgb = (red << 16) | (green << 8) | blue;
                image.setRGB(x, y, rgb);
            }
        }

        // Write the image to the output file
        ImageIO.write(image, "png", new File(outputPngFilePath));
    }

    private static String byteToBoardString(byte b) {
        char[] board = new char[9];
        // Position 1 is always 'O'
        board[0] = 'O';
        // Positions 2-9 correspond to bits 7-0
        for (int i = 1; i <= 8; i++) {
            int bitIndex = 7 - (i - 1); // positions 2-9 correspond to bits 7-0
            boolean bitValue = ((b >> bitIndex) & 1) == 1;
            board[i] = bitValue ? 'X' : 'O';
        }
        return new String(board);
    }

    private static byte boardStringToByte(String board) {
        if (board.length() != 9) {
            throw new IllegalArgumentException("Board must have exactly 9 characters");
        }
        // Position 1 is ignored as it's always 'O'
        byte value = 0;
        for (int i = 1; i < 9; i++) {
            char c = board.charAt(i);
            if (c != 'X' && c != 'O') {
                throw new IllegalArgumentException("Invalid character in board string");
            }
            int bitValue = c == 'X' ? 1 : 0;
            int bitIndex = 7 - (i - 1); // positions 2-9 correspond to bits 7-0
            value |= (bitValue << bitIndex);
        }
        return value;
    }

    public static void main(String[] args) {
        try {
         //   encodeImageToTicTacToe("C:\\Users\\tadiw\\Desktop\\TicTacToe\\test1.png","C:\\Users\\tadiw\\Desktop\\TicTacToe\\test.tac");
            decodeTicTacToeToImage("C:\\Users\\tadiw\\Desktop\\TicTacToe\\test.tac", "C:\\Users\\tadiw\\Desktop\\TicTacToe\\decodedjoe.png");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}