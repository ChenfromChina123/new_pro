import fnmatch
import locale
import os
from typing import Dict, List, Optional, Tuple


def search_files(pattern: str, root_dir: str, limit: int = 50) -> List[str]:
    results: List[str] = []
    for root, dirs, files in os.walk(root_dir):
        dirs[:] = [d for d in dirs if not d.startswith(".")]
        for name in files:
            if fnmatch.fnmatch(name, pattern):
                results.append(os.path.join(root, name))
                if len(results) >= limit:
                    return results
    return results


def calculate_diff_of_lines(path_of_file: str, content_new: str) -> Tuple[int, int]:
    lines_old = 0
    if os.path.exists(path_of_file):
        with open(path_of_file, "r", encoding="utf-8") as f:
            lines_old = len(f.readlines())
    lines_new = len(content_new.splitlines())
    added = max(0, lines_new - lines_old)
    deleted = max(0, lines_old - lines_new)
    return added, deleted


def ensure_parent_dir(path_of_file: str) -> None:
    os.makedirs(os.path.dirname(path_of_file), exist_ok=True)


def read_lines_robust(path_of_file: str) -> List[str]:
    try:
        with open(path_of_file, "r", encoding="utf-8") as f:
            return [line.rstrip("\n") for line in f.readlines()]
    except UnicodeDecodeError:
        try:
            with open(path_of_file, "r", encoding=locale.getpreferredencoding()) as f:
                return [line.rstrip("\n") for line in f.readlines()]
        except Exception:
            with open(path_of_file, "r", encoding="utf-8", errors="replace") as f:
                return [line.rstrip("\n") for line in f.readlines()]


def read_range(path_of_file: str, start_line: int = 1, end_line: Optional[int] = None) -> Tuple[int, int, str]:
    lines_all = read_lines_robust(path_of_file)
    total_lines = len(lines_all)
    actual_end = end_line if end_line and end_line <= total_lines else total_lines
    lines_target = lines_all[start_line - 1 : actual_end]
    content = "\n".join(lines_target)
    return total_lines, actual_end, content


class TreeNode:
    name: str
    children: Dict[str, "TreeNode"]

    def __init__(self, name: str):
        self.name = name
        self.children = {}


def generate_tree_structure(paths: List[str], cwd: str) -> str:
    if not paths:
        return ""

    paths = sorted(paths)

    if len(paths) == 1:
        common_root = os.path.dirname(paths[0])
    else:
        common_root = os.path.commonpath(paths)
        if not os.path.isdir(common_root):
            common_root = os.path.dirname(common_root)

    if not common_root or common_root == os.path.splitdrive(cwd)[0]:
        common_root = cwd

    tree_lines = [f"{common_root}/"]
    root_node = TreeNode("")

    for path in paths:
        rel_path = os.path.relpath(path, common_root)
        parts = rel_path.split(os.sep)
        curr = root_node
        for part in parts:
            if part not in curr.children:
                curr.children[part] = TreeNode(part)
            curr = curr.children[part]

    def build_tree_string(node: TreeNode, prefix: str = "", is_last: bool = True) -> List[str]:
        lines: List[str] = []
        if node.name:
            connector = "└── " if is_last else "├── "
            lines.append(f"{prefix}{connector}{node.name}")
            prefix = prefix + ("    " if is_last else "│   ")

        children = list(node.children.values())
        for idx, child in enumerate(children):
            lines.extend(build_tree_string(child, prefix, idx == len(children) - 1))
        return lines

    tree_lines.extend(build_tree_string(root_node))
    return "\n".join(tree_lines)
