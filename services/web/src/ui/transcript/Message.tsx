import type { HistoryMessage } from "../../api/types";
import { stripMarkdownTables } from "../replyText";
import { StayList } from "../stay/StayList";

export function Message({ message }: { message: HistoryMessage }) {
	const isUserMessage = message.role === "user";
	const isFailedSearch = message.outcome === "fail";
	const visibleReply = isUserMessage ? message.text : stripMarkdownTables(message.text);
	const hasReplyText = visibleReply !== "";
	const bubbleClassName = isUserMessage
		? "bubble bubble--user"
		: isFailedSearch
			? "bubble bubble--assistant bubble--fail"
			: "bubble bubble--assistant";

	return (
		<div className={bubbleClassName}>
			{hasReplyText && (
				<p>
					{isFailedSearch ? (
						<span className="bubble__kind">Search could not finish. </span>
					) : null}
					{visibleReply}
				</p>
			)}
			<StayList message={message} />
		</div>
	);
}
