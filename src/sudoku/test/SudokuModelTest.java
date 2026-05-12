package sudoku.test;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import sudoku.model.SudokuModel;
import sudoku.model.SudokuModelImpl;

/**
 * JUnit 4 unit tests for SudokuModelImpl.
 *
 * Each test documents its scenario using JML-style specification,
 * sets the model into a state that satisfies the method's preconditions,
 * invokes the method under test, then uses JUnit assertion methods to
 * verify that the postconditions hold.
 *
 * ── Shared class-level invariant (must hold between every test) ───────────
 *
 * @invariant model != null
 * @invariant (\forall int r; 0<=r<9;
 *               (\forall int c; 0<=c<9;
 *                  0 <= model.getCell(r,c) && model.getCell(r,c) <= 9))
 * @invariant (\forall int r; 0<=r<9;
 *               (\forall int c; 0<=c<9;
 *                  model.isGiven(r,c) ==> model.getCell(r,c) != 0))
 */
public class SudokuModelTest {

    private SudokuModel model;

    /**
     * Classic "easy" Sudoku puzzle used across all three tests.
     * Non-zero digits are given cells; zeros are empty.
     * Row 0: "530070000"  – givens at cols 0(5), 1(3), 3(0->skip), …
     *   actually:  5 3 0 0 7 0 0 0 0
     * Row 1: 6 0 0 1 9 5 0 0 0
     * Row 2: 0 9 8 0 0 0 0 6 0
     * Row 3: 8 0 0 0 6 0 0 0 3
     * Row 4: 4 0 0 8 0 3 0 0 1
     * Row 5: 7 0 0 0 2 0 0 0 6
     * Row 6: 0 6 0 0 0 0 2 8 0
     * Row 7: 0 0 0 4 1 9 0 0 5
     * Row 8: 0 0 0 0 8 0 0 7 9
     */
    private static final String PUZZLE =
        "530070000" +
        "600195000" +
        "098000060" +
        "800060003" +
        "400803001" +
        "700020006" +
        "060000280" +
        "000419005" +
        "000080079";

    /**
     * Known solution for PUZZLE (computed once; verified by test 1).
     * Used as ground-truth for tests 1 and 3.
     */
    private static final int[][] SOLUTION = {
        {5,3,4,6,7,8,9,1,2},
        {6,7,2,1,9,5,3,4,8},
        {1,9,8,3,4,2,5,6,7},
        {8,5,9,7,6,1,4,2,3},
        {4,2,6,8,5,3,7,9,1},
        {7,1,3,9,2,4,8,5,6},
        {9,6,1,5,3,7,2,8,4},
        {2,8,7,4,1,9,6,3,5},
        {3,4,5,2,8,6,1,7,9}
    };

