import os

from .agent import VoidAgent
from .config import Config
from .console import Fore, Style


def run_cli() -> None:
    apiKey = os.environ.get("VOID_API_KEY", "sk-45021a61b84a4693a1db4deb72cec673")
    baseUrl = os.environ.get("VOID_BASE_URL", "https://api.deepseek.com")
    modelName = os.environ.get("VOID_MODEL", "deepseek-chat")

    config = Config(apiKey=apiKey, baseUrl=baseUrl, modelName=modelName)
    agent = VoidAgent(config)

    while True:
        try:
            inputOfUser = input(f"\n{Style.BRIGHT}VoidUser: ")
            if inputOfUser.strip().lower() == "rollback":
                agent.rollbackLastOperation()
                continue
            if inputOfUser.strip().lower() in ["exit", "quit"]:
                break
            agent.chat(inputOfUser)
        except KeyboardInterrupt:
            print(f"\n{Fore.BLUE}VoidAgent Exiting...{Style.RESET_ALL}")
            break

