package br.hubpedro.infra.api.router;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ParameterConverterTest {

    private ParameterConverter converter;

    @BeforeEach
    void setUp() {
        converter = new ParameterConverter();
    }

    @Test
    void testConvertString() {
        assertEquals("hello", converter.convertType("hello", String.class));
        assertEquals(" hello ", converter.convertType(" hello ", String.class));
    }

    @Test
    void testConvertInt() {
        assertEquals(42, converter.convertType("42", int.class));
        assertEquals(42, converter.convertType(" 42 ", int.class));
    }

    @Test
    void testConvertInteger() {
        assertEquals(42, converter.convertType("42", Integer.class));
        assertEquals(42, converter.convertType(" 42 ", Integer.class));
    }

    @Test
    void testConvertLong() {
        assertEquals(42L, converter.convertType("42", long.class));
        assertEquals(42L, converter.convertType(" 42 ", long.class));
    }

    @Test
    void testConvertLongWrapper() {
        assertEquals(42L, converter.convertType("42", Long.class));
        assertEquals(42L, converter.convertType(" 42 ", Long.class));
    }

    @Test
    void testConvertNullValue() {
        assertEquals("", converter.convertType(null, String.class));
        assertNull(converter.convertType(null, Integer.class));
        assertNull(converter.convertType(null, int.class));
    }

    @Test
    void testConvertBlankValue() {
        assertEquals("", converter.convertType("   ", String.class));
        assertNull(converter.convertType("   ", Integer.class));
        assertNull(converter.convertType("   ", int.class));
    }

    @Test
    void testUnsupportedTypeThrowsUnsupportedOperationException() {
        UnsupportedOperationException exception = assertThrows(
                UnsupportedOperationException.class,
                () -> converter.convertType("42", Double.class)
        );
        assertEquals("Tipo não suportado pelo framework: java.lang.Double", exception.getMessage());
    }

    @Test
    void testInvalidNumberFormatThrowsIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> converter.convertType("abc", int.class)
        );
        assertEquals("O valor 'abc' não é válido para o tipo int", exception.getMessage());

        IllegalArgumentException exception2 = assertThrows(
                IllegalArgumentException.class,
                () -> converter.convertType("abc", Long.class)
        );
        assertEquals("O valor 'abc' não é válido para o tipo Long", exception2.getMessage());
    }
}
