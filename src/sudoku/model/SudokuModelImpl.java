package sudoku.model;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Concrete implementation of SudokuModel.
 *
 * ── Class invariants ──────────────────────────────────────────────────────────
 *
 * @invariant board != null && board.length == 9
 * @invariant (\forall int i; 0 <= i && i < 9; board[i].length == 9)
 * @invariant (\forall int i; 0 <= i && i < 9;
 *               (\forall int j; 0 <= j && j < 9;
 *                  0 <= board[i][j] && board[i][j] <= 9))
 * @invariant given != null && given.length == 9
 * @invariant (\forall int i; 0 <= i && i < 9; given[i].length == 9)
 * @invariant (\forall int i; 0 <= i && i < 9;
 *               (\forall int j; 0 <= j && j < 9;
 *                  given[i][j] ==> board[i][j] != 0))
 * @invariant (\forall int i; 0 <= i && i < 9;
 *               (\forall int j; 0 <= j && j < 9;
 *                  given[i][j] ==> board[i][j] == solution[i][j]))
 * @invariant !puzzleLines.isEmpty()
 * @invariant 0 <= puzzleIndex && puzzleIndex < puzzleLines.size()
 */
@SuppressWarnings("deprecation")
public class SudokuModelImpl extends Observable implements SudokuModel {

    // ── Fields ────────────────────────────────────────────────────────────────

    /** Current board values; 0 = empty, 1-9 = placed digit. */
    private final int[][] board = new int[9][9];

    /** True iff the cell was pre-filled (given) in the current puzzle. */
    private final boolean[][] given = new boolean[9][9];

    /** Unique solution of the current puzzle, used for hints. */
    private final int[][] solution = new int[9][9];

    /**
     * Cached invalid flags.
     * invalid[r][c] == true iff board[r][c] shares its digit with another
     * cell in the same row, column, or 3x3 box.
     */
    private final boolean[][] invalid = new boolean[9][9];

    /**
     * Single-level undo record: {row, col, previousValue}, or null if no
     * undoable action exists.
     */
    private int[] undoEntry = null;

    // ── Flags (FR3) ───────────────────────────────────────────────────────────

    private boolean validationFeedbackEnabled = true;
    private boolean hintEnabled               = true;
    private boolean randomPuzzleSelection     = false;

    // ── Puzzle file state ─────────────────────────────────────────────────────

    private final List<String> puzzleLines = new ArrayList<>();
    private int    puzzleIndex = 0;
    private final Random rng   = new Random();

    // ── Constructor ───────────────────────────────────────────────────────────

