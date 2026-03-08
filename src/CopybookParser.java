import java.util.*;
import java.util.regex.*;

/**
 * Parses a COBOL copybook and generates JT400 RecordFormat field descriptions.
 * <p>
 * Supported PIC types:
 * PIC X(n)              → CharacterFieldDescription  / AS400Text
 * PIC S9(m)V99          → ZonedDecimalFieldDescription / AS400ZonedDecimal  (DISPLAY)
 * PIC S9(m)V99  COMP-3  → PackedDecimalFieldDescription / AS400PackedDecimal
 * PIC S9(m)     COMP-4  → BinaryFieldDescription / AS400Bin2 or AS400Bin4
 * PIC 9(m)              → ZonedDecimalFieldDescription (unsigned)
 * <p>
 * OCCURS handling:
 * - Field-level OCCURS  n  → emits n separate addFieldDescription() calls,
 * each field named FIELDNAME_1 .. FIELDNAME_n
 * - Group-level OCCURS  n  → every child field is expanded n times the same way
 * - Nested OCCURS           → outer × inner expansion (3 rows × 4 cols = 12 fields)
 * <p>
 * Usage:
 * List<String> lines = CopybookParser.parseCopybook(copybookString);
 * List<String> lines = CopybookParser.parseCopybookFile("MY.cpy");
 */
public class CopybookParser {

    // ── Internal model ───────────────────────────────────────────────────────
    private record CobolField(
            int level,
            String name,
            String pic,    // null → group item
            String comp,   // null if none
            int occurs  // 0 = no OCCURS on this line
    ) {
    }

    // ── Patterns ─────────────────────────────────────────────────────────────
    // Leaf field (has PIC), optional COMP, optional OCCURS
    private static final Pattern FIELD_RE = Pattern.compile(
            "^\\s*(\\d{2})\\s+(\\S+)" +
                    "\\s+PIC\\s+(S?9+(?:\\(\\d+\\))?(?:V9*(?:\\(\\d+\\))?)?|X(?:\\(\\d+\\))?)" +
                    "(?:\\s+(COMP-[34]|COMP))?" +
                    "(?:\\s+OCCURS\\s+(\\d+)(?:\\s+TIMES)?)?" +
                    "\\s*\\.?\\s*$",
            Pattern.CASE_INSENSITIVE
    );

    // Group item (no PIC), optional OCCURS
    private static final Pattern GROUP_RE = Pattern.compile(
            "^\\s*(\\d{2})\\s+(\\S+)" +
                    "(?:\\s+OCCURS\\s+(\\d+)(?:\\s+TIMES)?)?" +
                    "\\s*\\.?\\s*$",
            Pattern.CASE_INSENSITIVE
    );

    // ── PIC parser ───────────────────────────────────────────────────────────
    record PicInfo(String type, int length, int intDigits, int decDigits) {
    }

    public static PicInfo parsePic(String pic) {
        pic = pic.trim().toUpperCase();
        if (pic.startsWith("X")) {
            Matcher m = Pattern.compile("X\\((\\d+)\\)").matcher(pic);
            return new PicInfo("CHAR", m.find() ? Integer.parseInt(m.group(1)) : 1, 0, 0);
        }
        boolean signed = pic.startsWith("S");
        String work = signed ? pic.substring(1) : pic;
        int intD = 1, decD = 0;
        Matcher im = Pattern.compile("^9(?:\\((\\d+)\\))?").matcher(work);
        if (im.find()) {
            intD = im.group(1) != null ? Integer.parseInt(im.group(1)) : 1;
            work = work.substring(im.end());
        }
        if (work.startsWith("V")) {
            work = work.substring(1);
            Matcher dm = Pattern.compile("^9(?:\\((\\d+)\\))?").matcher(work);
            if (dm.find())
                decD = dm.group(1) != null ? Integer.parseInt(dm.group(1)) : countNines(work);
        }
        return new PicInfo("NUMERIC", intD + decD, intD, decD);
    }

    private static int countNines(String s) {
        int n = 0;
        for (char c : s.toCharArray()) {
            if (c == '9') n++;
            else break;
        }
        return Math.max(n, 1);
    }

    // ── Name normaliser ──────────────────────────────────────────────────────
    private static String normalise(String name) {
        return name
                .replaceAll("(?i)^WS[-_]MSG[-_]", "")
                .replaceAll("(?i)^WS[-_]", "")
                .replace("-", "_")
                .toUpperCase();
    }

    // ── Code-line builder for one leaf field ────────────────────────────────
    private static String fieldDescLine(String cobolName, String pic,
                                        String comp, String keySuffix) {
        PicInfo info = parsePic(pic);
        String key = normalise(cobolName) + (keySuffix == null ? "" : keySuffix);
        comp = comp == null ? "" : comp.toUpperCase();

        String ctor;
        if ("CHAR".equals(info.type)) {
            ctor = String.format(
                    "new CharacterFieldDescription(new AS400Text(%d), \"%s\")",
                    info.length, key);
        } else if (comp.equals("COMP-3")) {
            ctor = String.format(
                    "new PackedDecimalFieldDescription(new AS400PackedDecimal(%d, %d), \"%s\")",
                    info.length, info.decDigits, key);
        } else if (comp.equals("COMP-4") || comp.equals("COMP")) {
            String bin = info.intDigits <= 4 ? "AS400Bin2()" : "AS400Bin4()";
            ctor = String.format("new BinaryFieldDescription(new %s, \"%s\")", bin, key);
        } else {
            ctor = String.format(
                    "new ZonedDecimalFieldDescription(new AS400ZonedDecimal(%d, %d), \"%s\")",
                    info.length, info.decDigits, key);
        }
        return "format.addFieldDescription(" + ctor + ");";
    }

