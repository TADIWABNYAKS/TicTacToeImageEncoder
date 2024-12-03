import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.imageio.ImageIO;

/**
 * This class provides methods to encode images into Tic-Tac-Toe boards and decode them back.
 * @author TBN 
 */
public class ImageEncoder {

    /**
     * Encodes an image from a PNG file into a .tac file using Tic-Tac-Toe boards.
     *
     * @param inputPath  path to input PNG file
     * @param outputPath path to output .tac file
     * @throws IOException if there is an error reading or writing files
     */
    public static void encodeImageToTicTacToe(String inputPath, String outputPath) throws IOException {
        // Read the PNG image
        BufferedImage image = ImageIO.read(new File(inputPath));
        int width = image.getWidth();
        int height = image.getHeight();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {
            // Write width and height
            writer.write(width + " " + height);
            writer.newLine();

            // Iterate over each pixel
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int rgb = image.getRGB(x, y);
                    // Extract R, G, B components
                    // The getRGB method returns an integer where:
                    // - higher bits represen transparency
                    // - next 8 bits are red, then green, then blue
                    // Below extract the red, green, and blue components.
                    int red = (rgb >> 16) & 0xFF;
                    int green = (rgb >> 8) & 0xFF;
                    int blue = rgb & 0xFF;

                    // Convert each component to board strings
                    String boardR = byteToBoardString((byte) red);
                    String boardG = byteToBoardString((byte) green);
                    String boardB = byteToBoardString((byte) blue);

                    // Write the board strings to the file in RGB order 
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

    /**
     * Decodes a .tac file back into an image and saves it as a PNG file.
     *
     * @param inputPath   path to input .tac file
     * @param outputPath  path to output PNG file
     * @throws IOException
     */
    public static void decodeTicTacToeToImage(String inputPath, String outputPath) throws IOException {
        java.util.List<String> lines = Files.readAllLines(Paths.get(inputPath));
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

                byte red = boardStringToByte(boardR);
                byte green = boardStringToByte(boardG);
                byte blue = boardStringToByte(boardB);

                // Set the pixel in the image
                int rgb = (red << 16) | (green << 8) | blue;
                image.setRGB(x, y, rgb);
            }
        }

        // Write the image to the output file
        ImageIO.write(image, "png", new File(outputPath));
    }

    /**
     * Converts a byte value into a Tic-Tac-Toe board string.
     *
     * @param b the byte value to convert
     * @return a string representing the Tic-Tac-Toe board
     */
    private static String byteToBoardString(byte b) {
        char[] board = new char[9];
        // Position 1 is always 'O' as specified in the problem
        board[0] = 'O';
        // Positions 2-9 correspond to bits 7-0 of the byte
        for (int i = 1; i <= 8; i++) {
            int bitIndex = 7 - (i - 1); // positions 2-9 correspond to bits 7-0
            boolean bitValue = ((b >> bitIndex) & 1) == 1;
            board[i] = bitValue ? 'X' : 'O';
        }
        return new String(board);
    }

    /**
     * Converts a Tic-Tac-Toe board string back into a byte value.
     *
     * @param board the board string to convert
     * @return the byte value represented by the board
     */
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
        if (args.length != 3) {
            System.out.println("Usage: java YourProgram <inputFile> <outputFile> <mode>");
            System.out.println("Modes: E (encode), D (decode)");
            System.exit(1);
        }
     
        String inputFile = args[0];
        String outputFile = args[1];
        String mode = args[2];
    
        try {
            if (mode.equalsIgnoreCase("E")) {
                System.out.println("Encoding...");
                encodeImageToTicTacToe(inputFile, outputFile);
                System.out.println("Image successfully encoded at: " + outputFile);
            } else if (mode.equalsIgnoreCase("D")) {
                System.out.println("Decoding...");
                decodeTicTacToeToImage(inputFile, outputFile);
                System.out.println("Image successfully decoded at: " + outputFile);
            } else {
                System.out.println("Invalid mode provided. Use 'E' for encoding or 'D' for decoding.");
                System.exit(1);
            }
        } catch (IOException e) {
            System.out.println("An error occurred during processing: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}