    public SudokuModelImpl() {
        loadPuzzleFile();
        loadNextPuzzle();
        assert checkInvariant() : "constructor: invariant violated";
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void loadPuzzleFile() {
        InputStream is = getClass().getResourceAsStream("/puzzles.txt");
        if (is == null) {
            try { is = new FileInputStream("puzzles.txt"); }
            catch (FileNotFoundException e) {
                try { is = new FileInputStream("src/puzzles.txt"); }
                catch (FileNotFoundException e2) {
                    System.err.println("Warning: puzzles.txt not found."); }
            }
        }
        if (is != null) {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.length() == 81 && line.matches("[0-9]+"))
                        puzzleLines.add(line);
                }
            } catch (IOException e) {
                System.err.println("Error reading puzzles.txt: " + e.getMessage());
            }
        }
        if (puzzleLines.isEmpty())
            puzzleLines.add(
                "530070000600195000098000060800060003400803001700020006060000280000419005000080079");
    }

    /**
     * Selects the next puzzle (sequential or random per flag),
     * applies it, and notifies observers with "newgame".
     *
     * @requires !puzzleLines.isEmpty()
     */
    private void loadNextPuzzle() {
        assert !puzzleLines.isEmpty() : "loadNextPuzzle: no puzzles available";
        String raw;
        if (randomPuzzleSelection) {
            raw = puzzleLines.get(rng.nextInt(puzzleLines.size()));
        } else {
            raw = puzzleLines.get(puzzleIndex);
            puzzleIndex = (puzzleIndex + 1) % puzzleLines.size();
        }
        applyPuzzleString(raw);
        notifyObs("newgame");
    }

    /**
     * Initialises board/given/solution from an 81-character digit string.
     *
     * @requires s != null && s.length() == 81
     * @requires (\forall int k; 0 <= k && k < 81;
     *               '0' <= s.charAt(k) && s.charAt(k) <= '9')
     * @ensures  (\forall int r; 0 <= r && r < 9;
     *               (\forall int c; 0 <= c && c < 9;
     *                  board[r][c] == (s.charAt(r*9+c) - '0')))
     * @ensures  (\forall int r; 0 <= r && r < 9;
     *               (\forall int c; 0 <= c && c < 9;
     *                  given[r][c] == (board[r][c] != 0)))
     * @ensures  undoEntry == null
     */
    private void applyPuzzleString(String s) {
        assert s != null && s.length() == 81 : "applyPuzzleString: bad string";
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                int d = s.charAt(r * 9 + c) - '0';
                board[r][c] = d;
                given[r][c] = (d != 0);
            }
        }
        undoEntry = null;
        recomputeInvalid();
        solvePuzzle();
        assert undoEntry == null : "applyPuzzleString: undoEntry not cleared";
        assert checkInvariant()  : "applyPuzzleString: invariant violated";
    }

    private void solvePuzzle() {
        for (int r = 0; r < 9; r++) System.arraycopy(board[r], 0, solution[r], 0, 9);
        solve(solution);
    }

    private boolean solve(int[][] b) {
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                if (b[r][c] == 0) {
                    for (int d = 1; d <= 9; d++) {
                        if (canPlace(b, r, c, d)) {
                            b[r][c] = d;
                            if (solve(b)) return true;
                            b[r][c] = 0;
                        }
                    }
                    return false;
                }
            }
        }
        return true;
    }

    private boolean canPlace(int[][] b, int row, int col, int digit) {
        for (int i = 0; i < 9; i++) {
            if (b[row][i] == digit || b[i][col] == digit) return false;
        }
        int br = (row / 3) * 3, bc = (col / 3) * 3;
        for (int r = br; r < br + 3; r++)
            for (int c = bc; c < bc + 3; c++)
                if (b[r][c] == digit) return false;
        return true;
    }

    /**
     * Recomputes invalid[][] from the current board state.
     * A cell is marked invalid iff its non-zero digit appears in the same
     * row, column, or 3x3 box as another non-zero cell.
     *
     * @ensures (\forall int r; 0 <= r && r < 9;
     *             (\forall int c; 0 <= c && c < 9;
     *                invalid[r][c] ==> board[r][c] != 0))
     */
    private void recomputeInvalid() {
        for (boolean[] row : invalid) Arrays.fill(row, false);

        // rows
        for (int r = 0; r < 9; r++) {
            for (int c1 = 0; c1 < 9; c1++) {
                if (board[r][c1] == 0) continue;
                for (int c2 = c1 + 1; c2 < 9; c2++) {
                    if (board[r][c1] == board[r][c2]) {
                        invalid[r][c1] = invalid[r][c2] = true;
                    }
                }
            }
        }
        // columns
        for (int c = 0; c < 9; c++) {
            for (int r1 = 0; r1 < 9; r1++) {
                if (board[r1][c] == 0) continue;
                for (int r2 = r1 + 1; r2 < 9; r2++) {
                    if (board[r1][c] == board[r2][c]) {
                        invalid[r1][c] = invalid[r2][c] = true;
                    }
                }
            }
        }
        // 3x3 boxes
        for (int br = 0; br < 9; br += 3) {
            for (int bc = 0; bc < 9; bc += 3) {
                List<int[]> cells = new ArrayList<>(9);
                for (int r = br; r < br + 3; r++)
                    for (int c = bc; c < bc + 3; c++)
                        cells.add(new int[]{r, c});
                for (int i = 0; i < cells.size(); i++) {
                    int[] p1 = cells.get(i);
                    if (board[p1[0]][p1[1]] == 0) continue;
                    for (int j = i + 1; j < cells.size(); j++) {
                        int[] p2 = cells.get(j);
                        if (board[p1[0]][p1[1]] == board[p2[0]][p2[1]])
                            invalid[p1[0]][p1[1]] = invalid[p2[0]][p2[1]] = true;
                    }
                }
            }
        }
        // postcondition: no empty cell is flagged
        for (int r = 0; r < 9; r++)
            for (int c = 0; c < 9; c++)
                assert !invalid[r][c] || board[r][c] != 0
                    : "recomputeInvalid: empty cell flagged at (" + r + "," + c + ")";
    }

    private void notifyObs(Object arg) { setChanged(); notifyObservers(arg); }

    /**
     * Checks all class invariant clauses at runtime.
     * @ensures \result == true iff every invariant clause holds
     */
    private boolean checkInvariant() {
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                if (board[r][c] < 0 || board[r][c] > 9)          return false;
                if (given[r][c] && board[r][c] == 0)              return false;
                if (given[r][c] && board[r][c] != solution[r][c]) return false;
            }
        }
        return !puzzleLines.isEmpty();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  SudokuModel public interface
    // ═════════════════════════════════════════════════════════════════════════

    // ── Board queries ─────────────────────────────────────────────────────────

    /**
     * @requires 0 <= row && row < 9 && 0 <= col && col < 9
     * @ensures  \result == board[row][col]
     * @ensures  0 <= \result && \result <= 9
     */
    @Override
    public int getCell(int row, int col) {
        assert row >= 0 && row < 9 && col >= 0 && col < 9
            : "getCell: precondition 0<=row<9, 0<=col<9 violated";
        int result = board[row][col];
        assert result >= 0 && result <= 9
            : "getCell: postcondition 0<=result<=9 violated";
        return result;
    }

    /**
     * @requires 0 <= row && row < 9 && 0 <= col && col < 9
     * @ensures  \result == given[row][col]
     * @ensures  \result ==> board[row][col] != 0
     */
    @Override
    public boolean isGiven(int row, int col) {
        assert row >= 0 && row < 9 && col >= 0 && col < 9
            : "isGiven: precondition 0<=row<9, 0<=col<9 violated";
        boolean result = given[row][col];
        assert !result || board[row][col] != 0
            : "isGiven: postcondition (given ==> non-zero) violated";
        return result;
    }

    /**
     * @requires 0 <= row && row < 9 && 0 <= col && col < 9
     * @ensures  \result == invalid[row][col]
     * @ensures  \result ==> board[row][col] != 0
     */
    @Override
    public boolean isInvalid(int row, int col) {
        assert row >= 0 && row < 9 && col >= 0 && col < 9
            : "isInvalid: precondition 0<=row<9, 0<=col<9 violated";
        boolean result = invalid[row][col];
        assert !result || board[row][col] != 0
            : "isInvalid: postcondition (invalid ==> non-zero) violated";
        return result;
    }

    // ── Board mutations ───────────────────────────────────────────────────────

    /**
     * Places digit in the editable cell (row, col).
     *
     * @requires 0 <= row && row < 9
     * @requires 0 <= col && col < 9
     * @requires 1 <= digit && digit <= 9
     * @requires !isGiven(row, col)
     * @ensures  isGiven(row, col) ==>
     *              getCell(row, col) == \old(getCell(row, col))
     * @ensures  !isGiven(row, col) ==> getCell(row, col) == digit
     * @ensures  !isGiven(row, col) ==>
     *              undoEntry[0] == row && undoEntry[1] == col
     *              && undoEntry[2] == \old(getCell(row, col))
     * @ensures  checkInvariant()
     */
    @Override
    public void setCell(int row, int col, int digit) {
        assert row >= 0 && row < 9 && col >= 0 && col < 9
            : "setCell: precondition 0<=row<9, 0<=col<9 violated";
        assert digit >= 1 && digit <= 9
            : "setCell: precondition 1<=digit<=9 violated, got " + digit;

        if (given[row][col] || digit < 1 || digit > 9) return;

        int prevValue = board[row][col];
        undoEntry = new int[]{row, col, prevValue};
        board[row][col] = digit;

        if (validationFeedbackEnabled) recomputeInvalid();

        assert board[row][col] == digit
            : "setCell: postcondition board[row][col]==digit violated";
        assert undoEntry[0] == row && undoEntry[1] == col && undoEntry[2] == prevValue
            : "setCell: postcondition undoEntry incorrect";
        assert checkInvariant() : "setCell: invariant violated";

        if (isComplete()) notifyObs("complete");
        else              notifyObs("update");
    }

    /**
     * Clears an editable cell (sets it to 0).
     *
     * @requires 0 <= row && row < 9
     * @requires 0 <= col && col < 9
     * @requires !isGiven(row, col)
     * @ensures  isGiven(row, col) ==>
     *              getCell(row, col) == \old(getCell(row, col))
     * @ensures  !isGiven(row, col) ==> getCell(row, col) == 0
     * @ensures  !isGiven(row, col) ==>
     *              undoEntry[0] == row && undoEntry[1] == col
     *              && undoEntry[2] == \old(getCell(row, col))
     * @ensures  checkInvariant()
     */
    @Override
    public void eraseCell(int row, int col) {
        assert row >= 0 && row < 9 && col >= 0 && col < 9
            : "eraseCell: precondition 0<=row<9, 0<=col<9 violated";

        if (given[row][col]) return;

        int prevValue = board[row][col];
        undoEntry = new int[]{row, col, prevValue};
        board[row][col] = 0;

        if (validationFeedbackEnabled) recomputeInvalid();

        assert board[row][col] == 0
            : "eraseCell: postcondition board[row][col]==0 violated";
        assert undoEntry[0] == row && undoEntry[1] == col && undoEntry[2] == prevValue
            : "eraseCell: postcondition undoEntry incorrect";
        assert checkInvariant() : "eraseCell: invariant violated";

        notifyObs("update");
    }

    /**
     * Reverts the most recent user action (single-level undo).
     *
     * @requires undoEntry != null  (no-op otherwise)
     * @requires !isGiven(undoEntry[0], undoEntry[1])
     * @ensures  undoEntry == null
     * @ensures  getCell(\old(undoEntry[0]), \old(undoEntry[1]))
     *              == \old(undoEntry[2])
     * @ensures  checkInvariant()
     */
    @Override
    public void undo() {
        if (undoEntry == null) return;

        int row  = undoEntry[0];
        int col  = undoEntry[1];
        int prev = undoEntry[2];

        if (given[row][col]) { undoEntry = null; return; }

        board[row][col] = prev;
        undoEntry = null;

        if (validationFeedbackEnabled) recomputeInvalid();

        assert undoEntry == null
            : "undo: postcondition undoEntry==null violated";
        assert board[row][col] == prev
            : "undo: postcondition getCell(row,col)==prev violated";
        assert checkInvariant() : "undo: invariant violated";

        notifyObs("update");
    }

    /**
     * Reveals the correct value for one randomly chosen empty editable cell.
     *
     * @requires hintEnabled == true
     * @requires (\exists int r, c;
     *               0 <= r && r < 9 && 0 <= c && c < 9;
     *               !isGiven(r,c) && getCell(r,c) == 0)
     * @ensures  hintEnabled ==>
     *              (\exists int r, c;
     *                 0 <= r && r < 9 && 0 <= c && c < 9
     *                 && \old(getCell(r,c)) == 0 && !isGiven(r,c)
     *                 && getCell(r,c) == solution[r][c])
     * @ensures  checkInvariant()
     */
    @Override
    public void hint() {
        if (!hintEnabled) return;

        List<int[]> empty = new ArrayList<>();
        for (int r = 0; r < 9; r++)
            for (int c = 0; c < 9; c++)
                if (!given[r][c] && board[r][c] == 0)
                    empty.add(new int[]{r, c});

        if (empty.isEmpty()) return;

        int[] cell = empty.get(rng.nextInt(empty.size()));
        int row = cell[0], col = cell[1];
        int prevValue = board[row][col];   // == 0

        undoEntry = new int[]{row, col, prevValue};
        board[row][col] = solution[row][col];

        if (validationFeedbackEnabled) recomputeInvalid();

        assert board[row][col] == solution[row][col]
            : "hint: postcondition cell not filled with solution value";
        assert !given[row][col]
            : "hint: postcondition hinted cell must not be given";
        assert checkInvariant() : "hint: invariant violated";

        if (isComplete()) notifyObs("complete");
        else              notifyObs("update");
    }

    /**
     * Restores all editable cells to 0 (initial puzzle state).
     *
     * @ensures (\forall int r; 0 <= r && r < 9;
     *             (\forall int c; 0 <= c && c < 9;
     *                !isGiven(r,c) ==> getCell(r,c) == 0))
     * @ensures (\forall int r; 0 <= r && r < 9;
     *             (\forall int c; 0 <= c && c < 9;
     *                isGiven(r,c) ==>
     *                   getCell(r,c) == \old(getCell(r,c))))
     * @ensures undoEntry == null
     * @ensures checkInvariant()
     */
    @Override
    public void reset() {
        for (int r = 0; r < 9; r++)
            for (int c = 0; c < 9; c++)
                if (!given[r][c]) board[r][c] = 0;

        undoEntry = null;
        recomputeInvalid();

        for (int r = 0; r < 9; r++)
            for (int c = 0; c < 9; c++)
                assert given[r][c] || board[r][c] == 0
                    : "reset: postcondition !isGiven ==> cell==0 violated at ("
                      + r + "," + c + ")";
        assert undoEntry == null : "reset: postcondition undoEntry==null violated";
        assert checkInvariant() : "reset: invariant violated";

        notifyObs("reset");
    }

    /**
     * Loads the next puzzle from puzzles.txt according to the Puzzle Selection Flag.
     *
     * @requires !puzzleLines.isEmpty()
     * @ensures  undoEntry == null
     * @ensures  (\forall int r; 0 <= r && r < 9;
     *               (\forall int c; 0 <= c && c < 9;
     *                  isGiven(r,c) ==> getCell(r,c) != 0))
     * @ensures  checkInvariant()
     */
    @Override
    public void newGame() {
        assert !puzzleLines.isEmpty() : "newGame: no puzzles available";
        loadNextPuzzle();
        assert undoEntry == null : "newGame: postcondition undoEntry==null violated";
        assert checkInvariant()  : "newGame: invariant violated";
    }

    /**
     * Returns true iff every cell is filled AND the board has no duplicates.
     *
     * @ensures \result ==
     *   (\forall int r; 0<=r<9;
     *      (\forall int c; 0<=c<9; board[r][c] != 0))
     *   && isValid()
     */
    @Override
    public boolean isComplete() {
        for (int r = 0; r < 9; r++)
            for (int c = 0; c < 9; c++)
                if (board[r][c] == 0) return false;
        return isValid();
    }

    /**
     * Returns true iff no non-zero digit is repeated in any row, column, or box.
     *
     * @ensures \result ==
     *   (\forall int r; 0<=r<9;
     *      (\forall int c1, c2; 0<=c1 && c1<c2 && c2<9 && board[r][c1]!=0;
     *         board[r][c1] != board[r][c2]))
     *   && (\forall int c; 0<=c<9;
     *         (\forall int r1, r2; 0<=r1 && r1<r2 && r2<9 && board[r1][c]!=0;
     *            board[r1][c] != board[r2][c]))
     *   && (no 3x3 box contains two equal non-zero digits)
     */
    @Override
    public boolean isValid() {
        int[][] b = board;
        for (int r = 0; r < 9; r++) {
            Set<Integer> seen = new HashSet<>();
            for (int c = 0; c < 9; c++)
                if (b[r][c] != 0 && !seen.add(b[r][c])) return false;
        }
        for (int c = 0; c < 9; c++) {
            Set<Integer> seen = new HashSet<>();
            for (int r = 0; r < 9; r++)
                if (b[r][c] != 0 && !seen.add(b[r][c])) return false;
        }
        for (int br = 0; br < 9; br += 3) {
            for (int bc = 0; bc < 9; bc += 3) {
                Set<Integer> seen = new HashSet<>();
                for (int r = br; r < br+3; r++)
                    for (int c = bc; c < bc+3; c++)
                        if (b[r][c] != 0 && !seen.add(b[r][c])) return false;
            }
        }
        return true;
    }

    // ── Flags ─────────────────────────────────────────────────────────────────

    /** @ensures \result == validationFeedbackEnabled */
    @Override public boolean isValidationFeedbackEnabled() { return validationFeedbackEnabled; }

    /**
     * @requires true
     * @ensures  validationFeedbackEnabled == enabled
     * @ensures  checkInvariant()
     */
    @Override public void setValidationFeedbackEnabled(boolean enabled) {
        validationFeedbackEnabled = enabled;
        recomputeInvalid();
        assert checkInvariant() : "setValidationFeedbackEnabled: invariant violated";
        notifyObs("flagchange");
    }

    /** @ensures \result == hintEnabled */
    @Override public boolean isHintEnabled() { return hintEnabled; }

    /**
     * @requires true
     * @ensures  hintEnabled == enabled
     * @ensures  checkInvariant()
     */
    @Override public void setHintEnabled(boolean enabled) {
        hintEnabled = enabled;
        assert checkInvariant() : "setHintEnabled: invariant violated";
        notifyObs("flagchange");
    }

    /** @ensures \result == randomPuzzleSelection */
    @Override public boolean isRandomPuzzleSelection() { return randomPuzzleSelection; }

    /**
     * @requires true
     * @ensures  randomPuzzleSelection == random
     * @ensures  checkInvariant()
     */
    @Override public void setRandomPuzzleSelection(boolean random) {
        randomPuzzleSelection = random;
        assert checkInvariant() : "setRandomPuzzleSelection: invariant violated";
        notifyObs("flagchange");
    }

    // ── Test support ──────────────────────────────────────────────────────────

    /**
     * Loads a puzzle from an 81-character string. For unit tests only.
     *
     * @requires s != null
     * @requires s.length() == 81
     * @requires (\forall int k; 0 <= k && k < 81;
     *               '0' <= s.charAt(k) && s.charAt(k) <= '9')
     * @ensures  (\forall int r; 0 <= r && r < 9;
     *               (\forall int c; 0 <= c && c < 9;
     *                  getCell(r,c) == (s.charAt(r*9+c) - '0')))
     * @ensures  (\forall int r; 0 <= r && r < 9;
     *               (\forall int c; 0 <= c && c < 9;
     *                  isGiven(r,c) == (getCell(r,c) != 0)))
     * @ensures  undoEntry == null
     * @ensures  checkInvariant()
     */
    @Override
    public void loadPuzzleFromString(String s) {
        assert s != null
            : "loadPuzzleFromString: precondition s!=null violated";
        assert s.length() == 81
            : "loadPuzzleFromString: precondition s.length()==81 violated";
        assert s.matches("[0-9]+")
            : "loadPuzzleFromString: precondition all-digits violated";

        applyPuzzleString(s);

        // postcondition asserts
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                int expected = s.charAt(r * 9 + c) - '0';
                assert board[r][c] == expected
                    : "loadPuzzleFromString: board mismatch at (" + r + "," + c + ")";
                assert given[r][c] == (expected != 0)
                    : "loadPuzzleFromString: given flag mismatch at (" + r + "," + c + ")";
            }
        }
        assert undoEntry == null : "loadPuzzleFromString: undoEntry not null";
        assert checkInvariant()  : "loadPuzzleFromString: invariant violated";
    }
}
