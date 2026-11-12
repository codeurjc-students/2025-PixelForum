package es.codeurjc.backend.unit;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import es.codeurjc.backend.utils.TextRules;

@Tag("unit")
@DisplayName("TextRules Unitary tests")
class TextRulesUnitTest {

	private static final int MAX_LENGTH = 2000;
	private static final int MAX_LINES = 30;

	private String multiline(String value) {
		return TextRules.multiline("The comment", value, MAX_LENGTH, MAX_LINES);
	}

	// =============== multiline: normalization ===============

	@Test
	@DisplayName("multiline should collapse consecutive spaces and tabs")
	void multilineCollapsesSpacesTest() {
		assertEquals("Hello world !", multiline("Hello    world\t\t!"));
	}

	@Test
	@DisplayName("multiline should normalize line endings to LF")
	void multilineNormalizesLineEndingsTest() {
		assertEquals("First\nSecond\nThird", multiline("First\r\nSecond\rThird"));
	}

	@Test
	@DisplayName("multiline should remove spaces around line breaks")
	void multilineRemovesSpacesAroundNewlinesTest() {
		assertEquals("First line\nSecond line", multiline("  First line  \n  Second line  "));
	}

	@Test
	@DisplayName("multiline should reduce consecutive blank lines to one")
	void multilineReducesBlankLinesTest() {
		assertEquals("First\n\nSecond", multiline("First\n\n\n\nSecond"));
	}

	@Test
	@DisplayName("multiline should keep a single blank line between paragraphs")
	void multilineKeepsSingleBlankLineTest() {
		assertEquals("First\n\nSecond", multiline("First\n\nSecond"));
	}

	@Test
	@DisplayName("multiline should trim leading and trailing whitespace")
	void multilineTrimsTest() {
		assertEquals("Hello world", multiline("  Hello world  \n\n"));
	}

	@Test
	@DisplayName("multiline should be idempotent")
	void multilineIdempotentTest() {
		String once = multiline("  a   b \r\n\r\n\r\n c  ");
		assertEquals(once, multiline(once));
	}

	// =============== multiline: validation ===============

	@Test
	@DisplayName("multiline should throw when value is null")
	void multilineNullTest() {
		assertThrows(IllegalArgumentException.class, () -> multiline(null));
	}

	@Test
	@DisplayName("multiline should throw when value is empty")
	void multilineEmptyTest() {
		assertThrows(IllegalArgumentException.class, () -> multiline(""));
	}

	@Test
	@DisplayName("multiline should throw when value is only whitespace")
	void multilineBlankTest() {
		assertThrows(IllegalArgumentException.class, () -> multiline("   \n\n  \t "));
	}

	@Test
	@DisplayName("multiline should accept exactly the maximum length")
	void multilineMaxLengthTest() {
		assertEquals(MAX_LENGTH, multiline("a".repeat(MAX_LENGTH)).length());
	}

	@Test
	@DisplayName("multiline should throw when exceeding the maximum length")
	void multilineTooLongTest() {
		String tooLong = "a".repeat(MAX_LENGTH + 1);
		assertThrows(IllegalArgumentException.class, () -> multiline(tooLong));
	}

	@Test
	@DisplayName("multiline should accept exactly the maximum number of lines")
	void multilineMaxLinesTest() {
		assertDoesNotThrow(() -> multiline("a\n".repeat(MAX_LINES - 1) + "a"));
	}

	@Test
	@DisplayName("multiline should throw when exceeding the maximum number of lines")
	void multilineTooManyLinesTest() {
		String tooManyLines = "a\n".repeat(MAX_LINES) + "a";
		assertThrows(IllegalArgumentException.class, () -> multiline(tooManyLines));
	}

	@Test
	@DisplayName("multiline should validate against the normalized value, not the raw one")
	void multilineValidatesNormalizedTest() {
		assertDoesNotThrow(() -> multiline("  " + "a".repeat(MAX_LENGTH) + "  \n\n\n"));
	}

