package ReaderAndConverter.UI;

import ReaderAndConverter.App.AppConfig;
import ReaderAndConverter.Converter.*;
import ReaderAndConverter.Exceptions.CsvWriteException;
import ReaderAndConverter.Exceptions.JsonReadException;
import ReaderAndConverter.Model.Table;
import ReaderAndConverter.Reader.JsonReader;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.QuoteMode;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Minimal Swing UI that wires the existing pipeline:
 *
 * - Select JSON file and preview it (pretty-printed)
 * - Configure Normalizer + CSV options
 * - Convert -> normalize + preview CSV for each table (selectable)
 * - Save CSVs -> writes all tables to chosen folder
 *
 * This class is self-contained; it uses your existing JsonReader/JsonNormalizer/CsvWriter.
 */
public final class DesktopApp extends JFrame {

    // ----- Left: JSON input -----
    private final JTextArea jsonArea = new JTextArea();
    private final JButton openJsonBtn = new JButton("Open JSON…");
    private final JLabel jsonPathLabel = new JLabel("No file selected");

    // ----- Normalizer options -----
    private final JTextField rootTableField = new JTextField("root");
    private final JCheckBox allowNestedChk = new JCheckBox("Allow nested tables", true);
    private final JComboBox<String> arrayStrategyCombo =
            new JComboBox<>(new String[]{"explode", "inline"}); // arrays of objects
    private final JComboBox<String> primitiveArrayCombo =
            new JComboBox<>(new String[]{"join", "explode"});   // arrays of primitives
    private final JCheckBox addIdChk = new JCheckBox("Add id", true);
    private final JCheckBox addParentIdChk = new JCheckBox("Add parent_id", true);
    private final JTextField idFieldField = new JTextField("id");
    private final JTextField parentIdFieldField = new JTextField("parent_id");
    private final JTextField normJoinField = new JTextField("; ");
    private final JSpinner maxDepthSpinner = new JSpinner(new SpinnerNumberModel(64, 1, 9999, 1));
    private final JComboBox<String> headerOrderCombo =
            new JComboBox<>(new String[]{"sorted", "encountered"});

    // ----- CSV options -----
    private final JTextField delimiterField = new JTextField(",");
    private final JTextField quoteField = new JTextField("\"");
    private final JTextField recordSepField = new JTextField("\\n");
    private final JComboBox<String> quoteModeCombo =
            new JComboBox<>(new String[]{"minimal", "all", "none", "non_numeric"});
    private final JCheckBox headerChk = new JCheckBox("Print header", true);
    private final JTextField nullLiteralField = new JTextField("");
    private final JComboBox<String> encodingCombo =
            new JComboBox<>(new String[]{"UTF-8", "ISO-8859-1", "US-ASCII"});
    private final JTextField csvArrayJoinField = new JTextField("; ");
    private final JCheckBox excelSafeChk = new JCheckBox("Excel-safe", true);
    private final JCheckBox objectInlineJsonChk = new JCheckBox("Objects inline as JSON", true);
    private final JTextField dateFmtField = new JTextField("ISO_INSTANT");

    // ----- Actions -----
    private final JButton convertBtn = new JButton("Convert JSON");
    private final JButton saveBtn = new JButton("Save CSVs…");

    // ----- Right: CSV preview -----
    private final JComboBox<String> tablePicker = new JComboBox<>();
    private final JTextArea csvPreviewArea = new JTextArea();

    // Data from last conversion
    private List<Table> lastTables = List.of();
    private Map<String, String> lastCsvByTable = Map.of();

    private final ObjectMapper prettyMapper = new ObjectMapper();

