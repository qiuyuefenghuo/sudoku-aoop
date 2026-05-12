package sudoku;

import sudoku.model.SudokuModel;
import sudoku.model.SudokuModelImpl;


import java.util.Scanner;

/**
 * Command-line interface for Sudoku.
 *
 * Uses the SudokuModel interface; SudokuModelImpl is instantiated only in main().
 * No separate View or Controller class — this class combines both roles (NFR3).
 *
 * Commands:
 *   set <row> <col> <digit>   – place a digit (rows/cols are 1-indexed)
 *   erase <row> <col>         – clear a cell
 *   undo                      – undo last move
 *   hint                      – reveal one correct value
 *   reset                     – restore initial puzzle
 *   new                       – load new puzzle
 *   show                      – print the board
 *   help                      – list commands
 *   quit                      – exit
 */
public class SudokuCLI {

    private final SudokuModel model;
    private final Scanner     in;

    public SudokuCLI(SudokuModel model) {
        this.model = model;
        this.in    = new Scanner(System.in);
    }

    public void run() {
        System.out.println("=== Sudoku CLI ===");
        printHelp();
        printBoard();

        while (in.hasNextLine()) {
            String line = in.nextLine().trim();
            if (line.isEmpty()) continue;

            String[] parts = line.split("\\s+");
            String   cmd   = parts[0].toLowerCase();

            switch (cmd) {
                case "set":
                    cmdSet(parts);
                    break;
                case "erase":
                    cmdErase(parts);
                    break;
                case "undo":
                    model.undo();
                    printBoard();
                    break;
                case "hint":
                    model.hint();
                    printBoard();
                    break;
                case "reset":
                    model.reset();
                    System.out.println("Puzzle reset.");
                    printBoard();
                    break;
                case "new":
                    model.newGame();
                    System.out.println("New game loaded.");
                    printBoard();
                    break;
                case "show":
                    printBoard();
                    break;
                case "help":
                    printHelp();
                    break;
                case "quit":
                case "exit":
                    System.out.println("Goodbye!");
                    return;
                default:
                    System.out.println("Unknown command. Type 'help' for a list.");
            }
        }
    }

    private void cmdSet(String[] parts) {
        if (parts.length < 4) {
            System.out.println("Usage: set <row> <col> <digit>  (1-indexed)");
            return;
        }
        try {
            int row   = Integer.parseInt(parts[1]) - 1;
            int col   = Integer.parseInt(parts[2]) - 1;
            int digit = Integer.parseInt(parts[3]);

            if (row < 0 || row > 8 || col < 0 || col > 8) {
                System.out.println("Row and column must be 1-9.");
                return;
            }
            if (digit < 1 || digit > 9) {
                System.out.println("Digit must be 1-9.");
                return;
            }
            if (model.isGiven(row, col)) {
                System.out.println("That cell is pre-filled and cannot be changed.");
                return;
            }

            model.setCell(row, col, digit);
            printBoard();

            if (model.isComplete()) {
                System.out.println("*** Congratulations! Puzzle solved! ***");
            } else if (model.isValidationFeedbackEnabled() && model.isInvalid(row, col)) {
                System.out.printf("Warning: (%d,%d) conflicts with another cell.%n",
                        row + 1, col + 1);
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid number. Usage: set <row> <col> <digit>");
        }
    }

    private void cmdErase(String[] parts) {
        if (parts.length < 3) {
            System.out.println("Usage: erase <row> <col>  (1-indexed)");
            return;
        }
        try {
            int row = Integer.parseInt(parts[1]) - 1;
            int col = Integer.parseInt(parts[2]) - 1;

            if (row < 0 || row > 8 || col < 0 || col > 8) {
                System.out.println("Row and column must be 1-9.");
                return;
            }
            if (model.isGiven(row, col)) {
                System.out.println("That cell is pre-filled and cannot be erased.");
                return;
            }

            model.eraseCell(row, col);
            printBoard();
        } catch (NumberFormatException e) {
            System.out.println("Invalid number. Usage: erase <row> <col>");
        }
    }

    // ── Board rendering ───────────────────────────────────────────────────────

    private void printBoard() {
        System.out.println();
        System.out.println("    1 2 3   4 5 6   7 8 9");
        System.out.println("  +-------+-------+-------+");
        for (int r = 0; r < 9; r++) {
            StringBuilder sb = new StringBuilder();
            sb.append(r + 1).append(" | ");
            for (int c = 0; c < 9; c++) {
                int val = model.getCell(r, c);
                if (val == 0) {
                    sb.append('.');
                } else if (model.isGiven(r, c)) {
                    sb.append((char)('0' + val));
                } else {
                    // User-entered: surround with brackets for visibility
                    sb.append((char)('0' + val));
                }
                // Mark invalid cells
                if (model.isValidationFeedbackEnabled() && model.isInvalid(r, c)) {
                    sb.append('!');
                } else {
                    sb.append(' ');
                }
                if (c == 2 || c == 5) sb.append("| ");
            }
            sb.append("|");
            System.out.println(sb);
            if (r == 2 || r == 5) {
                System.out.println("  +-------+-------+-------+");
            }
        }
        System.out.println("  +-------+-------+-------+");
        System.out.println("  (! = conflict)");
        System.out.println();
    }

    private void printHelp() {
        System.out.println();
        System.out.println("Commands (row/col are 1-9):");
        System.out.println("  set <row> <col> <digit>   – place digit");
        System.out.println("  erase <row> <col>         – clear cell");
        System.out.println("  undo                      – undo last move");
        System.out.println("  hint                      – reveal a correct value");
        System.out.println("  reset                     – restart current puzzle");
        System.out.println("  new                       – load a new puzzle");
        System.out.println("  show                      – redisplay the board");
        System.out.println("  help                      – this help");
        System.out.println("  quit                      – exit");
        System.out.println();
    }

    // ── Entry point ───────────────────────────────────────────────────────────

    public static void main(String[] args) {
        SudokuModel model = new SudokuModelImpl();
        new SudokuCLI(model).run();
    }
}
