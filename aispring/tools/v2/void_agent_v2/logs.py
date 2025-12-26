import datetime
import json
import os
from typing import Dict, List


def log_request(messages: List[Dict[str, str]], log_dir: str = "logs") -> None:
    os.makedirs(log_dir, exist_ok=True)
    timestamp = datetime.datetime.now().strftime("%Y%m%d_%H%M%S")
    log_file = os.path.join(log_dir, f"void_chat_{timestamp}.json")

    messages_to_log: List[Dict[str, object]] = []
    for msg in messages:
        msg_copy: Dict[str, object] = msg.copy()
        if "content" in msg_copy and isinstance(msg_copy["content"], str):
            if msg_copy.get("role") == "system":
                msg_copy["content"] = str(msg_copy["content"]).replace("\\n", "\n").splitlines()
            else:
                msg_copy["content"] = str(msg_copy["content"]).splitlines()
        messages_to_log.append(msg_copy)

    with open(log_file, "w", encoding="utf-8") as f:
        json.dump(messages_to_log, f, ensure_ascii=False, indent=2)

