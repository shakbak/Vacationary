import type { HistoryMessage } from "../../api/types";
import { whyForStay } from "../replyText";
import { StayCard } from "./StayCard";

export function StayList({ message }: { message: HistoryMessage }) {
	const stays = message.recommendations;
	if (stays.length === 0) {
		return null;
	}
	return (
		<div className="stay-list">
			{stays.map((option) => {
				const cardKey = option.name + option.url;
				const standoutWhy = whyForStay(message.text, option.name) ?? option.why;
				return <StayCard key={cardKey} option={option} why={standoutWhy} />;
			})}
		</div>
	);
}
