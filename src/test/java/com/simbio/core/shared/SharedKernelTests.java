package com.simbio.core.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

import com.simbio.core.shared.paging.PageQuery;
import com.simbio.core.shared.paging.PageResult;
import com.simbio.core.shared.value.EmailAddress;

class SharedKernelTests {

    @Test
    void normalizesEmailAndMasksLocalPartInStringRepresentation() {
        EmailAddress email = EmailAddress.of("  Ana@Example.COM ");

        assertThat(email.value()).isEqualTo("ana@example.com");
        assertThat(email.toString()).isEqualTo("***@example.com");
        assertThat(email.toString()).doesNotContain("ana");
    }

    @Test
    void rejectsPageSizesOutsideOneToOneHundred() {
        assertThatIllegalArgumentException().isThrownBy(() -> PageQuery.of(0, 101));
    }

    @Test
    void pageResultIsImmutableAndPreservesMetadata() {
        PageResult<String> result = new PageResult<>(java.util.List.of("item"), 0, 10, 1);

        assertThat(result.content()).containsExactly("item");
        assertThat(result.totalElements()).isEqualTo(1);
    }
}
