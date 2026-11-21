import { backendApi } from "../api/client";
import type { VacationaryApi } from "../api/types";
import { useChat } from "../chat/useChat";
import { browserSession, type Session } from "../session/session";
import { Composer } from "./composer/Composer";
import { useComposer } from "./composer/useComposer";
import { Header } from "./Header";
import { Status } from "./Status";
import { Transcript } from "./transcript/Transcript";

type AppProps = {
	api?: VacationaryApi;
	session?: Session;
};

export function App({ api, session }: AppProps) {
	const apiClient = api ?? backendApi;
	const activeSession = session ?? browserSession;
	const chat = useChat(apiClient, activeSession);
	const composer = useComposer(chat);

	return (
		<div className="app">
			<Header />
			<Transcript
				messages={chat.messages}
				pendingText={chat.pendingText}
				sending={chat.sending}
				loadingHistory={chat.loadingHistory}
			/>
			<div className="dock">
				<Status historyNote={chat.historyNote} httpError={chat.httpError} />
				<Composer
					text={composer.text}
					busy={composer.busy}
					inputRef={composer.inputRef}
					onChange={composer.setText}
					onSubmit={composer.onSubmit}
				/>
			</div>
		</div>
	);
}
