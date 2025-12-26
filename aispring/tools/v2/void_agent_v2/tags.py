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
    text_lower = text.lower()
    valid_tags = ["write_file", "read_file", "run_command", "search_files", "search_in_files", "edit_lines"]
    idx = 0

    while idx < len(text):
        next_tag = None
        next_start = -1
        for tag in valid_tags:
            start_tag = f"<{tag}>"
            s_idx = text.find(start_tag, idx)
            if s_idx == -1:
                s_idx = text_lower.find(start_tag, idx)
            if s_idx == -1:
                continue
            if next_start == -1 or s_idx < next_start:
                next_start = s_idx
                next_tag = tag

        if next_start == -1 or not next_tag:
            break

        start_tag = f"<{next_tag}>"
        end_tag = f"</{next_tag}>"
        e_idx = text.find(end_tag, next_start + len(start_tag))
        if e_idx == -1:
            e_idx = text_lower.find(end_tag, next_start + len(start_tag))
        if e_idx == -1:
            idx = next_start + len(start_tag)
            continue

        inner = text[next_start + len(start_tag) : e_idx].strip()
        task: Dict[str, Any] = {"type": next_tag}

        if next_tag == "write_file":
            task["path"] = find_substring(inner, "path")
            task["content"] = find_substring(inner, "content")
            if not task["path"] or not task["content"]:
                idx = e_idx + len(end_tag)
                continue
        elif next_tag == "read_file":
            task["path"] = find_substring(inner, "path")
            start_line_str = find_substring(inner, "start_line")
            end_line_str = find_substring(inner, "end_line")
            task["start_line"] = int(start_line_str) if start_line_str.strip() else 1
            task["end_line"] = int(end_line_str) if end_line_str.strip() else None
            if not task["path"]:
                idx = e_idx + len(end_tag)
                continue
        elif next_tag == "run_command":
            task["command"] = find_substring(inner, "command") or inner.strip()
            if not task["command"]:
                idx = e_idx + len(end_tag)
                continue
        elif next_tag == "search_files":
            task["pattern"] = find_substring(inner, "pattern") or "*"
        elif next_tag == "search_in_files":
            task["regex"] = find_substring(inner, "regex")
            task["glob"] = find_substring(inner, "glob") or "**/*"
            task["root"] = find_substring(inner, "root") or "."
            max_matches_str = find_substring(inner, "max_matches")
            task["max_matches"] = int(max_matches_str) if max_matches_str.strip() else 200
            if not task["regex"]:
                idx = e_idx + len(end_tag)
                continue
        elif next_tag == "edit_lines":
            task["path"] = find_substring(inner, "path")
            delete_start_str = find_substring(inner, "delete_start")
            delete_end_str = find_substring(inner, "delete_end")
            insert_at_str = find_substring(inner, "insert_at")
            task["delete_start"] = int(delete_start_str) if delete_start_str.strip() else None
            task["delete_end"] = int(delete_end_str) if delete_end_str.strip() else None
            task["insert_at"] = int(insert_at_str) if insert_at_str.strip() else None
            task["content"] = find_substring(inner, "content")
            if not task["path"]:
                idx = e_idx + len(end_tag)
                continue
            if task["delete_start"] is None and task["insert_at"] is None and not task["content"]:
                idx = e_idx + len(end_tag)
                continue

        tasks.append(task)
        idx = e_idx + len(end_tag)

    return tasks
