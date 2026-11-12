export function normalizeMultiline(value: string): string {
	return value
		.replace(/\r\n?/g, '\n') // Normalize line endings to Unix style
		.replace(/[^\S\n]+/g, ' ') // Replace multiple spaces (except newlines) with a single space
		.replace(/ *\n */g, '\n') // Remove spaces around newlines
		.replace(/\n{3,}/g, '\n\n') // Limit consecutive newlines to a maximum of two
		.trim();
}

export interface MultilineRules {
	label: string;
	maxLength: number;
	maxLines: number;
}

export function validateMultiline(raw: string, rules: MultilineRules): string | null {
	const value = normalizeMultiline(raw);

	if (!value) return `${rules.label} cannot be empty.`;
	if (value.length > rules.maxLength) return `Maximum ${rules.maxLength} characters allowed.`;
	if (value.split('\n').length > rules.maxLines) return `Maximum ${rules.maxLines} lines allowed.`;

	return null;
}

export function normalizeSingleLine(value: string): string {
	return value.replace(/\s+/g, ' ').trim(); // Replace multiple whitespace characters with a single space and trim
}

export interface SingleLineRules {
	label: string;
	maxLength: number;
}

export function validateSingleLine(raw: string, rules: SingleLineRules): string | null {
	const value = normalizeSingleLine(raw);

	if (!value) return `${rules.label} cannot be empty.`;
	if (value.length > rules.maxLength) return `Maximum ${rules.maxLength} characters allowed.`;

	return null;
}