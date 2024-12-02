import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.RecursiveTask;

public class ImageEncoder {

    private static final int SEQUENTIAL_THRESHOLD = 12; // 

    public static void main(String[] args) {
        try {
            BufferedImage image =  ImageIO.read(new File("test1.png")); 
            String outputFileName = "outputt1.tac"; //Replace with args[0] for CLI 
            ForkJoinPool forkJoinPool = new ForkJoinPool();
            EncoderTask encoderTask = new EncoderTask(image, 0, 0, image.getWidth(), image.getHeight());
            List<String> encodedLines = forkJoinPool.invoke(encoderTask);
            writeToFile(outputFileName, encodedLines);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static class EncoderTask extends RecursiveTask<List<String>> {
        private final BufferedImage image;
        private final int xStart, yStart, width, height;

        public EncoderTask(BufferedImage image, int xStart, int yStart, int width, int height) {
            this.image = image;
            this.xStart = xStart;
            this.yStart = yStart;
            this.width = width;
            this.height = height;
        }
 
        @Override
        protected List<String> compute() {
            if (width * height <= SEQUENTIAL_THRESHOLD) {
                return processSequentially();
            } else {
                int halfWidth = width / 2;
                int halfHeight = height / 2;
    
                //Divide the image into quadrants 
                EncoderTask task1 = new EncoderTask(image, xStart, yStart, halfWidth, halfHeight);
                EncoderTask task2 = new EncoderTask(image, xStart + halfWidth, yStart, width - halfWidth, halfHeight);
                EncoderTask task3 = new EncoderTask(image, xStart, yStart + halfHeight, halfWidth, height - halfHeight);
                EncoderTask task4 = new EncoderTask(image, xStart + halfWidth, yStart + halfHeight, width - halfWidth, height - halfHeight);

                task1.fork();
                task2.fork();
                task3.fork();


                List<String> part4 = task4.compute();
                List<String> part1 = task1.join();
                List<String> part2 = task2.join();
                List<String> part3 = task3.join();
            
                //Merge boards from these tasks in order
                part1.addAll(part2);
                part1.addAll(part3);
                part1.addAll(part4);
                return part1;
            }
        }

        private List<String> processSequentially() {
            // Implement sequential processing and collect lines in a list
            return null; 
        }
    }

    private static void writeToFile(String outputFileName, List<String> encodedLines) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFileName))) {
            for (String line : encodedLines) {
                writer.write(line);
                writer.newLine();
            }
        }
    }
} 