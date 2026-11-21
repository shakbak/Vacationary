import { useLayoutEffect, useRef, useState } from "react";

import type { StayOption } from "../../api/types";
import { stayTypeLabel } from "./stayType";

export function StayCard({ option, why = option.why }: { option: StayOption; why?: string }) {
	const whyRef = useRef<HTMLParagraphElement>(null);
	const [expanded, setExpanded] = useState(false);
	const [overflows, setOverflows] = useState(false);
	const typeLabel = stayTypeLabel(option.stayType);
	const hasWhy = why !== "";
	const whyClassName = expanded ? "stay-card__why" : "stay-card__why stay-card__why--clamp";
	const showWhyToggle = overflows || expanded;
	const whyToggleLabel = expanded ? "See less" : "See more";

	useLayoutEffect(() => {
		const whyNode = whyRef.current;
		if (whyNode == null || expanded) {
			return;
		}
		const whyOverflows = whyNode.scrollHeight > whyNode.clientHeight + 1;
		setOverflows(whyOverflows);
	}, [why, expanded]);

	return (
		<article className="stay-card">
			<h3 className="stay-card__name">{option.name}</h3>
			<p className="stay-card__type">{typeLabel}</p>
			<p className="stay-card__location">{option.location}</p>
			<p className="stay-card__meta">
				<span>{option.price}</span>
				<span aria-hidden="true"> · </span>
				<span>Rating {option.rating}</span>
			</p>
			{hasWhy && (
				<div className="stay-card__why-block">
					<p ref={whyRef} className={whyClassName}>
						{why}
					</p>
					{showWhyToggle && (
						<button
							type="button"
							className="stay-card__toggle"
							aria-expanded={expanded}
							onClick={() => setExpanded((open) => !open)}
						>
							{whyToggleLabel}
						</button>
					)}
				</div>
			)}
			<a
				className="stay-card__link"
				href={option.url}
				target="_blank"
				rel="noreferrer noopener"
			>
				View listing
			</a>
		</article>
	);
}
