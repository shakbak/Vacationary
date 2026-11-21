export function Status({
	historyNote,
	httpError,
}: {
	historyNote: string | null;
	httpError: string | null;
}) {
	const showHistoryNote = historyNote != null;
	const showHttpError = httpError != null;

	return (
		<>
			{showHistoryNote && <p className="status">{historyNote}</p>}
			{showHttpError && <p className="status status--error">{httpError}</p>}
		</>
	);
}