	@Test
	@DisplayName("multiline should include the field name in the error message")
	void multilineErrorMessageTest() {
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> TextRules.multiline("Bio", "   ", 100, 5));
		assertTrue(ex.getMessage().contains("Bio"));
	}

	// =============== singleLine ===============

	@Test
	@DisplayName("singleLine should collapse any whitespace, including newlines, into single spaces")
	void singleLineCollapsesTest() {
		assertEquals("Hello world", TextRules.singleLine("Title", "Hello \n\t  world", 100));
	}

	@Test
	@DisplayName("singleLine should trim leading and trailing whitespace")
	void singleLineTrimsTest() {
		assertEquals("Title", TextRules.singleLine("Title", "  Title  ", 100));
	}

	@Test
	@DisplayName("singleLine should throw when value is null or blank")
	void singleLineBlankTest() {
		assertThrows(IllegalArgumentException.class, () -> TextRules.singleLine("Title", null, 100));
		assertThrows(IllegalArgumentException.class, () -> TextRules.singleLine("Title", " \n\t ", 100));
	}

	@Test
	@DisplayName("singleLine should accept exactly the maximum length")
	void singleLineMaxLengthTest() {
		assertEquals(100, TextRules.singleLine("Title", "a".repeat(100), 100).length());
	}

	@Test
	@DisplayName("singleLine should throw when exceeding the maximum length")
	void singleLineTooLongTest() {
		String tooLong = "a".repeat(101);
		assertThrows(IllegalArgumentException.class, () -> TextRules.singleLine("Title", tooLong, 100));
	}

	// =============== optionalMultiline ===============

	@Test
	@DisplayName("optionalMultiline should return null when value is null, empty or blank")
	void optionalMultilineEmptyTest() {
		assertNull(TextRules.optionalMultiline("Bio", null, 300, 5));
		assertNull(TextRules.optionalMultiline("Bio", "", 300, 5));
		assertNull(TextRules.optionalMultiline("Bio", "  \n\t ", 300, 5));
	}

	@Test
	@DisplayName("optionalMultiline should normalize a valid value")
	void optionalMultilineNormalizesTest() {
		assertEquals("Hello world", TextRules.optionalMultiline("Bio", "  Hello   world  ", 300, 5));
	}

	@Test
	@DisplayName("optionalMultiline should throw when a non-blank value exceeds the limits")
	void optionalMultilineLimitsTest() {
		String tooLong = "a".repeat(301);
		String tooManyLines = "a\n".repeat(5) + "a";
		assertThrows(IllegalArgumentException.class, () -> TextRules.optionalMultiline("Bio", tooLong, 300, 5));
		assertThrows(IllegalArgumentException.class, () -> TextRules.optionalMultiline("Bio", tooManyLines, 300, 5));
	}

	// =============== username ===============

	@Test
	@DisplayName("username should accept letters, numbers, underscores, dots and hyphens")
	void usernameValidTest() {
		assertEquals("john_doe-1.x", TextRules.username("john_doe-1.x"));
	}

	@Test
	@DisplayName("username should accept exactly 3 and 20 characters")
	void usernameBoundsTest() {
		assertDoesNotThrow(() -> TextRules.username("abc"));
		assertDoesNotThrow(() -> TextRules.username("a".repeat(20)));
	}

	@Test
	@DisplayName("username should throw when shorter than 3 or longer than 20 characters")
	void usernameLengthTest() {
		String tooLong = "a".repeat(21);
		assertThrows(IllegalArgumentException.class, () -> TextRules.username("ab"));
		assertThrows(IllegalArgumentException.class, () -> TextRules.username(tooLong));
	}

	@Test
	@DisplayName("username should throw with spaces, emojis, accents or other symbols")
	void usernameInvalidCharactersTest() {
		assertThrows(IllegalArgumentException.class, () -> TextRules.username("john doe"));
		assertThrows(IllegalArgumentException.class, () -> TextRules.username("john "));
		assertThrows(IllegalArgumentException.class, () -> TextRules.username("john😀"));
		assertThrows(IllegalArgumentException.class, () -> TextRules.username("jóhn"));
		assertThrows(IllegalArgumentException.class, () -> TextRules.username("john@doe"));
	}

	@Test
	@DisplayName("username should throw when null")
	void usernameNullTest() {
		assertThrows(IllegalArgumentException.class, () -> TextRules.username(null));
	}

	// =============== email ===============

	@Test
	@DisplayName("email should accept a valid email and convert it to lowercase")
	void emailValidTest() {
		assertEquals("pepe@mail.com", TextRules.email("Pepe@Mail.com"));
		assertEquals("pepe.garcia+tag@correo.urjc.es", TextRules.email("pepe.garcia+tag@correo.urjc.es"));
	}

	@Test
	@DisplayName("email should throw when it has leading or trailing spaces")
	void emailSpacesTest() {
		assertThrows(IllegalArgumentException.class, () -> TextRules.email(" pepe@mail.com"));
		assertThrows(IllegalArgumentException.class, () -> TextRules.email("pepe@mail.com "));
		assertThrows(IllegalArgumentException.class, () -> TextRules.email("pe pe@mail.com"));
	}

	@Test
	@DisplayName("email should throw when the format is invalid")
	void emailInvalidFormatTest() {
		assertThrows(IllegalArgumentException.class, () -> TextRules.email("pepemail.com"));
		assertThrows(IllegalArgumentException.class, () -> TextRules.email("@mail.com"));
		assertThrows(IllegalArgumentException.class, () -> TextRules.email("pepe@"));
		assertThrows(IllegalArgumentException.class, () -> TextRules.email("pepe@@mail.com"));
		assertThrows(IllegalArgumentException.class, () -> TextRules.email("pepe..x@mail.com"));
		assertThrows(IllegalArgumentException.class, () -> TextRules.email("pepe@mail..com"));
	}

	@Test
	@DisplayName("email should throw when null or empty")
	void emailNullTest() {
		assertThrows(IllegalArgumentException.class, () -> TextRules.email(null));
		assertThrows(IllegalArgumentException.class, () -> TextRules.email(""));
	}

	@Test
	@DisplayName("email should accept exactly 254 characters and reject 255")
	void emailMaxLengthTest() {
		String local = "a".repeat(64);
		String domain = "b".repeat(63) + "." + "b".repeat(63) + "." + "b".repeat(61);
		String max = local + "@" + domain;
		assertEquals(254, max.length());
		assertDoesNotThrow(() -> TextRules.email(max));
		assertThrows(IllegalArgumentException.class, () -> TextRules.email(max + "b"));
	}

	@Test
	@DisplayName("email should throw when the local part exceeds 64 characters")
	void emailLocalPartTooLongTest() {
		String tooLongLocal = "a".repeat(65);
		assertThrows(IllegalArgumentException.class, () -> TextRules.email(tooLongLocal + "@mail.com"));
	}

	// =============== password ===============

	@Test
	@DisplayName("password should accept exactly 8 and 64 characters")
	void passwordBoundsTest() {
		assertDoesNotThrow(() -> TextRules.password("a".repeat(8)));
		assertDoesNotThrow(() -> TextRules.password("a".repeat(64)));
	}

	@Test
	@DisplayName("password should throw when shorter than 8 or longer than 64 characters")
	void passwordLengthTest() {
		String tooShort = "a".repeat(7);
		String tooLong = "a".repeat(65);
		assertThrows(IllegalArgumentException.class, () -> TextRules.password(tooShort));
		assertThrows(IllegalArgumentException.class, () -> TextRules.password(tooLong));
	}

	@Test
	@DisplayName("password should keep spaces untouched")
	void passwordSpacesTest() {
		assertEquals("  mi perro come pan  ", TextRules.password("  mi perro come pan  "));
	}

	@Test
	@DisplayName("password should throw when it exceeds 72 bytes even if under 64 characters")
	void passwordBytesTest() {
		// 25 emojis = 50 chars (UTF-16), 100 bytes
		String tooManyBytes = "😀".repeat(25);
		assertThrows(IllegalArgumentException.class, () -> TextRules.password(tooManyBytes));
	}

	@Test
	@DisplayName("password should throw when null")
	void passwordNullTest() {
		assertThrows(IllegalArgumentException.class, () -> TextRules.password(null));
	}
}