    public DesktopApp() {
        super("JSON → CSV (Desktop) — Digital NAO");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Left panel: file + JSON area + options
        JPanel left = new JPanel(new BorderLayout(8, 8));
        left.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // File controls
        JPanel fileRow = new JPanel(new BorderLayout(8, 8));
        fileRow.add(openJsonBtn, BorderLayout.WEST);
        fileRow.add(jsonPathLabel, BorderLayout.CENTER);
        left.add(fileRow, BorderLayout.NORTH);

        // JSON area
        jsonArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        jsonArea.setEditable(false);
        JScrollPane jsonScroll = new JScrollPane(jsonArea);
        jsonScroll.setBorder(new TitledBorder("JSON input"));
        left.add(jsonScroll, BorderLayout.CENTER);

        // Options
        JPanel options = new JPanel(new GridLayout(1, 2, 8, 8));
        options.add(buildNormalizerPanel());
        options.add(buildCsvPanel());
        left.add(options, BorderLayout.SOUTH);

        // Right panel: results
        JPanel right = new JPanel(new BorderLayout(8, 8));
        right.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        actions.add(convertBtn);
        actions.add(saveBtn);

        JPanel tableRow = new JPanel(new BorderLayout(8, 8));
        tableRow.add(new JLabel("Table:"), BorderLayout.WEST);
        tableRow.add(tablePicker, BorderLayout.CENTER);

        JPanel topRight = new JPanel(new BorderLayout(8, 8));
        topRight.add(actions, BorderLayout.NORTH);
        topRight.add(tableRow, BorderLayout.SOUTH);
        right.add(topRight, BorderLayout.NORTH);

        csvPreviewArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane csvScroll = new JScrollPane(csvPreviewArea);
        csvScroll.setBorder(new TitledBorder("CSV preview"));
        right.add(csvScroll, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setResizeWeight(0.5);
        add(split, BorderLayout.CENTER);

        // Wire actions
        openJsonBtn.addActionListener(e -> onOpenJson());
        convertBtn.addActionListener(e -> onConvert());
        saveBtn.addActionListener(e -> onSaveCsvs());
        tablePicker.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                showSelectedTableCsv();
            }
        });

        setSize(1200, 700);
        setLocationRelativeTo(null);
    }

    // ----- Panels -----

    private JPanel buildNormalizerPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(new TitledBorder("Normalizer options"));
        GridBagConstraints c = gbDefaults();

        addRow(p, c, new JLabel("Root table"), rootTableField);
        addRow(p, c, allowNestedChk, new JLabel()); // single control row
        addRow(p, c, new JLabel("Arrays of objects"), arrayStrategyCombo);
        addRow(p, c, new JLabel("Arrays of primitives"), primitiveArrayCombo);
        addRow(p, c, addIdChk, addParentIdChk);
        addRow(p, c, new JLabel("id field"), idFieldField);
        addRow(p, c, new JLabel("parent id field"), parentIdFieldField);
        addRow(p, c, new JLabel("Join for primitive arrays"), normJoinField);
        addRow(p, c, new JLabel("Max depth"), maxDepthSpinner);
        addRow(p, c, new JLabel("Header order"), headerOrderCombo);

        return p;
    }

    private JPanel buildCsvPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(new TitledBorder("CSV options"));
        GridBagConstraints c = gbDefaults();

        addRow(p, c, new JLabel("Delimiter"), delimiterField);
        addRow(p, c, new JLabel("Quote"), quoteField);
        addRow(p, c, new JLabel("Record separator"), recordSepField);
        addRow(p, c, new JLabel("Quote mode"), quoteModeCombo);
        addRow(p, c, headerChk, new JLabel());
        addRow(p, c, new JLabel("Null literal"), nullLiteralField);
        addRow(p, c, new JLabel("Encoding"), encodingCombo);
        addRow(p, c, new JLabel("Join inside a cell"), csvArrayJoinField);
        addRow(p, c, excelSafeChk, objectInlineJsonChk);
        addRow(p, c, new JLabel("Date format"), dateFmtField);

        return p;
    }

    private GridBagConstraints gbDefaults() {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.gridx = 0; c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.0;
        return c;
    }

    private static void addRow(JPanel p, GridBagConstraints c, JComponent left, JComponent right) {
        GridBagConstraints c1 = (GridBagConstraints) c.clone();
        GridBagConstraints c2 = (GridBagConstraints) c.clone();
        c1.gridx = 0; c1.weightx = 0.0; c1.gridwidth = 1;
        c2.gridx = 1; c2.weightx = 1.0; c2.gridwidth = 1;
        p.add(left, c1);
        p.add(right, c2);
        c.gridy++;
    }

    // ----- Actions -----

    private void onOpenJson() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("JSON files", "json"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            jsonPathLabel.setText(f.getAbsolutePath());
            try {
                // Pretty print in the left area
                JsonNode node = new ObjectMapper().readTree(f);
                jsonArea.setText(prettyMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node));
                jsonArea.setCaretPosition(0);
            } catch (IOException ex) {
                showError("Failed to open JSON: " + ex.getMessage());
            }
        }
    }

    private void onConvert() {
        // Build configs from UI
        CsvFormatOptions csv = readCsvOptions();
        NormalizerConfig norm = readNormalizerOptions();

        // Read + normalize
        String path = jsonPathLabel.getText();
        if (path == null || path.isBlank() || path.equals("No file selected")) {
            showError("Please select a JSON file first.");
            return;
        }
        try {
            JsonReader reader = new JsonReader();
            JsonNode root = reader.readTree(Path.of(path));
            List<Table> tables = new JsonNormalizer(norm).normalize(root);

            // Render CSV previews to strings
            Map<String, String> csvByTable = new LinkedHashMap<>();
            for (Table t : tables) {
                String csvText = renderCsvToString(t.headers(), t.rows(), csv);
                csvByTable.put(t.name(), csvText);
            }

            // Update state + UI
            this.lastTables = tables;
            this.lastCsvByTable = csvByTable;

            tablePicker.removeAllItems();
            for (String name : csvByTable.keySet()) tablePicker.addItem(name);
            if (tablePicker.getItemCount() > 0) {
                tablePicker.setSelectedIndex(0);
                showSelectedTableCsv();
            } else {
                csvPreviewArea.setText("");
            }
        } catch (JsonReadException e) {
            showError("JSON read error: " + e.getMessage());
        } catch (Exception e) {
            showError("Conversion failed: " + e.getMessage());
        }
    }

    private void onSaveCsvs() {
        if (lastTables.isEmpty()) {
            showError("Nothing to save. Convert first.");
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Choose output directory");
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File dir = chooser.getSelectedFile();
            Path outDir = dir.toPath();
            try {
                CsvWriter writer = new CsvWriter(new DefaultValueFormatter());
                CsvFormatOptions csv = readCsvOptions();
                for (Table t : lastTables) {
                    Path out = outDir.resolve(t.name() + ".csv");
                    writer.write(out, t.headers(), t.rows(), csv);
                }
                JOptionPane.showMessageDialog(this, "CSV files saved to:\n" + outDir.toAbsolutePath());
            } catch (CsvWriteException ex) {
                showError("Write error: " + ex.getMessage());
            } catch (Exception ex) {
                showError("Failed to save: " + ex.getMessage());
            }
        }
    }

    private void showSelectedTableCsv() {
        String name = (String) tablePicker.getSelectedItem();
        String text = (name == null) ? "" : lastCsvByTable.getOrDefault(name, "");
        csvPreviewArea.setText(text);
        csvPreviewArea.setCaretPosition(0);
    }

    // ----- Config builders -----

    private CsvFormatOptions readCsvOptions() {
        char delimiter = getOneChar(delimiterField.getText(), ',');
        char quote = getOneChar(quoteField.getText(), '"');
        String recordSep = unescape(recordSepField.getText().isBlank() ? "\\n" : recordSepField.getText());
        QuoteMode quoteMode = switch (quoteModeCombo.getSelectedItem().toString()) {
            case "all" -> QuoteMode.ALL;
            case "none" -> QuoteMode.NONE;
            case "non_numeric" -> QuoteMode.NON_NUMERIC;
            default -> QuoteMode.MINIMAL;
        };
        boolean printHeader = headerChk.isSelected();
        String nullLiteral = nullLiteralField.getText();
        Charset encoding = switch (Objects.toString(encodingCombo.getSelectedItem(), "UTF-8")) {
            case "ISO-8859-1" -> StandardCharsets.ISO_8859_1;
            case "US-ASCII" -> StandardCharsets.US_ASCII;
            default -> StandardCharsets.UTF_8;
        };
        String arrayJoin = csvArrayJoinField.getText().isBlank() ? "; " : csvArrayJoinField.getText();
        boolean excelSafe = excelSafeChk.isSelected();
        boolean objectInlineJson = objectInlineJsonChk.isSelected();
        DateTimeFormatter dtf = "ISO_INSTANT".equalsIgnoreCase(dateFmtField.getText().trim())
                ? DateTimeFormatter.ISO_INSTANT
                : DateTimeFormatter.ofPattern(dateFmtField.getText().trim(), Locale.ROOT);

        return new CsvFormatOptions(
                delimiter, quote, recordSep, quoteMode, printHeader,
                nullLiteral, encoding, arrayJoin, excelSafe, objectInlineJson, dtf
        );
    }

    private NormalizerConfig readNormalizerOptions() {
        String rootTable = rootTableField.getText().isBlank() ? "root" : rootTableField.getText().trim();
        boolean allowNested = allowNestedChk.isSelected();
        NormalizerConfig.ArrayStrategy as = "inline".equals(arrayStrategyCombo.getSelectedItem())
                ? NormalizerConfig.ArrayStrategy.INLINE_AS_JSON
                : NormalizerConfig.ArrayStrategy.EXPLODE_TO_CHILD;
        NormalizerConfig.PrimitiveArrayMode pm = "explode".equals(primitiveArrayCombo.getSelectedItem())
                ? NormalizerConfig.PrimitiveArrayMode.EXPLODE_TO_CHILD
                : NormalizerConfig.PrimitiveArrayMode.JOIN;
        boolean addId = addIdChk.isSelected();
        boolean addParent = addParentIdChk.isSelected();
        String idField = idFieldField.getText().isBlank() ? "id" : idFieldField.getText().trim();
        String parentField = parentIdFieldField.getText().isBlank() ? "parent_id" : parentIdFieldField.getText().trim();
        String join = normJoinField.getText().isBlank() ? "; " : normJoinField.getText();
        int maxDepth = (Integer) maxDepthSpinner.getValue();
        NormalizerConfig.HeaderOrder ho = "encountered".equals(headerOrderCombo.getSelectedItem())
                ? NormalizerConfig.HeaderOrder.ENCOUNTERED
                : NormalizerConfig.HeaderOrder.SORTED;

        return new NormalizerConfig(
                rootTable, allowNested, as, pm,
                addId, addParent, idField, parentField,
                join, maxDepth, ho
        );
    }

    // ----- Helpers -----

    private static char getOneChar(String s, char fallback) {
        return (s != null && s.length() >= 1) ? s.charAt(0) : fallback;
    }

    private static String unescape(String s) {
        if (s == null) return "\n";
        return s.replace("\\r\\n", "\r\n").replace("\\n", "\n").replace("\\r", "\r").replace("\\t", "\t");
    }

    private static void showError(String msg) {
        JOptionPane.showMessageDialog(null, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Render a table as CSV text for preview using Commons CSV.
     * (This mirrors CsvWriter but writes to a String.)
     */
    private static String renderCsvToString(List<String> headers,
                                            List<? extends Map<String, ?>> rows,
                                            CsvFormatOptions opts) throws IOException {
        CSVFormat base = CSVFormat.Builder.create()
                .setDelimiter(opts.delimiter())
                .setQuote(opts.quote())
                .setRecordSeparator(opts.recordSeparator())
                .setQuoteMode(opts.quoteMode() == null ? QuoteMode.MINIMAL : opts.quoteMode())
                .setNullString(opts.nullString())
                .build();
        CSVFormat format = opts.printHeader()
                ? base.builder().setHeader(headers.toArray(new String[0])).build()
                : base;

        ValueFormatter vf = new DefaultValueFormatter();
        try (StringWriter sw = new StringWriter();
             CSVPrinter printer = new CSVPrinter(sw, format)) {
            for (Map<String, ?> row : rows) {
                Object[] record = new Object[headers.size()];
                for (int i = 0; i < headers.size(); i++) {
                    String key = headers.get(i);
                    String cell = vf.format(row.get(key), opts);
                    record[i] = cell;
                }
                printer.printRecord(record);
            }
            printer.flush();
            return sw.toString();
        }
    }

    // ----- main() -----
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DesktopApp().setVisible(true));
    }
}
