import { type FormEvent, useEffect, useRef, useState } from "react";

import type { Chat } from "../../chat/useChat";

export function useComposer(chat: Chat) {
	const [text, setText] = useState("");
	const inputRef = useRef<HTMLInputElement>(null);
	const busy = chat.sending || chat.loadingHistory;

	useEffect(() => {
		setText(chat.draft);
	}, [chat.draft]);

	useEffect(() => {
		if (!busy) {
			inputRef.current?.focus();
		}
	}, [busy, chat.messages, chat.httpError]);

	function onSubmit(event: FormEvent) {
		event.preventDefault();
		if (text.trim() === "" || busy) {
			return;
		}
		void chat.sendTurn(text);
		setText("");
	}

	return { text, setText, inputRef, busy, onSubmit };
}
