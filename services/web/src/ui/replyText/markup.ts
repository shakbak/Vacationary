export function stripMarkup(value: string): string {
	return value.replaceAll("**", "").replaceAll("*", "").trim();
}
