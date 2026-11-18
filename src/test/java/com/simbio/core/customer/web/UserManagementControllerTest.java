package com.simbio.core.customer.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserManagementControllerTest {

    @Test
    void neutralizesSpreadsheetFormulasAndQuotesCsvValues() {
        assertThat(UserManagementController.csvCell("=HYPERLINK(\"https://example.test\")"))
                .isEqualTo("\"'=HYPERLINK(\"\"https://example.test\"\")\"");
        assertThat(UserManagementController.csvCell("  +SUM(1,2)"))
                .isEqualTo("\"'  +SUM(1,2)\"");
        assertThat(UserManagementController.csvCell("Simbio"))
                .isEqualTo("\"Simbio\"");
    }

    @Test
    void neutralizesFormulaAfterLeadingControlWhitespace() {
        assertThat(UserManagementController.csvCell("\t@SUM(1,2)"))
                .isEqualTo("\"'\t@SUM(1,2)\"");
    }
}
