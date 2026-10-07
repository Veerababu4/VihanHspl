package com.vihan.hospital.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.vihan.hospital.common.error.ApiException;

class TableExportTest {

	private static ReportTable table(Object... cells) {
		return new ReportTable("Test report", "today", List.of("A", "B", "C"), List.of(Arrays.asList(cells)));
	}

	@Test
	void csvStartsWithABomAndQuotesWhereNeeded() {
		String csv = new String(TableExport.csv(table("Rao, K", "say \"hi\"", 12)), StandardCharsets.UTF_8);
		assertThat(csv).isEqualTo("﻿A,B,C\r\n\"Rao, K\",\"say \"\"hi\"\"\",12\r\n");
	}

	@Test
	void csvNeutralisesFormulasTypedIntoPublicForms() {
		String csv = new String(TableExport.csv(table("=HYPERLINK(\"x\")", "+91", -5)), StandardCharsets.UTF_8);
		assertThat(csv).endsWith("\"'=HYPERLINK(\"\"x\"\")\",\"'+91\",-5\r\n");   // numbers stay numbers
	}

	@Test
	void csvWritesNullAsAnEmptyCell() {
		assertThat(new String(TableExport.csv(table("x", null, 1.5)), StandardCharsets.UTF_8)).endsWith("x,,1.5\r\n");
	}

	@Test
	void xlsxIsAZipFile() {
		byte[] xlsx = TableExport.xlsx(table("Telugu: వైద్యుడు", 3, null));
		assertThat(new String(xlsx, 0, 2, StandardCharsets.US_ASCII)).isEqualTo("PK");
	}

	@Test
	void onlyCsvAndXlsxAreAccepted() {
		assertThat(TableExport.parse(" XLSX ")).isEqualTo(TableExport.Format.XLSX);
		assertThatThrownBy(() -> TableExport.parse("pdf")).isInstanceOf(ApiException.class);
		assertThat(ReportService.pct(1, 6)).isEqualTo(16.7);
		assertThat(ReportService.pct(0, 0)).isNull();
	}

}
