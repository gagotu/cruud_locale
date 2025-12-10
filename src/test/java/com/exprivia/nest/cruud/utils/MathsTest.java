package com.exprivia.nest.cruud.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Maths Test Class
 */
@ExtendWith(SpringExtension.class)
public class MathsTest {

    String alternativeValue;

    @BeforeEach
    void setUp() {
        alternativeValue = "null";
    }

    @Test
    void testSumDouble() {
        String value = "123";
        String function = "+10";

        assertEquals(Double.parseDouble("133"), Double.parseDouble(Maths.execute(value, function, alternativeValue)));
    }

    @Test
    void testSumInteger() {
        String value = "123";
        String function = "+10";

        assertEquals(Integer.parseInt("133"), Integer.parseInt(Maths.execute(value, function, alternativeValue)));
    }

    @Test
    void testSumString() {
        String value = "123";
        String function = "+10";

        assertEquals(Integer.parseInt("133"), Integer.parseInt(Maths.execute(value, function, alternativeValue)));
    }

    @Test
    void testMultiplicationDouble() {
        String value = "10";
        String function = "*3.6";

        assertEquals(Double.parseDouble("36"), Double.parseDouble(Maths.execute(value, function, alternativeValue)));
    }

    @Test
    void testMultiplicationInteger() {
        String value = "10";
        String function = "*3";

        assertEquals(Integer.parseInt("30"), Integer.parseInt(Maths.execute(value, function, alternativeValue)));
    }

    @Test
    void testMultiplicationString() {
        String value = "10";
        String function = "*3";

        assertEquals(Integer.parseInt("30"), Integer.parseInt(Maths.execute(value, function, alternativeValue)));
    }

    @Test
    void testDivisionDouble() {
        String value = "12";
        String function = "/10";

        assertEquals(Double.parseDouble("1.2"), Double.parseDouble(Maths.execute(value, function, alternativeValue)));
    }

    @Test
    void testDivisionInteger() {
        String value = "120";
        String function = "/10";

        assertEquals(Integer.parseInt("12"), Integer.parseInt(Maths.execute(value, function, alternativeValue)));
    }

    @Test
    void testDivisionString() {
        String value = "120";
        String function = "/10";

        assertEquals(Integer.parseInt("12"), Integer.parseInt(Maths.execute(value, function, alternativeValue)));
    }

    @Test
    void testSubtractionDouble() {
        String value = "126.41";
        String function = "-0.40";

        assertEquals(Double.parseDouble("126.01"), Double.parseDouble(Maths.execute(value, function, alternativeValue)));
    }

    @Test
    void testSubtractionInteger() {
        String value = "126.41";
        String function = "-0.41";

        assertEquals(Integer.parseInt("126"), Integer.parseInt(Maths.execute(value, function, alternativeValue)));
    }

    @Test
    void testSubtractionString() {
        String value = "126.41";
        String function = "-0.40";

        assertEquals(Double.parseDouble("126.01"), Double.parseDouble(Maths.execute(value, function, alternativeValue)));
    }

}
