export function normalizeName(name: string): string {
	return name.toLowerCase().replace(/[^a-z0-9]+/g, " ").trim();
}

export function nameMatchScore(optionName: string, tableName: string): number {
	if (tableName === "" || optionName === "") {
		return 0;
	}
	if (optionName === tableName) {
		return 3;
	}
	if (optionName.includes(tableName) || tableName.includes(optionName)) {
		return 2;
	}
	return 0;
}
