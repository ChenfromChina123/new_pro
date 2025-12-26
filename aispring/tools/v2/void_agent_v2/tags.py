from typing import Any, Dict, List


def find_substring(source: str, sub: str) -> str:
    s, e = f"<{sub}>", f"</{sub}>"
    si = source.find(s)
    ei = source.find(e)
    if si != -1 and ei != -1:
        return source[si + len(s) : ei].strip()
    s_lower, e_lower = f"<{sub.lower()}>", f"</{sub.lower()}>"
    si_lower = source.lower().find(s_lower)
    ei_lower = source.lower().find(e_lower)
    if si_lower != -1 and ei_lower != -1:
        return source[si_lower + len(s_lower) : ei_lower].strip()
    return ""


def parse_stack_of_tags(text: str) -> List[Dict[str, Any]]:
    tasks: List[Dict[str, Any]] = []
    valid_tags = ["write_file", "read_file", "run_command", "search_files"]
    text_lower = text.lower()

    for tag in valid_tags:
        start_tag = f"<{tag}>"
        end_tag = f"</{tag}>"
        if text_lower.count(start_tag) > 1 or text_lower.count(end_tag) > 1:
            return tasks

    for tag in valid_tags:
        start_tag = f"<{tag}>"
        end_tag = f"</{tag}>"
        s_idx = text.find(start_tag) if start_tag in text else text_lower.find(start_tag.lower())
        if s_idx == -1:
            continue
        e_idx = (
            text.find(end_tag, s_idx + len(start_tag))
            if end_tag in text
            else text_lower.find(end_tag.lower(), s_idx + len(start_tag))
        )
        if e_idx == -1:
            continue

        inner = text[s_idx + len(start_tag) : e_idx].strip()
        task: Dict[str, Any] = {"type": tag}

        if tag == "write_file":
            task["path"] = find_substring(inner, "path")
            task["content"] = find_substring(inner, "content")
            if not task["path"] or not task["content"]:
                continue
        elif tag == "read_file":
            task["path"] = find_substring(inner, "path")
            start_line_str = find_substring(inner, "start_line")
            end_line_str = find_substring(inner, "end_line")
            task["start_line"] = int(start_line_str) if start_line_str.strip() else 1
            task["end_line"] = int(end_line_str) if end_line_str.strip() else None
            if not task["path"]:
                continue
        elif tag == "run_command":
            task["command"] = find_substring(inner, "command") or inner.strip()
            if not task["command"]:
                continue
        elif tag == "search_files":
            task["pattern"] = find_substring(inner, "pattern") or "*"

        tasks.append(task)
        break

    return tasks

