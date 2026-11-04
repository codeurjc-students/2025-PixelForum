package es.codeurjc.backend.utils;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;

public final class TextRules {

	private static final Pattern CRLF = Pattern.compile("\\r\\n?");
	private static final Pattern INLINE_SPACES = Pattern.compile("[^\\S\\n]+");
	private static final Pattern SPACES_AROUND_NEWLINE = Pattern.compile(" *\\n *");
	private static final Pattern EXTRA_NEWLINES = Pattern.compile("\\n{3,}");
	private static final Pattern ANY_WHITESPACE = Pattern.compile("\\s+");
	private static final Pattern USERNAME = Pattern.compile("^[a-zA-Z0-9_.-]{3,20}$");
	private static final Pattern EMAIL = Pattern.compile(
			"^(?=.{1,254}$)(?=.{1,64}@)[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+)*"
					+ "@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$");

	private TextRules() {
	}

	public static String multiline(String field, String value, int maxLength, int maxLines) {
		String normalized = requireNonBlank(field, value);
		normalized = CRLF.matcher(normalized).replaceAll("\n");
		normalized = INLINE_SPACES.matcher(normalized).replaceAll(" ");
		normalized = SPACES_AROUND_NEWLINE.matcher(normalized).replaceAll("\n");
		normalized = EXTRA_NEWLINES.matcher(normalized).replaceAll("\n\n").trim();

		checkLength(field, normalized, maxLength);
		if (normalized.split("\n", -1).length > maxLines) {
			throw new IllegalArgumentException(field + " cannot exceed " + maxLines + " lines");
		}
		return normalized;
	}

	public static String optionalMultiline(String field, String value, int maxLength, int maxLines) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return multiline(field, value, maxLength, maxLines);
	}

	public static String singleLine(String field, String value, int maxLength) {
		requireNonBlank(field, value);
		String normalized = ANY_WHITESPACE.matcher(value).replaceAll(" ").trim();

		checkLength(field, normalized, maxLength);
		return normalized;
	}

	private static String requireNonBlank(String field, String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(field + " is required");
		}
		return value;
	}

	private static void checkLength(String field, String value, int maxLength) {
		if (value.length() > maxLength) {
			throw new IllegalArgumentException(field + " cannot exceed " + maxLength + " characters");
		}
	}

	public static String username(String value) {
		if (value == null || !USERNAME.matcher(value).matches()) {
			throw new IllegalArgumentException(
					"Username must be 3-20 characters: letters, numbers, underscores, dots and hyphens only");
		}
		return value;
	}

	public static String email(String value) {
		if (value.length() > 254 || !EMAIL.matcher(value).matches()) {
			throw new IllegalArgumentException("Please enter a valid email");
		}
		return value.toLowerCase(Locale.ROOT);
	}

	public static String password(String value) {
		if (value == null || value.length() < 8 || value.length() > 64) {
			throw new IllegalArgumentException("Password must be between 8 and 64 characters");
		}
		if (value.getBytes(StandardCharsets.UTF_8).length > 72) {
			throw new IllegalArgumentException("Password is too long");
		}
		return value;
	}
}