package com.vihan.hospital.report;

import java.util.List;

/**
 * A titled table, the one shape every report and the dashboard export use: shown on screen as
 * JSON and written to CSV / XLSX by {@link TableExport}. Cells are strings, numbers or null.
 *
 * @param subtitle filters and date range, e.g. "01-10-2026 to 07-10-2026 · All branches"
 */
public record ReportTable(String title, String subtitle, List<String> columns, List<List<Object>> rows) {
}
