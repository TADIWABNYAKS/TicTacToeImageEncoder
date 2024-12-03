import org.junit.Assert;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File; 
import java.io.FileReader;
import java.io.IOException;

public class ImageEncoderTest {

    @Test
    public void testEncodeDecodeCycle() throws IOException {
        // Arrange
        String inputPath = "test1.png";
        String tacPath = "output1.tac";
        String decodedPath = "decodedResult.png";

        // Encode the image
        ImageEncoder.encodeImageToTicTacToe(inputPath, tacPath);

        // Decode it back
        ImageEncoder.decodeTicTacToeToImage(tacPath, decodedPath);

        // Assert - Compare original and decoded images
        BufferedImage originalImage = ImageIO.read(new File(inputPath));
        BufferedImage decodedImage = ImageIO.read(new File(decodedPath));

        assertImagesEqual(originalImage, decodedImage);
    }
    
    @Test
    public void testMissingArguments() {
        // Arrange
        String[] args = new String[2]; // Missing mode argument

        // Act
        try {
            ImageEncoder.main(args);
            Assert.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // Expected e
        }
    }

    }

