package com.vacationary.messaging.domain.render;

import com.vacationary.messaging.constants.ReplyCopy;
import com.vacationary.messaging.core.interfaces.IReplyRenderer;
import com.vacationary.messaging.dtos.BlStayOption;
import com.vacationary.messaging.dtos.BlTurnResult;

public class ReplyRenderer implements IReplyRenderer {

	@Override
	public String render(BlTurnResult result) {
		if (result == null) {
			return ReplyCopy.EMPTY_REPLY;
		}
		if ("recommend".equalsIgnoreCase(result.outcome()) && !result.recommendations().isEmpty()) {
			return renderRecommend(result);
		}
		String text = ReplyTextStripper.stripTablesAndDumps(result.text());
		return text.isEmpty() ? ReplyCopy.EMPTY_REPLY : text;
	}

	private static String renderRecommend(BlTurnResult result) {
		StringBuilder body = new StringBuilder();
		String intro = ReplyTextStripper.stripTablesAndDumps(result.text());
		if (!intro.isEmpty()) {
			body.append(intro).append("\n\n");
		}
		for (int i = 0; i < result.recommendations().size(); i++) {
			if (i > 0) {
				body.append("\n\n");
			}
			appendStay(body, result.recommendations().get(i));
		}
		return body.toString().trim();
	}

	private static void appendStay(StringBuilder body, BlStayOption option) {
		String name = nullToEmpty(option.name());
		if (!name.isEmpty()) {
			body.append('*').append(name).append('*').append('\n');
		}
		String type = StayTypeLabel.of(option.stayType());
		String location = nullToEmpty(option.location());
		if (!type.isEmpty() && !location.isEmpty()) {
			body.append(type).append(" · ").append(location).append('\n');
		}
		else if (!type.isEmpty()) {
			body.append(type).append('\n');
		}
		else if (!location.isEmpty()) {
			body.append(location).append('\n');
		}
		String price = nullToEmpty(option.price());
		String rating = nullToEmpty(option.rating());
		if (!price.isEmpty() && !rating.isEmpty()) {
			body.append(price).append(" · Rating ").append(rating).append('\n');
		}
		else if (!price.isEmpty()) {
			body.append(price).append('\n');
		}
		else if (!rating.isEmpty()) {
			body.append("Rating ").append(rating).append('\n');
		}
		String why = nullToEmpty(option.why());
		if (!why.isEmpty()) {
			body.append(why).append('\n');
		}
		String url = nullToEmpty(option.url());
		if (!url.isEmpty()) {
			body.append(url);
		}
	}

	private static String nullToEmpty(String value) {
		return value == null ? "" : value.trim();
	}
}
