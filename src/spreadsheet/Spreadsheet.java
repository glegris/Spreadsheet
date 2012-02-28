package spreadsheet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import spreadsheet.api.CellLocation;
import spreadsheet.api.SpreadsheetInterface;
import spreadsheet.api.value.*;

public class Spreadsheet implements SpreadsheetInterface {

    private Map<CellLocation, Cell> locations =
            new HashMap<CellLocation, Cell>();

    private Set<Cell> invalid = new HashSet<Cell>();

    private Set<Cell> ignore = new HashSet<Cell>();

    @Override
    public void setExpression(CellLocation location, String expr) {
        if (getCellAt(location) != null) {
            Cell c = getCellAt(location);
            if (!invalid.contains(c)) {
                c.setExpr(expr);
            }
            c.setVal(new InvalidValue(expr));
        } else {
            Cell c = new Cell(this, location);
            c.setExpr(expr);
            c.setVal(new InvalidValue(expr));
            locations.put(location, c);
        }
    }

    @Override
    public String getExpression(CellLocation location) {
        Cell c = getCellAt(location);
        return c != null ? c.getExpr() : "";
    }

    @Override
    public Value getValue(CellLocation location) {
        Cell c = getCellAt(location);
        return c != null ? c.getVal() : null;
    }

    @Override
    public void recompute() {
        Iterator<Cell> i = invalid.iterator();
        while (i.hasNext()) {
            Cell c = i.next();
            recomputeCell(c);
            i.remove();
        }
    }

    private void recomputeCell(Cell c) {
        LinkedHashSet<Cell> seen = new LinkedHashSet<Cell>();
        checkLoops(c, seen);
        if (c.getVal() != LoopValue.INSTANCE) {
            if (!ignore.contains(c)) {
                c.setVal(new StringValue(c.getExpr()));
            } else {
                c.setVal(new InvalidValue(c.getExpr()));
            }
        }
    }

    private void checkLoops(Cell c, LinkedHashSet<Cell> cellsSeen) {
        if (cellsSeen.contains(c)) {
            markAsLoop(c, cellsSeen);
        } else {
            cellsSeen.add(c);
            for (Cell child : c.getReferences()) {
                checkLoops(child, cellsSeen);
            }
            cellsSeen.remove(c);
        }
    }

    private void markAsLoop(Cell startCell, LinkedHashSet<Cell> cells) {
        startCell.setVal(LoopValue.INSTANCE);

        boolean seenStart = false;
        for (Cell c : cells) {
            ignore.add(c); // not sure if this will "remove" cells from invalid
            if (c.getLoc().equals(startCell.getLoc())) {
                seenStart = true;
            }
            if (seenStart) {
                c.setVal(LoopValue.INSTANCE);
            }
        }
    }

    public Set<Cell> getInvalid() {
        return invalid;
    }

    public Cell getCellAt(CellLocation loc) {
        return locations.get(loc);
    }

}
