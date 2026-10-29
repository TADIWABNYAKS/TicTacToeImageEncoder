import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;

/** CLI and backwards-compatible entry points for the original university project. */
public final class ImageEncoder {
    private ImageEncoder() {}
    public static void encodeImageToTicTacToe(String input, String output) throws IOException {
        TacCodec.encode(Path.of(input), Path.of(output), 1, TacCodec.QUIET);
    }
    public static void decodeTicTacToeToImage(String input, String output) throws IOException {
        TacCodec.different(Path.of(input), Path.of(output));
        TacCodec.writePng(TacCodec.decode(Path.of(input), TacCodec.QUIET), Path.of(output));
    }
    public static void main(String[] args) {
        int status = run(args, System.out, System.err);
        if (status != 0) System.exit(status);
    }
    static int run(String[] args, PrintStream out, PrintStream err) {
        String usage = "Usage: ImageEncoder <input> <output> E [workers 1-8] | <input> <output> D";
        if (args.length == 0 || (args.length == 1 && (args[0].equals("--help") || args[0].equals("-h")))) {
            out.println(usage);
            return 0;
        }
        try {
            if (args.length < 3 || args.length > 4) throw new IOException(usage);
            if (args[2].equalsIgnoreCase("E")) {
                int workers = args.length == 4 ? Integer.parseInt(args[3]) : 1;
                TacCodec.encode(Path.of(args[0]), Path.of(args[1]), workers, TacCodec.QUIET);
            } else if (args[2].equalsIgnoreCase("D") && args.length == 3) {
                decodeTicTacToeToImage(args[0], args[1]);
            } else throw new IOException("Mode must be E (encode) or D (decode).");
            out.println("Saved " + args[1]);
            return 0;
        } catch (IOException | IllegalArgumentException e) {
            err.println("Conversion failed: " + e.getMessage());
            return 1;
        }
    }
}
