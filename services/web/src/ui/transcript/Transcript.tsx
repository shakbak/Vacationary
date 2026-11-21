import { useEffect, useRef } from "react";

import type { HistoryMessage } from "../../api/types";
import { EmptyState } from "./EmptyState";
import { Message } from "./Message";

type TranscriptProps = {
	messages: HistoryMessage[];
	pendingText: string | null;
	sending: boolean;
	loadingHistory: boolean;
};

export function Transcript({ messages, pendingText, sending, loadingHistory }: TranscriptProps) {
	const endRef = useRef<HTMLDivElement>(null);
	const showingEmptyState =
		messages.length === 0 && pendingText == null && !sending && !loadingHistory;
	const hasPendingUserBubble = pendingText != null;
	const showFindingStays = sending;

	useEffect(() => {
		endRef.current?.scrollIntoView({ block: "end" });
	}, [messages, pendingText, sending, loadingHistory]);

	return (
		<main className="transcript" aria-live="polite">
			{loadingHistory && <p className="loading-copy">Loading conversation…</p>}
			{showingEmptyState && <EmptyState />}
			{messages.map((message, index) => {
				const messageKey = `${index}-${message.role}`;
				return <Message key={messageKey} message={message} />;
			})}
			{hasPendingUserBubble && (
				<div className="bubble bubble--user">
					<p>{pendingText}</p>
				</div>
			)}
			{showFindingStays && (
				<div className="bubble bubble--assistant bubble--loading">
					<p className="loading-copy">Finding stays…</p>
				</div>
			)}
			<div ref={endRef} />
		</main>
	);
}
