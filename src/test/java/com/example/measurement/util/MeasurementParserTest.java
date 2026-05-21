package com.example.measurement.util;

import com.example.measurement.exception.InvalidInputException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MeasurementParserTest {

    private final MeasurementParser parser = new MeasurementParser();

    @Test
    @DisplayName("empty input -> empty list")
    void emptyInput() {
        assertThat(parser.parse("")).isEmpty();
    }

    @Test
    @DisplayName("a + b = single package, size 1, sum 2")
    void simpleOnePackage() {
        // 'a' header => size 1, 'b' value => 2
        assertThat(parser.parse("ab")).containsExactly(2);
    }

    @Test
    @DisplayName("c + abc = single package, size 3, sum 1+2+3 = 6")
    void simpleThreeValues() {
        assertThat(parser.parse("cabc")).containsExactly(6);
    }

    @Test
    @DisplayName("multiple packages chained")
    void multiplePackages() {
        // 'a','b' => pkg1 sum=2
        // 'b','c','d' => pkg2 size=2 sum=3+4=7
        assertThat(parser.parse("abbcd")).containsExactly(2, 7);
    }

    @Test
    @DisplayName("z continuation: zd = 30 — exercises 30 value chars")
    void zContinuationSingleZ() {
        // header 'zd' => 26 + 4 = 30 ; then 30 'a' chars (each = 1) => sum 30
        String header = "zd";
        String body = "a".repeat(30);
        assertThat(parser.parse(header + body)).containsExactly(30);
    }

    @Test
    @DisplayName("z continuation: zza = 53")
    void zContinuationDoubleZ() {
        String header = "zza";
        String body = "b".repeat(53); // each b = 2 ; total = 106
        assertThat(parser.parse(header + body)).containsExactly(106);
    }

    @Test
    @DisplayName("underscore in value mode counts as zero")
    void underscoreIsZero() {
        // header 'c' => 3 ; values "_b_" => 0+2+0 = 2
        assertThat(parser.parse("c_b_")).containsExactly(2);
    }

    @Test
    @DisplayName("case insensitive")
    void caseInsensitive() {
        assertThat(parser.parse("Ab")).containsExactly(2);
        assertThat(parser.parse("CABC")).containsExactly(6);
    }

    @Test
    @DisplayName("dangling z chain at end of input -> error")
    void danglingZ() {
        assertThatThrownBy(() -> parser.parse("zz"))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("dangling 'z' chain");
    }

    @Test
    @DisplayName("package size exceeds remaining characters -> error")
    void notEnoughValueChars() {
        // header 'e' => 5 ; only 2 chars follow
        assertThatThrownBy(() -> parser.parse("eab"))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("only 2");
    }

    @Test
    @DisplayName("invalid header character -> error")
    void invalidHeaderChar() {
        assertThatThrownBy(() -> parser.parse("_ab"))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("header character");
    }

    @Test
    @DisplayName("null input -> error")
    void nullInput() {
        assertThatThrownBy(() -> parser.parse(null))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    @DisplayName("realistic multi-package sample")
    void realisticSample() {
        // pkg1: 'b' header (2) + values 'ab' => 1+2 = 3
        // pkg2: 'c' header (3) + values 'def' => 4+5+6 = 15
        // pkg3: 'a' header (1) + values 'z' => 26
        List<Integer> result = parser.parse("babcdefaz");
        assertThat(result).containsExactly(3, 15, 26);
    }
}
