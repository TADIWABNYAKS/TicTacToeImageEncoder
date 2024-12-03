# Image to Tic-Tac-Toe Encoder/Decoder

This Java program encodes images into a custom `.tac` file format using Tic-Tac-Toe boards and decodes them back to images. Each pixel's color channels (Red, Green, Blue) are represented by separate Tic-Tac-Toe boards.

---

## Usage

The program accepts three command-line arguments:

1. **Input File Path**: The path to the input file (PNG for encoding, `.tac` for decoding).
2. **Output File Path**: The path where the output file will be saved (`.tac` for encoding, PNG for decoding).
3. **Mode**: Specifies the operation mode - `E` for encoding or `D` for decoding.

---

## Command Syntax

```sh
java ImageToTicTacToeEncoder <inputFile> <outputFile> <mode>
```

