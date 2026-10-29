# Image to Tic-Tac-Toe Encoder/Decoder

A Java command-line program that converts images into X/O boards stored in `.tac` files and decodes them back into PNGs.

## Compile and run

Requires JDK 17 or newer. Run these commands from the project folder:

```sh
javac -d . src/ImageEncoder.java src/TacCodec.java
```

`javac` compiles the source files. The `-d .` puts the compiled classes in the current folder, so you can run the program with `java ImageEncoder`.

```sh
# Encode an image
java ImageEncoder examples/original.png image.tac E

# Decode it back into a PNG
java ImageEncoder image.tac decoded.png D

# Optional: encode with four workers (1–8 supported)
java ImageEncoder examples/original.png image.tac E 4
```

You only need to compile again after changing the source code. Running `java ImageEncoder` without arguments prints usage. Output files are replaced if they exist; input and output must be different files.

## How it works

Each pixel becomes four boards: **red, green, blue, and alpha**. Every board is a line of nine `X`/`O` characters. `X` means 1 and `O` means 0.

```text
parity  bit7  bit6
bit5    bit4  bit3
bit2    bit1  bit0
```

Eight cells store a channel value from 0 to 255. The first cell provides even parity to detect single-cell damage. For example, 128 becomes `XXOOOOOOO`.

New files start with `TAC2 <width> <height> RGBA`, followed by four boards per pixel, left to right and top to bottom.

The decoder also reads the original `<width> <height>` format with three RGB boards per pixel and a fixed `O` in the first cell. Those files decode as opaque. New exports preserve transparency; the original decoder cannot read them.

## Example

| Original image | → X/O boards (`.tac`) → | Decoded image |
| :---: | :---: | :---: |
| <img src="examples/original.png" alt="Original image" width="260"> | <code>TAC2 857 1200 RGBA<br>XOOXXOOXO<br>XOOXOOOOO<br>OOOOXOOXO<br>OXXXXXXXX<br>…</code> | <img src="examples/decoded.png" alt="Decoded image" width="260"> |
| [original.png](examples/original.png) | [image.tac](examples/image.tac) | [decoded.png](examples/decoded.png) |

To reproduce the conversion:

```sh
java ImageEncoder <inputFile> <outputFile> <mode>
```