    // ── Line-continuation joiner ─────────────────────────────────────────────
    /**
     * Joins physical COBOL lines into logical lines.  Handles two cases:
     * (a) Fixed-format: col 7 = '-'
     * (b) Free-form: next line opens with a clause keyword (OCCURS, COMP-3,
     * VALUE, REDEFINES ...) instead of a level number - very common when
     * OCCURS is written on the line below PIC.
     */
    private static final Pattern CONTINUATION_KW = Pattern.compile(
            "^\\s*(OCCURS|TIMES|COMP-[34]|COMP|VALUE|REDEFINES)\\b",
            Pattern.CASE_INSENSITIVE);

    private static List<String> joinContinuations(String[] raw) {
        List<String> out = new ArrayList<>();
        StringBuilder buf = null;
        for (String r : raw) {
            String ind = r.length() > 6 ? String.valueOf(r.charAt(6)) : " ";
            String body = r.length() > 7 ? r.substring(7) : r;
            String stripped = body.strip();
            // Skip blank / comment lines
            if (stripped.isEmpty() || stripped.startsWith("*")) {
                if (buf != null) {
                    out.add(buf.toString());
                    buf = null;
                }
                continue;
            }
            // Fixed-format continuation (col 7 = '-')
            if ("-".equals(ind) && buf != null) {
                buf.append(" ").append(stripped.replaceAll("^'", ""));
            }
            // Free-form: line starts with a clause keyword -> belongs to previous line
            else if (CONTINUATION_KW.matcher(stripped).find() && buf != null) {
                buf.append(" ").append(stripped);
            } else {
                if (buf != null) out.add(buf.toString());
                buf = new StringBuilder(body);
            }
        }
        if (buf != null) out.add(buf.toString());
        return out;
    }

    // ── First pass: parse all lines into CobolField list ────────────────────
    private static List<CobolField> parseFields(String copybook) {
        List<CobolField> fields = new ArrayList<>();
        for (String line : joinContinuations(copybook.split("\\r?\\n"))) {
            if (line.trim().startsWith("*") || line.trim().isEmpty()) continue;
            line = line.replaceAll("\\.\\s*$", "");

            Matcher fm = FIELD_RE.matcher(line);
            if (fm.find()) {
                int lv = Integer.parseInt(fm.group(1));
                if (lv == 1) continue;
                fields.add(new CobolField(lv, fm.group(2), fm.group(3), fm.group(4),
                        fm.group(5) != null ? Integer.parseInt(fm.group(5)) : 0));
                continue;
            }

            Matcher gm = GROUP_RE.matcher(line);
            if (gm.find()) {
                int lv = Integer.parseInt(gm.group(1));
                if (lv == 1) continue;
                // group item: no PIC
                fields.add(new CobolField(lv, gm.group(2), null, null,
                        gm.group(3) != null ? Integer.parseInt(gm.group(3)) : 0));
            }
        }
        return fields;
    }

    // ── Second pass: expand OCCURS ───────────────────────────────────────────

    /**
     * Walk the field list maintaining a stack of ancestor (level → occursCount).
     * For every leaf field the "effective occurs" is:
     * own OCCURS  ×  product of all ancestor OCCURS counts
     * <p>
     * Each combination gets its own suffixed name:
     * WS-ITEM-PRICE with parent OCCURS 3 → ITEM_PRICE_1, ITEM_PRICE_2, ITEM_PRICE_3
     * WS-CELL with parent OCCURS 3 and own OCCURS 4 →
     * CELL_1_1 .. CELL_1_4, CELL_2_1 .. CELL_2_4, CELL_3_1 .. CELL_3_4
     */
    public static List<String> parseCopybook(String copybook) {
        List<CobolField> fields = parseFields(copybook);
        List<String> output = new ArrayList<>();

        // Stack entries: [level, occursCount, currentIndex (unused here)]
        // We store (level, occurs) pairs to support nesting
        Deque<int[]> stack = new ArrayDeque<>(); // [level, occurs]

        for (CobolField f : fields) {
            // Pop ancestors that are no longer parents
            while (!stack.isEmpty() && stack.peek()[0] >= f.level())
                stack.pop();

            if (f.pic() == null) {
                // Group item – only push if it carries OCCURS
                if (f.occurs() > 0) {
                    stack.push(new int[]{f.level(), f.occurs()});
                    output.add(String.format("// ── GROUP %s  OCCURS %d ──",
                            normalise(f.name()), f.occurs()));
                }
            } else {
                // Leaf field – collect ancestor OCCURS counts (innermost first)
                List<Integer> ancestorOccurs = new ArrayList<>();
                for (int[] e : stack) ancestorOccurs.add(0, e[1]); // reverse → outermost first

                int ownOccurs = f.occurs(); // 0 = no own OCCURS

                if (ancestorOccurs.isEmpty() && ownOccurs == 0) {
                    // Simple case – no OCCURS anywhere
                    output.add(fieldDescLine(f.name(), f.pic(), f.comp(), null));
                } else {
                    // Build all index combinations
                    if (ownOccurs > 0) {
                        // Treat own OCCURS as innermost dimension
                        ancestorOccurs.add(ownOccurs);
                        output.add(String.format(
                                "// %s  OCCURS %d (effective dimensions: %s)",
                                normalise(f.name()), ownOccurs, ancestorOccurs));
                    } else {
                        output.add(String.format(
                                "// %s  inherited from group OCCURS %s",
                                normalise(f.name()), ancestorOccurs));
                    }

                    List<String> suffixes = buildSuffixes(ancestorOccurs);
                    for (String suffix : suffixes) {
                        output.add(fieldDescLine(f.name(), f.pic(), f.comp(), suffix));
                    }
                }
            }
        }
        return output;
    }

