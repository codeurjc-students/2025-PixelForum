import { normalizeMultiline, normalizeSingleLine, validateMultiline, MultilineRules } from './text-rules';

describe('text-rules', () => {

	// ---------- normalizeMultiline ----------

	describe('normalizeMultiline', () => {
		it('should normalize consecutive spaces and tabs', () => {
			expect(normalizeMultiline('Hello    world\t\t!')).toBe('Hello world !');
		});

		it('should normalize line endings', () => {
			expect(normalizeMultiline('First line\r\nSecond line\rThird line')).toBe('First line\nSecond line\nThird line');
		});

		it('should remove spaces around line breaks', () => {
			expect(normalizeMultiline('  First line  \n  Second line  ')).toBe('First line\nSecond line');
		});

		it('should reduce consecutive blank lines to one', () => {
			expect(normalizeMultiline('First paragraph\n\n\n\nSecond paragraph')).toBe('First paragraph\n\nSecond paragraph');
		});

		it('should keep a single blank line between paragraphs', () => {
			expect(normalizeMultiline('First\n\nSecond')).toBe('First\n\nSecond');
		});

		it('should trim leading and trailing whitespace', () => {
			expect(normalizeMultiline('  Hello world  \n\n')).toBe('Hello world');
		});

		it('should return an empty string when input is only whitespace', () => {
			expect(normalizeMultiline('   \n   \n  ')).toBe('');
		});

		it('should be idempotent', () => {
			const once = normalizeMultiline('  a   b \r\n\r\n\r\n c  ');
			expect(normalizeMultiline(once)).toBe(once);
		});
	});

	// ---------- normalizeSingleLine ----------

	describe('normalizeSingleLine', () => {
		it('should collapse any whitespace (including newlines) into single spaces', () => {
			expect(normalizeSingleLine('Hello \n\t  world')).toBe('Hello world');
		});

		it('should trim leading and trailing whitespace', () => {
			expect(normalizeSingleLine('  Title  ')).toBe('Title');
		});

		it('should return an empty string when input is only whitespace', () => {
			expect(normalizeSingleLine(' \n\t ')).toBe('');
		});
	});

	// ---------- validateMultiline ----------

	describe('validateMultiline', () => {
		const rules: MultilineRules = { label: 'The comment', maxLength: 2000, maxLines: 30 };

		it('should return an error when content contains only whitespace', () => {
			expect(validateMultiline('   \n   \n  ', rules)).toBe('The comment cannot be empty.');
		});

		it('should return an error when content exceeds the character limit', () => {
			expect(validateMultiline('a'.repeat(2001), rules)).toBe('Maximum 2000 characters allowed.');
		});

		it('should return an error when content exceeds the line limit', () => {
			const raw = Array(rules.maxLines + 1).fill('Line').join('\n');
			expect(validateMultiline(raw, rules)).toBe(`Maximum ${rules.maxLines} lines allowed.`);
		});

		it('should return null when content is valid', () => {
			expect(validateMultiline('First paragraph\n\nSecond paragraph', rules)).toBeNull();
		});

		it('should accept content with exactly the maximum number of characters', () => {
			expect(validateMultiline('a'.repeat(2000), rules)).toBeNull();
		});

		it('should accept content with exactly the maximum number of lines', () => {
			const raw = Array(rules.maxLines).fill('Line').join('\n');
			expect(validateMultiline(raw, rules)).toBeNull();
		});

		it('should validate against the normalized content, not the raw one', () => {
			expect(validateMultiline('  ' + 'a'.repeat(2000) + '  \n\n\n', rules)).toBeNull();
		});

		it('should use the label in the empty error', () => {
			expect(validateMultiline('  ', { ...rules, label: 'The bio' })).toBe('The bio cannot be empty.');
		});
	});
});