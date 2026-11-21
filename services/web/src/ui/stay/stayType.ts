const LABELS: Record<string, string> = {
	hotel: "Hotel",
	motel: "Motel",
	vacation_rental: "Vacation rental",
	hostel: "Hostel",
	bnb: "B&B",
	resort: "Resort",
};

export function stayTypeLabel(stayType: string): string {
	const key = stayType.trim().toLowerCase();
	return LABELS[key] ?? stayType;
}
