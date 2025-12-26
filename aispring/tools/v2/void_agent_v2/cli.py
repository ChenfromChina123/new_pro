import os

from .agent import VoidAgent
from .config import Config
from .console import Fore, Style


def _normalize_base_url(rawUrl: str) -> str:
    """清理环境变量里的 BaseURL，兼容多余空格/引号/反引号。"""
    text = (rawUrl or "").strip()
    if not text:
        return ""
    text = text.strip("`").strip()
    if (text.startswith('"') and text.endswith('"')) or (text.startswith("'") and text.endswith("'")):
        text = text[1:-1].strip()
    return text


def _load_config_from_env() -> Config:
    """从环境变量加载配置，优先使用豆包(ARK)配置，其次使用默认配置。"""
    doubaoKey = (os.environ.get("DOUBAO_KEY") or "").strip()
    doubaoBaseUrl = _normalize_base_url(os.environ.get("DOUBAO_BASEURL") or os.environ.get("DOUBAO_BASE_URL") or "")
    doubaoModelName = (os.environ.get("DOUBAO_MODEL") or "").strip()

    if doubaoKey or doubaoBaseUrl or doubaoModelName:
        baseUrl = doubaoBaseUrl or "https://ark.cn-beijing.volces.com/api/v3"
        if not doubaoKey:
            raise ValueError("缺少 DOUBAO_KEY：请将豆包(ARK) API Key 写入环境变量 DOUBAO_KEY")
        if not doubaoModelName:
            raise ValueError("缺少 DOUBAO_MODEL：请将豆包模型名写入环境变量 DOUBAO_MODEL")
        return Config(apiKey=doubaoKey, baseUrl=baseUrl, modelName=doubaoModelName)

    apiKey = (os.environ.get("VOID_API_KEY") or "").strip()
    baseUrl = _normalize_base_url(os.environ.get("VOID_BASE_URL") or "https://api.deepseek.com")
    modelName = (os.environ.get("VOID_MODEL") or "deepseek-chat").strip()
    return Config(apiKey=apiKey, baseUrl=baseUrl, modelName=modelName)


def run_cli() -> None:
    """启动交互式 CLI，会话中通过远端 ChatCompletions 流式响应。"""
    try:
        config = _load_config_from_env()
    except ValueError as e:
        print(f"{Fore.RED}[Config Error] {str(e)}{Style.RESET_ALL}")
        return

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
