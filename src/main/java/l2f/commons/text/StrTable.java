package l2f.commons.text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *	
 *	StrTable table = new StrTable("Test Table :)");
 *	table.set(0, "#", 1).set(0, "val", 23.5).set(0, "desc", " value #1");
 *	table.set(1, "#", 2).set(1, "v", 22.5).set(1, "desc", " value #2");
 *	table.set(3, "#", 3).set(3, "val", true).set(3, "desc", " bool #3 1334");
 *	table.set(2, "#", -1).set(2, "v", 22.5).set(2, "desc", "#######");
 *	System.out.print(table);
 * 
 *          Test Table :)
 *  ----------------------------------
 * | #  | val  |     desc      |  v   |
 * |----|------|---------------|------|
 * | 1  | 23.5 |    value #1   |  -   |
 * | 2  |  -   |    value #2   | 22.5 |
 * | -1 |  -   |    #######    | 22.5 |
 * | 3  | true |  bool #3 1334 |  -   |
 *  ----------------------------------
 *  
 *  
 * @Author: Drin
 * @Date: 27/04/2009
 * 
 */
public class StrTable
{
    private final Map<Integer, Map<String, String>> rows;
    private final Map<String, Integer> columns;
    private final List<String> titles;
    
    public StrTable(final String title) {
        this.rows = new HashMap<>();
        this.columns = new LinkedHashMap<>();
        this.titles = new ArrayList<>();
        if (title != null) {
            this.titles.add(title);
        }
    }
    
    public StrTable() {
        this(null);
    }
    
    public StrTable set(final int rowIndex, final String colName, final boolean val) {
        return this.set(rowIndex, colName, Boolean.toString(val));
    }
    
    public StrTable set(final int rowIndex, final String colName, final byte val) {
        return this.set(rowIndex, colName, Byte.toString(val));
    }
    
    public StrTable set(final int rowIndex, final String colName, final char val) {
        return this.set(rowIndex, colName, String.valueOf(val));
    }
    
    public StrTable set(final int rowIndex, final String colName, final short val) {
        return this.set(rowIndex, colName, Short.toString(val));
    }
    
    public StrTable set(final int rowIndex, final String colName, final int val) {
        return this.set(rowIndex, colName, Integer.toString(val));
    }
    
    public StrTable set(final int rowIndex, final String colName, final long val) {
        return this.set(rowIndex, colName, Long.toString(val));
    }
    
    public StrTable set(final int rowIndex, final String colName, final float val) {
        return this.set(rowIndex, colName, Float.toString(val));
    }
    
    public StrTable set(final int rowIndex, final String colName, final double val) {
        return this.set(rowIndex, colName, Double.toString(val));
    }
    
    public StrTable set(final int rowIndex, final String colName, final Object val) {
        return this.set(rowIndex, colName, String.valueOf(val));
    }
    
    public StrTable set(final int rowIndex, final String colName, final String val) {
        Map<String, String> row;
        if (this.rows.containsKey(rowIndex)) {
            row = this.rows.get(rowIndex);
        }
        else {
            row = new HashMap<>();
            this.rows.put(rowIndex, row);
        }
        row.put(colName, val);
        int columnSize;
        if (!this.columns.containsKey(colName)) {
            columnSize = Math.max(colName.length(), val.length());
        }
        else if (this.columns.get(colName) >= (columnSize = val.length())) {
            return this;
        }
        this.columns.put(colName, columnSize);
        return this;
    }
    
    public StrTable addTitle(final String s) {
        this.titles.add(s);
        return this;
    }
    
    private static StringBuilder right(final StringBuilder result, final String s, int sz) {
        result.append(s);
        if ((sz -= s.length()) > 0) {
            for (int i = 0; i < sz; ++i) {
                result.append(" ");
            }
        }
        return result;
    }
    
    private static StringBuilder center(final StringBuilder result, final String s, final int sz) {
        final int offset = result.length();
        result.append(s);
        int i;
        while ((i = sz - (result.length() - offset)) > 0) {
            result.append(" ");
            if (i > 1) {
                result.insert(offset, " ");
            }
        }
        return result;
    }
    
    private static StringBuilder repeat(final StringBuilder result, final String s, final int sz) {
        for (int i = 0; i < sz; ++i) {
            result.append(s);
        }
        return result;
    }
    
    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();
        if (this.columns.isEmpty()) {
            return result.toString();
        }
        final StringBuilder header = new StringBuilder("|");
        final StringBuilder line = new StringBuilder("|");
        for (final String c : this.columns.keySet()) {
            center(header, c, this.columns.get(c) + 2).append("|");
            repeat(line, "-", this.columns.get(c) + 2).append("|");
        }
        if (!this.titles.isEmpty()) {
            result.append(" ");
            repeat(result, "-", header.length() - 2).append(" ").append("\n");
            for (final String title : this.titles) {
                result.append("| ");
                right(result, title, header.length() - 3).append("|").append("\n");
            }
        }
        result.append(" ");
        repeat(result, "-", header.length() - 2).append(" ").append("\n");
        result.append(header).append("\n");
        result.append(line).append("\n");
        for (final Map<String, String> row : this.rows.values()) {
            result.append("|");
            for (final String c2 : this.columns.keySet()) {
                center(result, row.containsKey(c2) ? row.get(c2) : "-", this.columns.get(c2) + 2).append("|");
            }
            result.append("\n");
        }
        result.append(" ");
        repeat(result, "-", header.length() - 2).append(" ").append("\n");
        return result.toString();
    }
}