    /**
     * Given a list of dimension sizes [d1, d2, ...] produce every combination
     * as a suffix string: "_1_1", "_1_2", ..., "_d1_d2".
     */
    private static List<String> buildSuffixes(List<Integer> dims) {
        List<String> result = new ArrayList<>();
        result.add("");  // seed
        for (int dim : dims) {
            List<String> next = new ArrayList<>();
            for (String prev : result)
                for (int i = 1; i <= dim; i++)
                    next.add(prev + "_" + i);
            result = next;
        }
        return result;
    }

    /**
     * Convenience: read copybook from a file.
     */
    public static List<String> parseCopybookFile(String path) throws Exception {
        String content = new String(java.nio.file.Files.readAllBytes(
                java.nio.file.Paths.get(path)));
        return parseCopybook(content);
    }

    // ── Demo ─────────────────────────────────────────────────────────────────
    public static void main(String[] args) throws Exception {

        // ── Test 1: original copybook (no OCCURS) ───────────────────────────
        String cb1 = """
                       01  WS-DTAQ-MESSAGE.
                           05  WS-MSG-TEXT        PIC X(10).
                           05  WS-MSG-ZONED_POS   PIC S9(5)V99.
                           05  WS-MSG-ZONED_NEG   PIC S9(5)V99.
                           05  WS-MSG-PACKED_POS  PIC S9(5)V99   COMP-3.
                           05  WS-MSG-PACKED_NEG  PIC S9(5)V99   COMP-3.
                           05  WS-MSG-BINARY_POS  PIC S9(5)      COMP-4.
                           05  WS-MSG-BINARY_NEG  PIC S9(5)      COMP-4.
                """;

        banner("Test 1 – original copybook (no OCCURS)");
        parseCopybook(cb1).forEach(System.out::println);

        // ── Test 2: field-level OCCURS ───────────────────────────────────────
        String cb2 = """
                       01  WS-ORDER.
                           05  WS-ORDER-ID        PIC 9(8).
                           05  WS-LINE-AMT        PIC S9(7)V99 COMP-3
                                                  OCCURS 5 TIMES.
                           05  WS-LINE-CODE       PIC X(3)
                                                  OCCURS 5 TIMES.
                """;

        banner("Test 2 – field-level OCCURS 5");
        parseCopybook(cb2).forEach(System.out::println);

        // ── Test 3: group-level OCCURS ───────────────────────────────────────
        String cb3 = """
                       01  WS-INVOICE.
                           05  WS-INV-NUMBER      PIC 9(8).
                           05  WS-INV-LINE        OCCURS 3 TIMES.
                               10  WS-ITEM-CODE   PIC X(5).
                               10  WS-ITEM-QTY    PIC S9(5)      COMP-4.
                               10  WS-ITEM-PRICE  PIC S9(7)V99   COMP-3.
                           05  WS-INV-TOTAL       PIC S9(9)V99   COMP-3.
                """;

        banner("Test 3 – group-level OCCURS 3 (sub-record array)");
        parseCopybook(cb3).forEach(System.out::println);

        // ── Test 4: nested OCCURS (3 rows × 4 columns) ──────────────────────
        String cb4 = """
                       01  WS-MATRIX.
                           05  WS-ROW             OCCURS 3 TIMES.
                               10  WS-CELL        PIC S9(5)V99
                                                  OCCURS 4 TIMES.
                """;

        banner("Test 4 – nested OCCURS  3 rows × 4 cols = 12 fields");
        parseCopybook(cb4).forEach(System.out::println);
    }

    private static void banner(String title) {
        System.out.println();
        System.out.println("// ══════════════════════════════════════════════════════");
        System.out.println("// " + title);
        System.out.println("// ══════════════════════════════════════════════════════");
        System.out.println("RecordFormat format = new RecordFormat();");
    }
}
