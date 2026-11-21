import type { ChangeEvent, FormEvent, RefObject } from "react";

type ComposerProps = {
	text: string;
	busy: boolean;
	inputRef: RefObject<HTMLInputElement | null>;
	onChange: (value: string) => void;
	onSubmit: (event: FormEvent) => void;
};

export function Composer({ text, busy, inputRef, onChange, onSubmit }: ComposerProps) {
	function handleFieldChange(event: ChangeEvent<HTMLInputElement>) {
		onChange(event.target.value);
	}

	return (
		<form className="composer" onSubmit={onSubmit} aria-busy={busy}>
			<label className="composer__label" htmlFor="message">
				Message
			</label>
			<input
				id="message"
				ref={inputRef}
				className="composer__field"
				type="text"
				name="message"
				autoComplete="off"
				placeholder="A city is enough…"
				value={text}
				disabled={busy}
				onChange={handleFieldChange}
			/>
			<button className="composer__send" type="submit" disabled={busy}>
				Send
			</button>
		</form>
	);
}