    @Before
    public void setUp() {
        model = new SudokuModelImpl();
        // Load the shared puzzle so every test starts from the same state
        model.loadPuzzleFromString(PUZZLE);
        model.setValidationFeedbackEnabled(true);
        model.setHintEnabled(true);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Scenario 1 – Filling all empty cells with correct values completes the puzzle
    // ──────────────────────────────────────────────────────────────────────────
    /**
     * @scenario Place every correct digit into every empty editable cell.
     *           After the last setCell call the board must be complete and valid.
     *
     * @invariant model satisfies class invariant throughout
     *
     * @precondition model is loaded with PUZZLE
     * @precondition (\forall r,c: !model.isGiven(r,c) ==> model.getCell(r,c)==0)
     * @precondition SOLUTION[r][c] is the unique correct digit for each (r,c)
     *
     * @postcondition model.isComplete() == true
     * @postcondition model.isValid()    == true
     * @postcondition (\forall int r; 0<=r<9;
     *                   (\forall int c; 0<=c<9;
     *                      model.getCell(r,c) == SOLUTION[r][c]))
     */
    @Test
    public void testCompletionAfterFillingAllCells() {

        // Precondition verification: every non-given cell starts empty
        for (int r = 0; r < 9; r++)
            for (int c = 0; c < 9; c++)
                if (!model.isGiven(r, c))
                    assertEquals("Precondition: empty cell (" + r + "," + c + ") should be 0",
                                 0, model.getCell(r, c));

        // Action: fill every empty cell with the correct solution digit
        for (int r = 0; r < 9; r++)
            for (int c = 0; c < 9; c++)
                if (!model.isGiven(r, c))
                    model.setCell(r, c, SOLUTION[r][c]);

        // Postcondition 1: board is complete
        assertTrue("isComplete() must be true after filling all cells correctly",
                   model.isComplete());

        // Postcondition 2: board is valid (no duplicates)
        assertTrue("isValid() must be true when all cells match the unique solution",
                   model.isValid());

        // Postcondition 3: every cell matches the solution
        for (int r = 0; r < 9; r++)
            for (int c = 0; c < 9; c++)
                assertEquals("Cell (" + r + "," + c + ") must equal SOLUTION[r][c]",
                             SOLUTION[r][c], model.getCell(r, c));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Scenario 2 – Placing a duplicate digit triggers the invalid flag
    // ──────────────────────────────────────────────────────────────────────────
    /**
     * @scenario With validation feedback enabled, placing a digit that already
     *           exists as a given in the same row causes both the new cell and
     *           the conflicting given to be flagged as invalid.
     *
     * @invariant model satisfies class invariant throughout
     * @invariant validationFeedbackEnabled == true
     *
     * @precondition model.isGiven(0, 0) == true  &&  model.getCell(0, 0) == 5
     * @precondition model.isGiven(0, 7) == false  &&  model.getCell(0, 7) == 0
     * @precondition model.isValidationFeedbackEnabled() == true
     * @precondition model.isInvalid(0, 7) == false  (no conflict before action)
     *
     * @postcondition model.getCell(0, 7) == 5
     * @postcondition model.isInvalid(0, 7) == true
     *                  (duplicate 5 in row 0)
     * @postcondition model.isInvalid(0, 0) == true
     *                  (original 5 also flagged as duplicate)
     * @postcondition model.isComplete() == false
     *                  (board is not fully filled)
     */
    @Test
    public void testValidationFlagsConflictingCell() {

        // Precondition verification
        assertEquals("Precondition: given at (0,0) must be 5",
                     5, model.getCell(0, 0));
        assertTrue("Precondition: (0,0) must be given",
                   model.isGiven(0, 0));
        assertFalse("Precondition: (0,7) must not be given",
                    model.isGiven(0, 7));
        assertEquals("Precondition: (0,7) must be empty",
                     0, model.getCell(0, 7));
        assertTrue("Precondition: validation feedback must be enabled",
                   model.isValidationFeedbackEnabled());
        assertFalse("Precondition: (0,7) must not be invalid before action",
                    model.isInvalid(0, 7));

        // Action: place a conflicting duplicate of 5 in row 0
        model.setCell(0, 7, 5);

        // Postcondition 1: the new cell now holds the placed digit
        assertEquals("Postcondition: getCell(0,7) must be 5",
                     5, model.getCell(0, 7));

        // Postcondition 2: the new cell is flagged invalid (duplicate in row 0)
        assertTrue("Postcondition: isInvalid(0,7) must be true (duplicate 5 in row)",
                   model.isInvalid(0, 7));

        // Postcondition 3: the original given 5 at (0,0) is also flagged
        assertTrue("Postcondition: isInvalid(0,0) must be true (also a duplicate 5)",
                   model.isInvalid(0, 0));

        // Postcondition 4: incomplete board is not marked complete
        assertFalse("Postcondition: isComplete() must be false (board not fully filled)",
                    model.isComplete());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Scenario 3 – Undo restores the previous cell value and clears invalid flag
    // ──────────────────────────────────────────────────────────────────────────
    /**
     * @scenario After placing a wrong digit in an empty editable cell,
     *           calling undo() reverts that cell to its previous value (0)
     *           and clears the invalid flag.
     *
     * @invariant model satisfies class invariant throughout
     *
     * @precondition model.isGiven(2, 0) == false  &&  model.getCell(2, 0) == 0
     *   (row 2 = "098000060": col 0 is '0', i.e. empty and editable)
     * @precondition model.isValidationFeedbackEnabled() == true
     *
     * Let prevValue = model.getCell(2, 0)   =>  prevValue == 0
     *
     * After model.setCell(2, 0, 5):
     * @postcondition (intermediate) model.getCell(2, 0) == 5
     * @postcondition (intermediate) model.isInvalid(2, 0) == true
     *   because row 2 already has given 9 and 8; however, column 0 already has
     *   givens 8, 4, 7 and no 5 — the duplicate arises in the 3x3 box
     *   (top-left box contains given 5 at (0,0)); therefore the cell IS invalid.
     *
     * After model.undo():
     * @postcondition model.getCell(2, 0) == prevValue  (== 0)
     * @postcondition model.isInvalid(2, 0) == false
     * @postcondition model.isComplete() == false
     */
    @Test
    public void testUndoRestoresPreviousValueAndClearsFlag() {

        final int ROW = 2, COL = 0;

        // Precondition verification
        assertFalse("Precondition: (2,0) must not be given",
                    model.isGiven(ROW, COL));
        assertEquals("Precondition: (2,0) must be empty (0)",
                     0, model.getCell(ROW, COL));

        int prevValue = model.getCell(ROW, COL);  // == 0

        // First action: place a digit (5 is a duplicate in this box/row context)
        model.setCell(ROW, COL, 5);

        // Intermediate postcondition: digit was stored
        assertEquals("Intermediate: getCell(2,0) must be 5 after setCell",
                     5, model.getCell(ROW, COL));

        // Second action: undo the placement
        model.undo();

        // Postcondition 1: cell reverts to its value before setCell
        assertEquals("Postcondition: getCell(2,0) must revert to " + prevValue,
                     prevValue, model.getCell(ROW, COL));

        // Postcondition 2: invalid flag is cleared after undo
        assertFalse("Postcondition: isInvalid(2,0) must be false after undo",
                    model.isInvalid(ROW, COL));

        // Postcondition 3: board is not complete (still has empty cells)
        assertFalse("Postcondition: isComplete() must be false (cells still empty)",
                    model.isComplete());

        // Postcondition 4: class invariant still holds (value in range)
        int val = model.getCell(ROW, COL);
        assertTrue("Postcondition: getCell result must satisfy 0<=v<=9",
                   val >= 0 && val <= 9);
    }
}
