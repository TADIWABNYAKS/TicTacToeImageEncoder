import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

/** Streaming TAC2 codec: eight data cells plus one even-parity cell per channel. */
public final class TacCodec {
    public static final int MAX_PIXELS = 4_194_304;
    public static final int MAX_DIMENSION = 16_384;
    public interface Progress { void update(int done, int total) throws IOException; }
    public static final Progress QUIET = (done, total) -> {};
    private TacCodec() {}

    static void dimensions(int width, int height) throws IOException {
        if (width <= 0 || height <= 0 || width > MAX_DIMENSION || height > MAX_DIMENSION
                || (long) width * height > MAX_PIXELS)
            throw new IOException("Image exceeds limits: positive dimensions, at most 16,384 per side and 4,194,304 pixels.");
    }
    static void tick(Progress progress, int done, int total) throws IOException {
        if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Conversion cancelled.");
        progress.update(done, total);
    }
    public static BufferedImage readImage(Path path) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(path.toFile())) {
            if (input == null) throw new IOException("Cannot open image.");
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IOException("Unsupported or unreadable image.");
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                dimensions(reader.getWidth(0), reader.getHeight(0));
                BufferedImage image = reader.read(0);
                if (image == null) throw new IOException("Image contains no readable pixels.");
                return image;
            } finally { reader.dispose(); }
        }
    }
    public static String board(int value) {
        char[] cells = new char[9];
        cells[0] = (Integer.bitCount(value & 255) & 1) == 1 ? 'X' : 'O';
        for (int i = 1; i < 9; i++) cells[i] = ((value >>> (8 - i)) & 1) == 1 ? 'X' : 'O';
        return new String(cells);
    }
    static int channel(String board, boolean parity, long number) throws IOException {
        if (board == null || board.length() != 9) throw new IOException("Missing or invalid board " + number + ": expected 9 cells.");
        int value = 0;
        for (int i = 0; i < 9; i++) {
            char cell = board.charAt(i);
            if (cell != 'X' && cell != 'O') throw new IOException("Invalid cell in board " + number + ". Use X or O.");
            if (i > 0) value = (value << 1) | (cell == 'X' ? 1 : 0);
        }
        if (parity && board.charAt(0) != board(value).charAt(0))
            throw new IOException("Parity check failed at board " + number + ". The file may be damaged.");
        if (!parity && board.charAt(0) != 'O') throw new IOException("Legacy board " + number + " must begin with O.");
        return value;
    }
    public static void encode(Path input, Path output, int workers, Progress progress) throws IOException {
        different(input, output);
        encode(readImage(input), output, workers, progress);
    }
    public static void encode(BufferedImage image, Path output, int workers, Progress progress) throws IOException {
        if (workers < 1 || workers > 8) throw new IOException("Choose between 1 and 8 workers.");
        int width = image.getWidth(), height = image.getHeight();
        dimensions(width, height);
        atomic(output, temporary -> {
            ExecutorService pool = Executors.newFixedThreadPool(workers);
            Deque<Future<String>> pending = new ArrayDeque<>();
            int rows = Math.max(1, 8192 / width);
            try (BufferedWriter out = Files.newBufferedWriter(temporary, StandardCharsets.US_ASCII)) {
                out.write("TAC2 " + width + " " + height + " RGBA\n");
                int submitted = 0, completed = 0;
                while (submitted < height || !pending.isEmpty()) {
                    while (submitted < height && pending.size() < workers * 2) {
                        tick(progress, completed, height);
                        int count = Math.min(rows, height - submitted);
                        int[] pixels = image.getRGB(0, submitted, width, count, null, 0, width);
                        pending.add(pool.submit(() -> encodePixels(pixels)));
                        submitted += count;
                    }
                    try { out.write(pending.removeFirst().get()); }
                    catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new InterruptedIOException("Conversion cancelled.");
                    } catch (ExecutionException e) { throw new IOException("Encoding worker failed.", e.getCause()); }
                    completed = Math.min(height, completed + rows);
                    tick(progress, completed, height);
                }
            } finally { pool.shutdownNow(); }
        });
    }
    private static String encodePixels(int[] pixels) throws IOException {
        StringBuilder text = new StringBuilder(pixels.length * 40);
        for (int pixel : pixels) {
            if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Conversion cancelled.");
            text.append(board((pixel >>> 16) & 255)).append('\n');
            text.append(board((pixel >>> 8) & 255)).append('\n');
            text.append(board(pixel & 255)).append('\n');
            text.append(board((pixel >>> 24) & 255)).append('\n');
        }
        return text.toString();
    }
    // Bounded lines stop malformed inputs from allocating arbitrarily large strings.
    private static String line(BufferedReader reader) throws IOException {
        StringBuilder value = new StringBuilder(48);
        int c;
        while ((c = reader.read()) != -1) {
            if (c == '\n') return value.toString();
            if (c == '\r') {
                reader.mark(1);
                if (reader.read() != '\n') reader.reset();
                return value.toString();
            }
            if (value.length() >= 128) throw new IOException("Invalid file: line exceeds 128 characters.");
            value.append((char) c);
        }
        return value.isEmpty() ? null : value.toString();
    }
    public static BufferedImage decode(Path input, Progress progress) throws IOException {
        if (Files.size(input) > (long) MAX_PIXELS * 44 + 128) throw new IOException("TAC file exceeds the supported size.");
        try (BufferedReader reader = Files.newBufferedReader(input, StandardCharsets.US_ASCII)) {
            String header = line(reader);
            if (header == null) throw new IOException("Empty TAC file.");
            String[] fields = header.trim().split("\\s+");
            boolean modern = fields[0].equals("TAC2");
            if ((modern && (fields.length != 4 || !fields[3].equals("RGBA"))) || (!modern && fields.length != 2))
                throw new IOException("Unsupported TAC header. Expected TAC2 <width> <height> RGBA, or legacy <width> <height>.");
            int width, height;
            try {
                width = Integer.parseInt(fields[modern ? 1 : 0]);
                height = Integer.parseInt(fields[modern ? 2 : 1]);
            } catch (NumberFormatException e) { throw new IOException("Invalid image dimensions.", e); }
            dimensions(width, height);
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            long number = 0;
            int[] row = new int[width];
            for (int y = 0; y < height; y++) {
                tick(progress, y, height);
                for (int x = 0; x < width; x++) {
                    int r = channel(line(reader), modern, ++number);
                    int g = channel(line(reader), modern, ++number);
                    int b = channel(line(reader), modern, ++number);
                    int a = modern ? channel(line(reader), true, ++number) : 255;
                    row[x] = (a << 24) | (r << 16) | (g << 8) | b;
                }
                image.setRGB(0, y, width, 1, row, 0, width);
            }
            if (line(reader) != null) throw new IOException("Unexpected data after the last board.");
            tick(progress, height, height);
            return image;
        }
    }
    public static void writePng(BufferedImage image, Path output) throws IOException {
        atomic(output, temporary -> {
            if (!ImageIO.write(image, "png", temporary.toFile())) throw new IOException("PNG writer unavailable.");
        });
    }
    private interface FileAction { void write(Path temporary) throws IOException; }
    private static void atomic(Path output, FileAction action) throws IOException {
        Path target = output.toAbsolutePath().normalize();
        Path temporary = Files.createTempFile(target.getParent(), ".tac-", ".tmp");
        try {
            action.write(temporary);
            if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Conversion cancelled.");
            try { Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }
    public static void different(Path input, Path output) throws IOException {
        if (input.toAbsolutePath().normalize().equals(output.toAbsolutePath().normalize())
                || (Files.exists(output) && Files.isSameFile(input, output)))
            throw new IOException("Input and output must be different files.");
    }
}
