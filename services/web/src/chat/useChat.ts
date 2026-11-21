import { useCallback, useEffect, useState } from "react";

import { backendApi } from "../api/client";
import { TransportError, type HistoryMessage, type VacationaryApi } from "../api/types";
import { browserSession, type Session } from "../session/session";

const HISTORY_NOTE = "Earlier messages could not be loaded. You can still chat.";

export type ChatState = {
	messages: HistoryMessage[];
	pendingText: string | null;
	sending: boolean;
	loadingHistory: boolean;
	httpError: string | null;
	historyNote: string | null;
	draft: string;
};

export type Chat = ChatState & {
	loadHistory: () => Promise<void>;
	sendTurn: (text: string) => Promise<void>;
};

export function useChat(
	api: VacationaryApi = backendApi,
	session: Session = browserSession,
): Chat {
	const [messages, setMessages] = useState<HistoryMessage[]>([]);
	const [pendingText, setPendingText] = useState<string | null>(null);
	const [sending, setSending] = useState(false);
	const [loadingHistory, setLoadingHistory] = useState(true);
	const [httpError, setHttpError] = useState<string | null>(null);
	const [historyNote, setHistoryNote] = useState<string | null>(null);
	const [draft, setDraft] = useState("");

	const loadHistory = useCallback(async () => {
		setLoadingHistory(true);
		try {
			const userId = session.getOrCreateUserId();
			const loaded = await api.getConversation(userId);
			setMessages(loaded);
			setHistoryNote(null);
		} catch {
			setMessages([]);
			setHistoryNote(HISTORY_NOTE);
		} finally {
			setLoadingHistory(false);
		}
	}, [api, session]);

	useEffect(() => {
		void loadHistory();
	}, [loadHistory]);

	const sendTurn = useCallback(
		async (text: string) => {
			const trimmed = text.trim();
			if (trimmed === "" || sending || loadingHistory) {
				return;
			}
			setHttpError(null);
			setSending(true);
			setPendingText(trimmed);
			setDraft("");
			try {
				const userId = session.getOrCreateUserId();
				const response = await api.postTurn({
					channel: "web",
					externalUserId: userId,
					text: trimmed,
					eventId: session.newEventId(),
				});
				setMessages((current) => [
					...current,
					{ role: "user", text: trimmed, recommendations: [] },
					{
						role: "assistant",
						text: response.text,
						recommendations: response.recommendations,
						outcome: response.outcome,
					},
				]);
				setDraft("");
			} catch (error) {
				const message = error instanceof TransportError
					? error.message
					: "Could not reach the assistant. Please retry.";
				setHttpError(message);
				setDraft(trimmed);
			} finally {
				setPendingText(null);
				setSending(false);
			}
		},
		[api, loadingHistory, sending, session],
	);

	return {
		messages,
		pendingText,
		sending,
		loadingHistory,
		httpError,
		historyNote,
		draft,
		loadHistory,
		sendTurn,
	};
}
