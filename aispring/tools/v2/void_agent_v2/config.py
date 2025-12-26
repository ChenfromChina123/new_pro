from dataclasses import dataclass, field
from typing import List


@dataclass
class Config:
    apiKey: str
    baseUrl: str
    modelName: str
    maxCycles: int = 30
    whitelistedTools: List[str] = field(default_factory=lambda: ["search_files", "search_in_files", "read_file"])
    whitelistedCommands: List[str] = field(default_factory=lambda: ["ls", "dir", "pwd", "whoami", "echo", "cat", "type"])
