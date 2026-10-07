package com.vihan.hospital.report;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.vihan.hospital.common.error.ApiException.BadRequestException;

/** Writes a {@link ReportTable} as CSV (opens in Excel) or XLSX (FR-D5, FR-D6). */
public final class TableExport {

	public enum Format { CSV, XLSX }

	private static final MediaType XLSX_TYPE =
			MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
	/** Byte order mark: without it Excel opens a UTF-8 CSV as ANSI and breaks Telugu names. */
	private static final String BOM = "﻿";
	private static final int TABLE_START_ROW = 3;

	private TableExport() {
	}

	public static Format parse(String format) {
		try {
			return Format.valueOf(format.strip().toUpperCase(Locale.ROOT));
		}
		catch (IllegalArgumentException | NullPointerException e) {
			throw new BadRequestException("INVALID_FORMAT", "Format must be csv or xlsx");
		}
	}

	/** @param fileBase file name without extension, e.g. {@code dashboard-2026-10-07} */
	public static ResponseEntity<byte[]> download(ReportTable table, Format format, String fileBase) {
		boolean csv = format == Format.CSV;
		byte[] body = csv ? csv(table) : xlsx(table);
		return ResponseEntity.ok()
			.contentType(csv ? new MediaType("text", "csv", StandardCharsets.UTF_8) : XLSX_TYPE)
			.header(HttpHeaders.CONTENT_DISPOSITION,
					ContentDisposition.attachment().filename(fileBase + (csv ? ".csv" : ".xlsx")).build().toString())
			.body(body);
	}

	static byte[] csv(ReportTable table) {
		StringBuilder sb = new StringBuilder(BOM);
		sb.append(line(table.columns().stream().map(Object.class::cast).toList()));
		table.rows().forEach(r -> sb.append(line(r)));
		return sb.toString().getBytes(StandardCharsets.UTF_8);
	}

	static byte[] xlsx(ReportTable table) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			Workbook wb = new Workbook(out, "Vihan Hospital", "1.0");
			Worksheet ws = wb.newWorksheet(sheetName(table.title()));
			ws.value(0, 0, table.title());
			ws.style(0, 0).bold().fontSize(14).set();
			ws.value(1, 0, table.subtitle());
			for (int c = 0; c < table.columns().size(); c++) {
				ws.value(TABLE_START_ROW, c, table.columns().get(c));
				ws.style(TABLE_START_ROW, c).bold().fillColor("E2E8F0").set();
				ws.width(c, Math.max(10, table.columns().get(c).length() + 4));
			}
			for (int r = 0; r < table.rows().size(); r++) {
				List<Object> row = table.rows().get(r);
				for (int c = 0; c < row.size(); c++) {
					Object v = row.get(c);
					if (v instanceof Number n) {
						ws.value(TABLE_START_ROW + 1 + r, c, n);
					}
					else if (v != null) {
						ws.value(TABLE_START_ROW + 1 + r, c, v.toString());
					}
				}
			}
			ws.freezePane(0, TABLE_START_ROW + 1);
			wb.finish();
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return out.toByteArray();
	}

	private static String line(List<Object> cells) {
		return cells.stream().map(TableExport::cell).collect(Collectors.joining(",")) + "\r\n";
	}

	private static String cell(Object v) {
		if (v == null) {
			return "";
		}
		if (v instanceof Number) {
			return v.toString();
		}
		String s = v.toString();
		// Formula injection: a patient could type "=HYPERLINK(...)" as their name on the public form.
		if (!s.isEmpty() && "=+-@\t\r".indexOf(s.charAt(0)) >= 0) {
			s = "'" + s;
		}
		boolean quote = s.startsWith("'") || s.chars().anyMatch(ch -> ch == '"' || ch == ',' || ch == '\r' || ch == '\n');
		return quote ? "\"" + s.replace("\"", "\"\"") + "\"" : s;
	}

	/** Excel sheet names: at most 31 characters, none of []:*?/\ */
	private static String sheetName(String title) {
		String s = title.replaceAll("[\\[\\]:*?/\\\\]", " ");
		return s.length() > 31 ? s.substring(0, 31) : s;
	}

}
